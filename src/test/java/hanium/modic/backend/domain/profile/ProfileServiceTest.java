package hanium.modic.backend.domain.profile;

import static hanium.modic.backend.common.error.ErrorCode.USER_NOT_FOUND_EXCEPTION;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.follow.repository.FollowEntityRepository;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.profile.service.ProfileService;
import hanium.modic.backend.domain.transaction.entity.Account;
import hanium.modic.backend.domain.transaction.repository.AccountRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.repository.UserImageEntityRepository;
import hanium.modic.backend.domain.user.service.UserImageService;
import hanium.modic.backend.web.profile.dto.GetMyProfileResponse;
import hanium.modic.backend.web.profile.dto.GetProfileResponse;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

	@InjectMocks
	private ProfileService profileService;

	@Mock
	private UserEntityRepository userRepository;

	@Mock
	private PostEntityRepository postRepository;

	@Mock
	private FollowEntityRepository followRepository;

	@Mock
	private UserImageEntityRepository userImageEntityRepository;

	@Mock
	private UserImageService userImageService;

	@Mock
	private AccountRepository accountRepository;

	@Test
	@DisplayName("TEST1: 내 프로필 조회 성공")
	void getMyProfileSuccess() {
		// given
		UserEntity me = UserFactory.createMockUser(1L);

		when(postRepository.countByUserId(1L)).thenReturn(5L);
		when(followRepository.countByMyId(1L)).thenReturn(3L);
		when(followRepository.countByFollowingId(1L)).thenReturn(7L);
		when(userImageEntityRepository.findByUserId(1L)).thenReturn(Optional.empty());
		when(accountRepository.findById(1L)).thenReturn(
			Optional.ofNullable(
				Account.builder().userId(1L).build()
			)
		);

		// when
		GetMyProfileResponse response = profileService.getMyProfile(me);

		// then
		assertThat(response.userId()).isEqualTo(me.getId());
		assertThat(response.userEmail()).isEqualTo(me.getEmail());
		assertThat(response.userName()).isEqualTo(me.getName());
		assertThat(response.postCount()).isEqualTo(5L);
		assertThat(response.followingCount()).isEqualTo(3L);
		assertThat(response.followerCount()).isEqualTo(7L);
		assertThat(response.coin()).isEqualTo(0L);
	}

	@Test
	@DisplayName("TEST2: 타인 프로필 조회 성공")
	void getOtherProfileSuccess() {
		// given
		long userId = 2L;
		UserEntity target = UserFactory.createMockUserWithoutId("user1");

		when(userRepository.findById(userId)).thenReturn(Optional.of(target));
		when(postRepository.countByUserId(userId)).thenReturn(4L);
		when(followRepository.countByMyId(userId)).thenReturn(2L);
		when(followRepository.countByFollowingId(userId)).thenReturn(9L);
		when(userImageService.createImageGetUrlOptional(userId)).thenReturn(Optional.empty());
		when(userImageEntityRepository.findByUserId(userId)).thenReturn(Optional.empty());

		// when
		GetProfileResponse response = profileService.getProfile(userId);

		// then
		assertThat(response.userId()).isEqualTo(target.getId());
		assertThat(response.userEmail()).isEqualTo(target.getEmail());
		assertThat(response.userName()).isEqualTo(target.getName());
		assertThat(response.postCount()).isEqualTo(4L);
		assertThat(response.followingCount()).isEqualTo(2L);
		assertThat(response.followerCount()).isEqualTo(9L);
	}

	@Test
	@DisplayName("TEST3: 존재하지 않는 유저 프로필 조회 시 예외 발생")
	void getOtherProfileThrowsIfUserNotFound() {
		// given
		long userId = 999L;
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		AppException ex = assertThrows(AppException.class, () ->
			profileService.getProfile(userId)
		);

		assertThat(ex.getErrorCode()).isEqualTo(USER_NOT_FOUND_EXCEPTION);
	}
}
