package hanium.modic.backend.domain.follow.service;

import static hanium.modic.backend.domain.follow.dto.FollowType.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.follow.dto.FollowType;
import hanium.modic.backend.domain.follow.entity.FollowEntity;
import hanium.modic.backend.domain.follow.repository.FollowEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.follow.dto.response.GetFollowersResponse;
import hanium.modic.backend.web.follow.dto.response.GetFollowingsResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowService {
	private final FollowEntityRepository followRepository;
	private final UserEntityRepository userRepository;

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
		boolean exists = followRepository.existsByFollowerIdAndFollowingId(me.getId(), targetId);
		if (type == FOLLOW && !exists) {
			followRepository.save(FollowEntity.builder()
				.follower(me)
				.following(target)
				.build());
		} else if (type == UNFOLLOW && exists) {
			followRepository.deleteByFollowerIdAndFollowingId(me.getId(), targetId);
		}
	}

	// 팔로워 목록 조회
	public Page<GetFollowersResponse> getFollowers(final long userId, final Pageable pageable) {
		return followRepository.findFollowers(userId, pageable)
			.map(u -> new GetFollowersResponse(u.getName(), u.getEmail()));
	}

	// 팔로잉 목록 조회
	public Page<GetFollowingsResponse> getFollowings(final long userId, final Pageable pageable) {
		return followRepository.findFollowing(userId, pageable)
			.map(u -> new GetFollowingsResponse(u.getName(), u.getEmail()));
	}
}
