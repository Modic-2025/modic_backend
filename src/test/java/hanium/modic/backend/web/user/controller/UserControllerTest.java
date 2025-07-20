package hanium.modic.backend.web.user.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.service.UserCoinService;
import hanium.modic.backend.domain.user.service.UserService;
import hanium.modic.backend.web.user.dto.request.UpdateUserNameRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserPasswordRequest;
import hanium.modic.backend.web.user.dto.request.UserCreateRequest;
import hanium.modic.backend.web.user.dto.request.GetUserUpdateTokenRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserEmailRequest;
import hanium.modic.backend.web.user.dto.response.UserInfoResponse;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest extends BaseControllerTest {

	@MockitoBean
	private UserService userService;
	@MockitoBean
	private UserCoinService userCoinService;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("유저 회원가입 컨트롤러 테스트")
	void createUserSuccessTest() throws Exception {
		// given
		UserCreateRequest request = new UserCreateRequest("youth@cotato.kr", "youth", "qwer1234@#!", "code");
		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated());

		// then
		verify(userService).createUser(
			request.email(),
			request.password(),
			request.name(),
			request.code()
		);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("invalidUserCreateRequests")
	@DisplayName("유저 회원가입 컨트롤러 테스트 - 유효성 검사 실패")
	void createUserValidationTest(String description, UserCreateRequest request, String expectedErrorMessage) throws
		Exception {
		// given
		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason").value(expectedErrorMessage));
	}

	static Stream<Arguments> invalidUserCreateRequests() {
		return Stream.of(
			Arguments.of(
				"이메일 형식이 올바르지 않은 경우",
				new UserCreateRequest("invalid-email", "youth", "qwer1234@#!", "code"),
				"이메일 형식으로 요청해주세요."
			),
			Arguments.of(
				"이메일이 null인 경우",
				new UserCreateRequest(null, "youth", "qwer1234@#!", "code"),
				"이메일은 필수입니다."
			),
			Arguments.of(
				"비밀번호가 형식이 맞지 않는 경우",
				new UserCreateRequest("youth@cotato.kr", "youth", null, "code"),
				"비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다."
			),
			Arguments.of(
				"이름이 20자 이상인 경우",
				new UserCreateRequest("youth@cotato.kr", "youthyouthyouthyouthyouth", "youth@123", "code"),
				"이름은 2자 이상 20자 이하로 입력해주세요."
			),
			Arguments.of(
				"인증코드가 null인 경우",
				new UserCreateRequest("youth@cotato.kr", "youth", "youth@123", null),
				"인증 코드를 입력해주세요."
			),
			Arguments.of(
				"인증코드가 빈 경우",
				new UserCreateRequest("youth@cotato.kr", "youth", "youth@123", ""),
				"인증 코드를 입력해주세요."
			)
		);
	}

	@Test
	@DisplayName("회원 정보 조회 성공")
	void getUserInfo_success() throws Exception {
		// given
		final Long userId = 1L;
		UserEntity mockUser = UserFactory.createMockUser(userId);

		Authentication authentication = new UsernamePasswordAuthenticationToken(mockUser, null, List.of());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		UserInfoResponse mockResponse = UserInfoResponse.from(mockUser);
		given(userService.getUserInfo(any(UserEntity.class))).willReturn(mockResponse);

		// when & then
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(mockUser.getId()))
			.andExpect(jsonPath("$.data.email").value(mockUser.getEmail()));

		SecurityContextHolder.clearContext();
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("invalidNameRequests")
	@DisplayName("이름 변경 유효성 검증 실패")
	void updateUserName_validation_fail(String description, UpdateUserNameRequest request, String expectedMessage) throws Exception {
		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(patch("/api/users/name")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedMessage));
	}

	static Stream<Arguments> invalidNameRequests() {
		return Stream.of(
			Arguments.of(
				"이름이 null인 경우",
				new UpdateUserNameRequest(null),
				"이름은 필수입니다."
			),
			Arguments.of(
				"이름이 빈 문자열인 경우",
				new UpdateUserNameRequest(""),
				"이름은 2자 이상 20자 이하로 입력해주세요."
			),
			Arguments.of(
				"이름이 21자 이상인 경우",
				new UpdateUserNameRequest("a".repeat(21)),
				"이름은 2자 이상 20자 이하로 입력해주세요."
			)
		);
	}

    @Test
    @DisplayName("유저 정보 변경 토큰 발급 컨트롤러 테스트")
    void getUserUpdateTokenControllerTest() throws Exception {
        // given
        Long userId = 1L;
        String password = "password1!";
        String token = "update-token-123";
        GetUserUpdateTokenRequest request = new GetUserUpdateTokenRequest(password);
        String json = objectMapper.writeValueAsString(request);
        UserEntity mockUser = UserFactory.createMockUser(userId);

        given(userService.getUserUpdateToken(anyLong(), eq(password))).willReturn(token);

        // when & then
        mockMvc.perform(post("/api/users/update-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userUpdateToken").value(token));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidUpdateTokenRequests")
    @DisplayName("유저 정보 변경 토큰 발급 유효성 검증 실패")
    void getUserUpdateToken_validation_fail(String description, GetUserUpdateTokenRequest request, String expectedMessage) throws Exception {
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/users/update-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.reason").value(expectedMessage));
    }

    static Stream<Arguments> invalidUpdateTokenRequests() {
        return Stream.of(
            Arguments.of(
                "비밀번호가 null인 경우",
                new GetUserUpdateTokenRequest(null),
                "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다."
            ),
            Arguments.of(
                "비밀번호가 빈 문자열인 경우",
                new GetUserUpdateTokenRequest(""),
                "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다."
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidEmailRequests")
    @DisplayName("이메일 변경 유효성 검증 실패")
    void updateUserEmail_validation_fail(String description, UpdateUserEmailRequest request, String expectedMessage) throws Exception {
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/api/users/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.reason").value(expectedMessage));
    }

    static Stream<Arguments> invalidEmailRequests() {
        return Stream.of(
            Arguments.of(
                "이메일이 null인 경우",
                new UpdateUserEmailRequest(null, "validToken"),
                "이메일은 필수입니다."
            ),
            Arguments.of(
                "이메일 형식이 올바르지 않은 경우",
                new UpdateUserEmailRequest("invalid-email", "validToken"),
                "이메일 형식이 올바르지 않습니다."
            ),
            Arguments.of(
                "토큰이 null인 경우",
                new UpdateUserEmailRequest("valid@email.com", null),
                "토큰은 필수입니다."
            ),
            Arguments.of(
                "토큰이 빈 문자열인 경우",
                new UpdateUserEmailRequest("valid@email.com", ""),
                "토큰은 필수입니다."
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidPasswordRequests")
    @DisplayName("비밀번호 변경 유효성 검증 실패")
    void updateUserPassword_validation_fail(String description, UpdateUserPasswordRequest request, String expectedMessage) throws Exception {
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/api/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.reason").value(expectedMessage));
    }

    static Stream<Arguments> invalidPasswordRequests() {
        return Stream.of(
            Arguments.of(
                "비밀번호가 null인 경우",
                new UpdateUserPasswordRequest(null, "validToken"),
                "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다."
            ),
            Arguments.of(
                "비밀번호가 형식에 맞지 않는 경우",
                new UpdateUserPasswordRequest("short", "validToken"),
                "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다."
            ),
            Arguments.of(
                "토큰이 null인 경우",
                new UpdateUserPasswordRequest("ValidPassword1!", null),
                "토큰은 필수입니다."
            ),
            Arguments.of(
                "토큰이 빈 문자열인 경우",
                new UpdateUserPasswordRequest("ValidPassword1!", ""),
                "토큰은 필수입니다."
            )
        );
    }
}