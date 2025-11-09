package hanium.modic.backend.web.auth.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import hanium.modic.backend.domain.auth.repository.AuthCodeRepository;
import hanium.modic.backend.domain.auth.util.CookieUtil;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.auth.dto.LoginRequest;
import hanium.modic.backend.web.auth.dto.SendEmailRequest;
import hanium.modic.backend.web.auth.dto.VerifyEmailCodeRequest;
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

	@Autowired
	private AuthCodeRepository authCodeRepository;

	@Autowired
	private CookieUtil cookieUtil;

	@Test
	@DisplayName("로그인 API 테스트")
	void loginApiSuccessTest() throws Exception {
		// given
		final String email = "test@test.kr";
		final String originPassword = "qwer1234@#!";
		String encoded = passwordEncoder.encode(originPassword);
		UserEntity user = UserEntity.builder().email(email).password(encoded).name("찬호").build();

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
		UserEntity user = UserEntity.builder().name("찬호").email("youth@cotato.kr").password("password").build();
		userEntityRepository.save(user);

		Token token = jwtTokenProvider.createToken(user);
		RefreshToken refreshToken = RefreshToken.builder()
			.userId(user.getId())
			.refreshToken(token.refreshToken())
			.build();
		refreshTokenRepository.save(refreshToken);

		Cookie refreshCookie = cookieUtil.createRefreshCookie(token.refreshToken());

		// when, then
		mockMvc.perform(post("/api/auth/reissue")
				.cookie(refreshCookie))
			.andExpect(status().isOk())
			.andExpect(header().string("Authorization", startsWith("Bearer ")))
			.andExpect(cookie().exists("refreshToken"));
	}

	@Test
	@DisplayName("회원 인증 이메일 코드 발송 API")
	void sendEmailSignupCodeApiSuccess() throws Exception {
		// given
		SendEmailRequest request = new SendEmailRequest("test@test.kr");

		// when, then
		mockMvc.perform(post("/api/auth/email/verification")
				.param("type", "sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("회원 가입 인증 코드 성공 API")
	void verifyEmailCodeApiSuccess() throws Exception {
		// given
		final String email = "youth@cotato.kr";
		final String code = "1234";

		authCodeRepository.saveCode(email, code);

		VerifyEmailCodeRequest request = new VerifyEmailCodeRequest(email, code);

		// when, then
		mockMvc.perform(post("/api/auth/email/verification/check")
				.param("type", "sign-up")
				.content(objectMapper.writeValueAsString(request))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpectAll(jsonPath("$.data.email").value(email),
				jsonPath("$.data.isVerified").value(true));
	}

	@Test
	@DisplayName("이메일 중복 확인 API - 중복된 이메일 (사용 불가)")
	void checkEmailDuplicate_ExistingEmail() throws Exception {
		// given
		final String email = "existing@example.com";
		UserEntity existingUser = UserEntity.builder()
			.email(email)
			.password(passwordEncoder.encode("password123"))
			.name("기존사용자")
			.build();
		userEntityRepository.save(existingUser);

		// when, then
		mockMvc.perform(get("/api/auth/email/check")
				.param("email", email))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.email").value(email))
			.andExpect(jsonPath("$.data.available").value(false));
	}

	@Test
	@DisplayName("이메일 중복 확인 API - 사용 가능한 이메일")
	void checkEmailDuplicate_AvailableEmail() throws Exception {
		// given
		final String email = "available@example.com";
		// 데이터베이스에 해당 이메일이 없는 상태

		// when, then
		mockMvc.perform(get("/api/auth/email/check")
				.param("email", email))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.email").value(email))
			.andExpect(jsonPath("$.data.available").value(true));
	}

	@Test
	@DisplayName("이메일 중복 확인 API - 잘못된 이메일 형식")
	void checkEmailDuplicate_InvalidEmailFormat() throws Exception {
		// given
		final String invalidEmail = "invalid-email-format";

		// when, then
		mockMvc.perform(get("/api/auth/email/check")
				.param("email", invalidEmail))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("이메일 중복 확인 API - 빈 이메일")
	void checkEmailDuplicate_EmptyEmail() throws Exception {
		// given
		final String emptyEmail = "";

		// when, then
		mockMvc.perform(get("/api/auth/email/check")
				.param("email", emptyEmail))
			.andExpect(status().isBadRequest());
	}
}
