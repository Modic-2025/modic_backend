package hanium.modic.backend.domain.profile.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.follow.repository.FollowEntityRepository;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.transaction.repository.AccountRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.entity.UserImageEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.repository.UserImageEntityRepository;
import hanium.modic.backend.domain.user.service.UserImageService;
import hanium.modic.backend.web.profile.dto.GetMyProfileResponse;
import hanium.modic.backend.web.profile.dto.GetProfileResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService {

	// 유저 관련
	private final UserEntityRepository userRepository;
	private final UserImageService userImageService;
	private final UserImageEntityRepository userImageEntityRepository;

	// 포스트 관련
	private final PostEntityRepository postRepository;

	// 팔로우 관련
	private final FollowEntityRepository followRepository;
	private final AccountRepository accountRepository;

	// 내 프로필 조회(코인 함께 조회)
	@Transactional(readOnly = true)
	public GetMyProfileResponse getMyProfile(final UserEntity user) {
		final long postCount = postRepository.countByUserId(user.getId()); // TODO: 추후 개선 필요, count 쿼리 없애는 방법
		final long followingCount = followRepository.countByMyId(user.getId()); // TODO: 추후 개선 필요
		final long followerCount = followRepository.countByFollowingId(user.getId()); // TODO: 추후 개선 필요
		final Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(user.getId());
		final Optional<Long> userImageId = userImageEntityRepository.findByUserId(user.getId())
			.map(UserImageEntity::getId);
		final boolean hasUserImage = userImageUrl.isPresent();
		final long coinAmount = accountRepository.findById(user.getId())
			.map(account -> account.getCoin())
			.orElseThrow(() -> new AppException(ACCOUNT_NOT_FOUND_EXCEPTION));

		return new GetMyProfileResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			hasUserImage,
			userImageUrl.orElse(null),
			userImageId.orElse(null),
			postCount,
			followerCount,
			followingCount,
			coinAmount
		);
	}

	// 사용자 프로필 조회
	@Transactional(readOnly = true)
	public GetProfileResponse getProfile(final long userId) {
		UserEntity user = userRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		final long postCount = postRepository.countByUserId(userId); // TODO: 추후 개선 필요, count 쿼리 없애는 방법
		final long followingCount = followRepository.countByMyId(userId); // TODO: 추후 개선 필요
		final long followerCount = followRepository.countByFollowingId(userId); // TODO: 추후 개선 필요
		final Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(userId);
		final Optional<Long> userImageId = userImageEntityRepository.findByUserId(userId)
			.map(UserImageEntity::getId);
		final boolean hasUserImage = userImageUrl.isPresent();

		return new GetProfileResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			hasUserImage,
			userImageUrl.orElse(null),
			userImageId.orElse(null),
			postCount,
			followerCount,
			followingCount
		);
	}
}
