package hanium.modic.backend.domain.follow.service;

import static hanium.modic.backend.domain.follow.dto.FollowType.*;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.follow.dto.FollowType;
import hanium.modic.backend.domain.follow.dto.FollowerWithStatus;
import hanium.modic.backend.domain.follow.dto.FollowingWithStatus;
import hanium.modic.backend.domain.follow.repository.FollowEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.repository.UserImageEntityRepository;
import hanium.modic.backend.domain.user.service.UserImageService;
import hanium.modic.backend.web.follow.dto.response.GetFollowersResponse;
import hanium.modic.backend.web.follow.dto.response.GetFollowersWithStatusResponse;
import hanium.modic.backend.web.follow.dto.response.GetFollowingsResponse;
import hanium.modic.backend.web.follow.dto.response.GetFollowingsWithStatusResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowService {
	private final FollowEntityRepository followRepository;
	private final UserEntityRepository userRepository;
	private final UserImageService userImageService;
	private final UserImageEntityRepository userImageRepository;
	private final UserImageEntityRepository userImageEntityRepository;

	// 팔로우 또는 언팔로우 처리
	@Transactional
	public void followOrUnfollow(
		final UserEntity me,
		final Long targetId,
		final FollowType type
	) {
		// 자기 자신을 팔로우할 수 없도록 예외 처리
		if (me.getId().equals(targetId)) {
			throw new AppException(ErrorCode.CANNOT_FOLLOW_SELF_EXCEPTION);
		}

		UserEntity target = userRepository.findById(targetId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		// 팔로우 요청 및 기존 팔로우 존재 여부에 따라 팔로우, 언팔로우 처리
		if (type == FOLLOW) {
			followRepository.insertFollowIfExist(me.getId(), target.getId());
		} else if (type == UNFOLLOW) {
			followRepository.deleteByMyIdAndFollowingId(me.getId(), targetId);
		}
	}

	// TODO : 정렬 기준 고려
	// 팔로워 목록 조회 (미인증 유저용)
	@Transactional(readOnly = true)
	public Page<GetFollowersResponse> getFollowers(final long userId, final int page, final int size) {
		validateUserExists(userId);

		// 1. 팔로워들 조회
		Page<UserEntity> followers = followRepository.findFollowersOrderByCreatedAt(userId,
			PageRequest.of(page, size));

		// 2. userImage N + 1 해결을 위한 배치 조회
		userImageEntityRepository.findAllByUserIdIn(
			followers.stream().map(UserEntity::getId).toList()
		);

		// 3. 응답 생성
		return followers
			.map(u -> {
				final Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(u.getId());
				final boolean hasUserImage = userImageUrl.isPresent();

				return new GetFollowersResponse(
					u.getId(),
					hasUserImage,
					userImageUrl.orElse(null),
					u.getName(),
					u.getEmail()
				);
			});
	}

	// TODO : 정렬 기준 고려
	// 내 팔로워 목록 조회 (인증 유저용 - 팔로우 상태 포함)
	@Transactional(readOnly = true)
	public Page<GetFollowersWithStatusResponse> getMyFollowers(final long userId, final int page, final int size) {
		validateUserExists(userId);

		// 1. 팔로워들 조회
		Page<FollowerWithStatus> followers = followRepository.findFollowersWithStatusOrderByCreatedAt(
			userId, userId, PageRequest.of(page, size));

		// 2. userImage N + 1 해결을 위한 배치 조회
		userImageEntityRepository.findAllByUserIdIn(
			followers.stream().map(FollowerWithStatus::id).toList()
		);

		return followers
			.map(u -> {
				final Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(u.id());
				final boolean hasUserImage = userImageUrl.isPresent();

				return new GetFollowersWithStatusResponse(
					u.id(),
					hasUserImage,
					userImageUrl.orElse(null),
					u.name(),
					u.email(),
					u.isFollowing()
				);
			});
	}

	// TODO : 정렬 기준 고려
	// 팔로워 목록 조회 (인증 유저용 - 팔로우 상태 포함)
	@Transactional(readOnly = true)
	public Page<GetFollowersWithStatusResponse> getFollowersWithStatus(
		final long currentUserId,
		final long targetUserId,
		final int page,
		final int size
	) {
		validateUserExists(targetUserId);

		// 1. 팔로워들 조회
		Page<FollowerWithStatus> followers = followRepository.findFollowersWithStatusOrderByCreatedAt(
			targetUserId, currentUserId, PageRequest.of(page, size));

		// 2. userImage N + 1 해결을 위한 배치 조회
		userImageEntityRepository.findAllByUserIdIn(
			followers.stream().map(FollowerWithStatus::id).toList()
		);

		return followers
			.map(u -> {
				final Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(u.id());
				final boolean hasUserImage = userImageUrl.isPresent();

				return new GetFollowersWithStatusResponse(
					u.id(),
					hasUserImage,
					userImageUrl.orElse(null),
					u.name(),
					u.email(),
					u.isFollowing()
				);
			});
	}

	// TODO : 정렬 기준 고려
	// 팔로잉 목록 조회 (미인증 유저용)
	@Transactional(readOnly = true)
	public Page<GetFollowingsResponse> getFollowings(final long userId, final int page, final int size) {
		validateUserExists(userId);

		// 1. 팔로잉들 조회
		Page<UserEntity> followings = followRepository.findFollowingOrderByCreatedAt(userId,
			PageRequest.of(page, size));

		// 2. userImage N + 1 해결을 위한 배치 조회
		userImageEntityRepository.findAllByUserIdIn(
			followings.stream().map(UserEntity::getId).toList()
		);

		// 3. 응답 생성
		return followings
			.map(u -> {
				final Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(u.getId());
				final boolean hasUserImage = userImageUrl.isPresent();

				return new GetFollowingsResponse(
					u.getId(),
					hasUserImage,
					userImageUrl.orElse(null),
					u.getName(),
					u.getEmail()
				);
			});
	}

	// TODO : 정렬 기준 고려
	// 내 팔로잉 목록 조회 (인증 유저용 - isFollowing 항상 true)
	@Transactional(readOnly = true)
	public Page<GetFollowingsWithStatusResponse> getMyFollowings(final long userId, final int page, final int size) {
		validateUserExists(userId);

		// 1. 팔로잉들 조회
		Page<UserEntity> followings = followRepository.findFollowingOrderByCreatedAt(userId, PageRequest.of(page, size));

		// 2. userImage N + 1 해결을 위한 배치 조회
		userImageEntityRepository.findAllByUserIdIn(
			followings.stream().map(UserEntity::getId).toList()
		);

		// 3. 응답 생성
		return followRepository.findFollowingOrderByCreatedAt(userId, PageRequest.of(page, size))
			.map(u -> {
				final Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(u.getId());
				final boolean hasUserImage = userImageUrl.isPresent();

				return new GetFollowingsWithStatusResponse(
					u.getId(),
					hasUserImage,
					userImageUrl.orElse(null),
					u.getName(),
					u.getEmail(),
					true // 내 팔로잉 목록이므로 항상 true
				);
			});
	}

	// TODO : 정렬 기준 고려
	// 팔로잉 목록 조회 (인증 유저용 - 팔로우 상태 포함)
	@Transactional(readOnly = true)
	public Page<GetFollowingsWithStatusResponse> getFollowingsWithStatus(
		final long currentUserId,
		final long targetUserId,
		final int page,
		final int size
	) {
		validateUserExists(targetUserId);

		// 1. 팔로잉들 조회
		Page<FollowingWithStatus> followings = followRepository.findFollowingsWithStatusOrderByCreatedAt(
			targetUserId,
			currentUserId,
			PageRequest.of(page, size)
		);

		// 2. userImage N + 1 해결을 위한 배치 조회
		userImageEntityRepository.findAllByUserIdIn(
			followings.stream().map(FollowingWithStatus::id).toList()
		);

		// 3. 응답 생성
		return followings
			.map(u -> {
				final Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(u.id());
				final boolean hasUserImage = userImageUrl.isPresent();

				return new GetFollowingsWithStatusResponse(
					u.id(),
					hasUserImage,
					userImageUrl.orElse(null),
					u.name(),
					u.email(),
					u.isFollowing()
				);
			});
	}

	// 팔로우 여부 확인
	public boolean isFollowing(final Long currentUserId, final Long targetUserId) {
		// 자기 자신을 팔로우하는지 확인하는 경우
		if (currentUserId.equals(targetUserId)) {
			return false;
		}

		// 대상 사용자 존재 여부 확인
		validateUserExists(targetUserId);

		return followRepository.existsByMyIdAndFollowingId(currentUserId, targetUserId);
	}

	// 유저 존재 체크
	private void validateUserExists(final long userId) {
		if (!userRepository.existsById(userId)) {
			throw new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION);
		}
	}
}
