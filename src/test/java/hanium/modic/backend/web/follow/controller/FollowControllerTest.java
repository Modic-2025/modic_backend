package hanium.modic.backend.web.follow.controller;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.follow.dto.FollowType;
import hanium.modic.backend.domain.follow.service.FollowService;

@WebMvcTest(controllers = FollowController.class)
@AutoConfigureMockMvc(addFilters = false)
class FollowControllerTest extends BaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FollowService followService;

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

	@Test
	@DisplayName("팔로우 상태 확인 성공 - 팔로우 중인 경우")
	void getFollowStatusSuccess() throws Exception {
		// given
		setupCurrentUserMocking();
		Long targetUserId = 2L;
		when(followService.isFollowing(1L, targetUserId)).thenReturn(true);

		// when & then
		mockMvc.perform(get("/api/follows/status")
				.param("userId", String.valueOf(targetUserId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.isFollowing").value(true))
			.andExpect(jsonPath("$.isSuccess").value(true));

		verify(followService).isFollowing(1L, targetUserId);
	}

	@Test
	@DisplayName("팔로우 상태 확인 성공 - 팔로우하지 않은 경우")
	void getFollowStatusNotFollowing() throws Exception {
		// given
		setupCurrentUserMocking();
		Long targetUserId = 2L;
		when(followService.isFollowing(1L, targetUserId)).thenReturn(false);

		// when & then
		mockMvc.perform(get("/api/follows/status")
				.param("userId", String.valueOf(targetUserId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.isFollowing").value(false))
			.andExpect(jsonPath("$.isSuccess").value(true));

		verify(followService).isFollowing(1L, targetUserId);
	}

	@Test
	@DisplayName("팔로우 상태 확인 실패 - 존재하지 않는 사용자")
	void getFollowStatusUserNotFound() throws Exception {
		// given
		setupCurrentUserMocking();
		Long targetUserId = 999L;
		when(followService.isFollowing(1L, targetUserId))
			.thenThrow(new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		// when & then
		mockMvc.perform(get("/api/follows/status")
				.param("userId", String.valueOf(targetUserId)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND_EXCEPTION.getCode()));
	}

	@Test
	@DisplayName("팔로우 상태 확인 실패 - userId 파라미터 누락")
	void getFollowStatusMissingUserId() throws Exception {
		// given
		setupCurrentUserMocking();

		// when & then
		mockMvc.perform(get("/api/follows/status"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}
}