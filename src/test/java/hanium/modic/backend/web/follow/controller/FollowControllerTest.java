package hanium.modic.backend.web.follow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.domain.follow.dto.FollowType;
import hanium.modic.backend.domain.follow.service.FollowService;

@WebMvcTest(controllers = FollowController.class)
@AutoConfigureMockMvc(addFilters = false)
class FollowControllerTest extends BaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FollowService followService;

	@BeforeEach
	void setup() {
		setupCurrentUserMocking();
	}

	@ParameterizedTest
	@DisplayName("팔로우 요청 실패 - 필수 파라미터 누락 시 400 반환")
	@MethodSource("provideInvalidFollowRequest")
	void followRequestValidationFail(String userId, String type) throws Exception {
		mockMvc.perform(post("/api/follows")
				.param("userId", userId)
				.param("type", type))
			.andExpect(status().isBadRequest());
	}

	private static Stream<Arguments> provideInvalidFollowRequest() {
		return Stream.of(
			// userId 누락
			Arguments.of("", FollowType.FOLLOW.name()),
			// type 누락
			Arguments.of("1", ""),
			// 둘 다 누락
			Arguments.of("", "")
		);
	}

	@Test
	@DisplayName("팔로워 목록 조회 실패 - 유효하지 않은 page/size")
	void getMyFollowersInvalidPaginationFail() throws Exception {
		mockMvc.perform(get("/api/follows/followers/me")
				.param("page", "-1")
				.param("size", "50"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_INPUT_EXCEPTION.getCode()));
	}

	@Test
	@DisplayName("팔로잉 목록 조회 실패 - 유효하지 않은 page/size")
	void getMyFollowingsInvalidPaginationFail() throws Exception {
		mockMvc.perform(get("/api/follows/followings/me")
				.param("page", "-1")
				.param("size", "100"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_INPUT_EXCEPTION.getCode()));
	}
}