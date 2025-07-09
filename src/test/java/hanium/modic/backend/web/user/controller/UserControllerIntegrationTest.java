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
import hanium.modic.backend.web.user.dto.request.UpdateUserNameRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserPasswordRequest;
import hanium.modic.backend.web.user.dto.request.UserCreateRequest;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private PasswordEncoder passwordEncoder;

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

		UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(oldPassword, newPassword);

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
	@DisplayName("TEST3: 기존 비밀번호가 일치하지 않아 비밀번호 변경에 실패한다")
	@WithCustomUser(email = "viewer@test.com")
	void updateUserPasswordFailDueToWrongOldPassword() throws Exception {
		// given
		UserEntity user = ContextHolderUtil.getCurrentUser();
		String actualOldPassword = "correctPassword1!";
		String wrongOldPassword = "wrongPassword1!";
		String newPassword = "newPassword1!";

		user.updatePassword(passwordEncoder.encode(actualOldPassword));
		userEntityRepository.save(user);

		UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(wrongOldPassword, newPassword);

		// when
		ResultActions result = mockMvc.perform(patch("/api/users/password")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(USER_PASSWORD_MISMATCH_EXCEPTION.getCode()));

		UserEntity updatedUser = userEntityRepository.findByEmail("viewer@test.com").orElseThrow();
		assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isFalse(); // 비밀번호가 변경되지 않아야 함
		assertThat(passwordEncoder.matches(actualOldPassword, updatedUser.getPassword())).isTrue(); // 기존 비밀번호는 여전히 일치해야 함
	}
}
