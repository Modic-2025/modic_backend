package hanium.modic.backend.web.user.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.domain.user.service.UserService;
import hanium.modic.backend.web.user.dto.UserCreateRequest;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

	@MockitoBean
	private UserService userService;

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("유저 회원가입 컨트롤러 테스트")
	void createUserSuccessTest() throws Exception {
		// given
		UserCreateRequest request = new UserCreateRequest("youth@cotato.kr", "password", "youth");
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
			request.name()
		);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("invalidUserCreateRequests")
	@DisplayName("유저 회원가입 컨트롤러 테스트 - 유효성 검사 실패")
	void createUserValidationTest(String description, UserCreateRequest request, String expectedErrorMessage) throws Exception {
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
				new UserCreateRequest("invalid-email", "youth", "qwer1234@#!"),
				"이메일 형식으로 요청해주세요."
			),
			Arguments.of(
				"이메일이 null인 경우",
				new UserCreateRequest(null, "youth", "qwer1234@#!"),
				"이메일은 필수입니다."
			),
			Arguments.of(
				"비밀번호가 형식이 맞지 않는 경우",
				new UserCreateRequest("youth@cotato.kr", "youth", null),
				"비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다."
			),
			Arguments.of(
				"이름이 20자 이상인 경우",
				new UserCreateRequest("youth@cotato.kr", "youthyouthyouthyouthyouth", "youth@123"),
				"이름은 2자 이상 20자 이하로 입력해주세요."
			)
		);
	}
}