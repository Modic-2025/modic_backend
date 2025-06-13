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
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.common.jwt.RefreshToken;
import hanium.modic.backend.common.jwt.RefreshTokenRepository;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.auth.util.CookieUtil;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.auth.dto.LoginRequest;
import hanium.modic.backend.web.auth.dto.SendEmailRequest;
import jakarta.servlet.http.Cookie;

public class AuthControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

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

	@Test
	@DisplayName("토큰 재발급 API 테스트")
	void reissueApiSuccess() throws Exception {
		// given
		UserEntity user = UserEntity.builder().email("youth@cotato.kr").password("password").build();
		userEntityRepository.save(user);

		Token token = jwtTokenProvider.createToken(user);
		RefreshToken refreshToken = RefreshToken.builder()
			.userId(user.getId())
			.refreshToken(token.refreshToken())
			.build();
		refreshTokenRepository.save(refreshToken);

		Cookie refreshCookie = CookieUtil.createRefreshCookie(token.refreshToken());

		// when, then
		mockMvc.perform(post("/api/auth/reissue")
				.cookie(refreshCookie))
			.andExpect(status().isOk())
			.andExpect(header().string("Authorization", "Bearer " + token.accessToken()))
			.andExpect(cookie().value("refreshToken", token.refreshToken()));
	}

	@Test
	@DisplayName("회원 인증 이메일 코드 발송 API")
	void sendEmailSignupCodeApiSuccess() throws Exception {
		// given
		SendEmailRequest request = new SendEmailRequest("boysoeng@naver.com");

		// when, then
		try {
			mockMvc.perform(post("/api/auth/email/verification")
					.param("type", "sign-up")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
