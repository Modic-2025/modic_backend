package hanium.modic.backend.web.auth.controller;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MissingRequestCookieException;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.domain.auth.service.AuthService;
import hanium.modic.backend.web.auth.dto.LoginRequest;
import hanium.modic.backend.web.auth.dto.LoginResponse;
import hanium.modic.backend.web.auth.dto.ReissueResponse;
import hanium.modic.backend.web.auth.dto.SendEmailRequest;
import jakarta.servlet.http.Cookie;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest extends BaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("로그인 컨트롤러 테스트")
	void loginSuccess() throws Exception {
		// given
		final String email = "youth@youth.kr";
		final String password = "password123@#!";
		LoginRequest request = new LoginRequest(email, password);

		when(authService.login(email, password))
			.thenReturn(new LoginResponse("accessToken", "refreshToken"));

		// when
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
			.andExpect(header().string("Authorization", "Bearer accessToken"))
			.andReturn();

		// then
		MockHttpServletResponse servletResponse = result.getResponse();
		Cookie[] cookies = servletResponse.getCookies();

		boolean hasRefreshCookie = Arrays.stream(cookies)
			.anyMatch(cookie -> "refreshToken".equals(cookie.getName()) && cookie.getValue().equals("refreshToken"));

		assertThat(hasRefreshCookie).isTrue();
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("invalidLoginRequests")
	@DisplayName("로그인 실패 테스트")
	void loginFail(String description, LoginRequest request, String expectedErrorMessage) throws Exception {
		// given
		when(authService.login(request.email(), request.password()))
			.thenThrow(new RuntimeException(expectedErrorMessage));

		// when + then
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedErrorMessage)); // ✔ 여기를 수정
	}

	private static Stream<Arguments> invalidLoginRequests() {
		return Stream.of(
			Arguments.of(
				"이메일이 빈 문자열인 경우",
				new LoginRequest("", "password123@#!"),
				"이메일은 필수 입력 항목입니다."
			),
			Arguments.of(
				"이메일 형식이 잘못된 경우",
				new LoginRequest("not-an-email", "password123@#!"),
				"유효하지 않은 이메일 형식입니다."
			),
			Arguments.of(
				"비밀번호가 null인 경우",
				new LoginRequest("youth@youth.kr", null),
				"비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다."
			)
		);
	}

	@Test
	@DisplayName("리프레시 토큰 재발급 테스트")
	void reissueSuccess() throws Exception {
		// given
		String oldRefreshToken = "oldRefreshToken";
		String newAccessToken = "newAccessToken";
		String newRefreshToken = "newRefreshToken";

		ReissueResponse mockResponse = new ReissueResponse(newAccessToken, newRefreshToken);

		when(authService.reissue(oldRefreshToken)).thenReturn(mockResponse);

		// when, then
		mockMvc.perform(post("/api/auth/reissue")
				.cookie(new Cookie("refreshToken", oldRefreshToken)))
			.andExpect(status().isOk())
			.andExpect(header().string("Authorization", "Bearer " + newAccessToken))
			.andExpect(cookie().value("refreshToken", newRefreshToken));
	}

	@Test
	@DisplayName("토큰 재발급 API - 실패 케이스 (쿠키 누락)")
	void reissue_fail_missing_cookie() throws Exception {
		mockMvc.perform(post("/api/auth/reissue"))
			.andExpect(status().isBadRequest())
			.andExpect(result -> assertInstanceOf(MissingRequestCookieException.class, result.getResolvedException()));
	}

	@Test
	@DisplayName("이메일 인증 코드 전송 테스트")
	void sendSignupEmailVerificationCode() throws Exception {
		// given
		final String email = "youth@youth.kr";

		SendEmailRequest request = new SendEmailRequest(email);

		// when, then
		mockMvc.perform(post("/api/auth/email/verification?type=sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk());
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("invalidEmailRequests")
	@DisplayName("이메일 인증 코드 전송 실패 테스트")
	void sendSignupEmailVerificationCodeFail(String description, SendEmailRequest request,
		String expectedErrorMessage) throws Exception {
		// given

		// when, then
		mockMvc.perform(post("/api/auth/email/verification?type=sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedErrorMessage)); // ✔ 여기를 수정
	}

	static Stream<Arguments> invalidEmailRequests() {
		return Stream.of(
			Arguments.of(
				"이메일이 빈 문자열인 경우",
				new SendEmailRequest(""),
				"이메일은 필수 입력 항목입니다."
			),
			Arguments.of(
				"이메일 형식이 잘못된 경우",
				new SendEmailRequest("not-an-email"),
				"유효하지 않은 이메일 형식입니다."
			)
		);
	}
}