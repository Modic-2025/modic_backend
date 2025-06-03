package hanium.modic.backend.web.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.auth.dto.LoginRequest;

public class AuthControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		userEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("로그인 API 테스트")
	void loginApiSuccessTest() throws Exception {
		// given
		final String email = "test@test.kr";
		final String originPassword = "qwer1234@#!";
		String encoded = passwordEncoder.encode(originPassword);
		UserEntity user = UserEntity.builder().email(email).password(encoded).build();

		userEntityRepository.save(user);

		LoginRequest request = new LoginRequest(email, originPassword);

		// when, then
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
	}
}
