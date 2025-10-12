package hanium.modic.backend.domain.follow.service;

import static hanium.modic.backend.domain.follow.dto.FollowType.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.follow.repository.FollowEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.repository.UserImageEntityRepository;
import hanium.modic.backend.domain.user.service.UserImageService;
import hanium.modic.backend.web.follow.dto.response.GetFollowersResponse;
import hanium.modic.backend.web.follow.dto.response.GetFollowingsResponse;

@ExtendWith(MockitoExtension.class)
class FollowMockingServiceTest {

	@InjectMocks
	private FollowService followService;

	@Mock
	private FollowEntityRepository followRepository;

	@Mock
	private UserEntityRepository userRepository;

	@Mock
	private UserImageService userImageService;

	@Mock
	private UserImageEntityRepository userImageEntityRepository;

	@Test
	@DisplayName("TEST1: 존재하지 않는 유저의 팔로워 목록 조회 시 예외 발생")
	void getFollowersThrowsIfUserNotExists() {
		// given
		long invalidUserId = 999L;
		when(userRepository.existsById(invalidUserId)).thenReturn(false);

		// when & then
		AppException ex = assertThrows(AppException.class, () ->
			followService.getFollowers(invalidUserId, 0, 10)
		);

		assertEquals(ErrorCode.USER_NOT_FOUND_EXCEPTION, ex.getErrorCode());
	}

	@Test
	@DisplayName("TEST2: 팔로우 요청 처리 - 기존 팔로우 없음")
	void followSuccess() {
		// given
		UserEntity me = mock(UserEntity.class);
		when(me.getId()).thenReturn(1L);

		UserEntity target = mock(UserEntity.class);
		when(target.getId()).thenReturn(2L);

		when(userRepository.findById(2L)).thenReturn(Optional.of(target));

		// when
		followService.followOrUnfollow(me, 2L, FOLLOW);

		// then
		verify(followRepository, times(1)).insertFollowIfExist(me.getId(), target.getId());
	}

	@Test
	@DisplayName("TEST3: 자기 자신을 팔로우할 경우 예외 발생")
	void followSelfThrowsException() {
		// given
		UserEntity me = mock(UserEntity.class);
		when(me.getId()).thenReturn(1L);

		// when & then
		AppException ex = assertThrows(AppException.class, () ->
			followService.followOrUnfollow(me, 1L, FOLLOW)
		);

		assertEquals(ErrorCode.CANNOT_FOLLOW_SELF_EXCEPTION, ex.getErrorCode());
	}

	@Test
	@DisplayName("TEST4: 언팔로우 요청 처리 - 기존 팔로우 있음")
	void unfollowSuccess() {
		// given
		UserEntity me = mock(UserEntity.class);
		when(me.getId()).thenReturn(1L);

		UserEntity target = mock(UserEntity.class);

		when(userRepository.findById(2L)).thenReturn(Optional.of(target));

		// when
		followService.followOrUnfollow(me, 2L, UNFOLLOW);

		// then
		verify(followRepository, times(1)).deleteByMyIdAndFollowingId(1L, 2L);
	}

	@Test
	@DisplayName("TEST5: 팔로워 목록 조회 성공")
	void getFollowersSuccess() {
		// given
		long userId = 1L;
		when(userRepository.existsById(userId)).thenReturn(true);

		UserEntity user2 = mock(UserEntity.class);
		when(user2.getId()).thenReturn(2L);
		when(user2.getName()).thenReturn("user2");
		when(user2.getEmail()).thenReturn("user2@email.com");

		UserEntity user3 = mock(UserEntity.class);
		when(user3.getId()).thenReturn(3L);
		when(user3.getName()).thenReturn("user3");
		when(user3.getEmail()).thenReturn("user3@email.com");

		Page<UserEntity> page = new PageImpl<>(List.of(user2, user3));
		when(followRepository.findFollowersOrderByCreatedAt(eq(userId), any(PageRequest.class)))
			.thenReturn(page);
		when(userImageService.createImageGetUrlMap(List.of(2L, 3L))).thenReturn(Map.of()); // 프로필 없음

		// when
		Page<GetFollowersResponse> result = followService.getFollowers(userId, 0, 10);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).extracting("userId").containsExactly(2L, 3L);
	}

	@Test
	@DisplayName("TEST6: 팔로잉 목록 조회 성공")
	void getFollowingsSuccess() {
		// given
		long userId = 1L;
		when(userRepository.existsById(userId)).thenReturn(true);

		UserEntity user4 = mock(UserEntity.class);
		when(user4.getId()).thenReturn(2L);
		when(user4.getName()).thenReturn("user2");
		when(user4.getEmail()).thenReturn("user2@email.com");

		UserEntity user5 = mock(UserEntity.class);
		when(user5.getId()).thenReturn(3L);
		when(user5.getName()).thenReturn("user3");
		when(user5.getEmail()).thenReturn("user3@email.com");

		Page<UserEntity> page = new PageImpl<>(List.of(user4, user5));
		when(followRepository.findFollowingOrderByCreatedAt(eq(userId), any(PageRequest.class))).thenReturn(page);
		when(userImageService.createImageGetUrlMap(List.of(2L, 3L))).thenReturn(Map.of()); // 프로필 없음

		// when
		Page<GetFollowingsResponse> result = followService.getFollowings(userId, 0, 10);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent().get(0).userId()).isEqualTo(2L);
		assertThat(result.getContent().get(1).userId()).isEqualTo(3L);
	}
}
