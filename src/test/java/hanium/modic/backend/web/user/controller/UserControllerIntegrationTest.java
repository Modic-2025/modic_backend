package hanium.modic.backend.web.user.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.user.dto.UserCreateRequest;

@AutoConfigureMockMvc(addFilters = true)
public class UserControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

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
	void getUserInfoApiTest() throws Exception {
		// given
		UserEntity user = userEntityRepository.save(
			UserEntity.builder().email("youth@youth.kr").name("youth").password("test-password").build());

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
}
