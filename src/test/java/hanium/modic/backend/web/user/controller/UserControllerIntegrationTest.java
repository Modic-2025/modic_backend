package hanium.modic.backend.web.user.controller;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.base.login.ContextHolderUtil;
import hanium.modic.backend.base.login.WithCustomUser;
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.service.UserService;
import hanium.modic.backend.web.user.dto.request.UpdateUserNameRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserPasswordRequest;
import hanium.modic.backend.web.user.dto.request.UserCreateRequest;
import hanium.modic.backend.web.user.dto.request.GetUserUpdateTokenRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserEmailRequest;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserService userService;

	@Test
	@DisplayName("회원가입 API 테스트")
	void createUserApiTest() throws Exception {
		// given
		UserCreateRequest request = new UserCreateRequest("youth@cotato.kr", "youth", "qwer1234@#!");
		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated());

		// then
		assertThat(userEntityRepository.findAll()).hasSize(1);

		var saved = userEntityRepository.findAll().get(0);
		assertThat(saved.getEmail()).isEqualTo("youth@cotato.kr");
		assertThat(saved.getName()).isEqualTo("youth");
	}

	@Test
	@DisplayName("유저 정보 조회 API")
	@WithCustomUser(email = "user@test.com")
	void getUserInfoApiTest() throws Exception {
		// given
		UserEntity user = ContextHolderUtil.getCurrentUser();

		Token token = jwtTokenProvider.createToken(user);

		// when, then
		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer " + token.accessToken())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpectAll(jsonPath("$.data.id").value(user.getId()),
				jsonPath("$.data.email").value(user.getEmail()),
				jsonPath("$.data.name").value(user.getName()));
	}

	@Test
	@DisplayName("TEST1: 이름 변경에 성공한다")
	@WithCustomUser(email = "viewer@test.com")
	void updateUserNameSuccess() throws Exception {
		// given
		String newName = "UpdatedViewer";
		UpdateUserNameRequest request = new UpdateUserNameRequest(newName);

		// when
		ResultActions result = mockMvc.perform(patch("/api/users/name")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		result.andExpect(status().isOk());

		UserEntity user = userEntityRepository.findByEmail("viewer@test.com").orElseThrow();
		assertThat(user.getName()).isEqualTo(newName);
	}

	@Test
	@DisplayName("TEST2: 비밀번호 변경에 성공한다")
	@WithCustomUser(email = "viewer@test.com")
	void updateUserPasswordSuccess() throws Exception {
		// given
		UserEntity user = ContextHolderUtil.getCurrentUser();
		String oldPassword = "oldPassword1!";
		String newPassword = "newPassword1!";

		// 인코딩으로 인하여 비밀번호 세팅
		user.updatePassword(passwordEncoder.encode(oldPassword));
		userEntityRepository.save(user);

		// 유저 토큰 발급
		String userUpdateToken = userService.getUserUpdateToken(user.getId(), oldPassword);

		UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(newPassword, userUpdateToken);

		// when
		ResultActions result = mockMvc.perform(patch("/api/users/password")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		result.andExpect(status().isOk());

		UserEntity updatedUser = userEntityRepository.findByEmail("viewer@test.com").orElseThrow();
		assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue(); // 비밀번호가 변경되어야 함
	}

	@Test
	@DisplayName("유저 정보 변경 토큰 발급 API 테스트")
	@WithCustomUser(email = "user@token.com")
	void getUserUpdateTokenApiTest() throws Exception {
		UserEntity user = ContextHolderUtil.getCurrentUser();
		String password = "originPassword1!";
		user.updatePassword(passwordEncoder.encode(password));
		userEntityRepository.save(user);

		GetUserUpdateTokenRequest request = new GetUserUpdateTokenRequest(password);
		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/users/update-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.userUpdateToken").isNotEmpty());
	}

	@Test
	@DisplayName("유저 이메일 변경 API 테스트 (토큰 기반)")
	@WithCustomUser(email = "user@email.com")
	void updateUserEmailApiTest() throws Exception {
		UserEntity user = ContextHolderUtil.getCurrentUser();
		String password = "originPassword2!";
		user.updatePassword(passwordEncoder.encode(password));
		userEntityRepository.save(user);

		// 토큰 발급
		String updateToken = userService.getUserUpdateToken(user.getId(), password);

		String newEmail = "changed@email.com";
		UpdateUserEmailRequest emailRequest = new UpdateUserEmailRequest(newEmail, updateToken);

		mockMvc.perform(patch("/api/users/email")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(emailRequest)))
			.andExpect(status().isOk());

		UserEntity updated = userEntityRepository.findById(user.getId()).orElseThrow();
		assertThat(updated.getEmail()).isEqualTo(newEmail);
	}

	@Test
	@DisplayName("유저 비밀번호 변경 API 테스트 (토큰 기반)")
	@WithCustomUser(email = "user@pw.com")
	void updateUserPasswordApiTest() throws Exception {
		UserEntity user = ContextHolderUtil.getCurrentUser();
		String oldPassword = "originPassword3!";
		user.updatePassword(passwordEncoder.encode(oldPassword));
		userEntityRepository.save(user);

		// 토큰 발급
		String updateToken = userService.getUserUpdateToken(user.getId(), oldPassword);

		String newPassword = "changedPassword3!";
		UpdateUserPasswordRequest pwRequest = new UpdateUserPasswordRequest(newPassword, updateToken);

		mockMvc.perform(patch("/api/users/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(pwRequest)))
			.andExpect(status().isOk());

		UserEntity updated = userEntityRepository.findById(user.getId()).orElseThrow();
		assertThat(passwordEncoder.matches(newPassword, updated.getPassword())).isTrue();
	}
}
