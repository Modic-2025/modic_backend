package hanium.modic.backend.domain.profile.service;

import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.follow.repository.FollowEntityRepository;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.profile.dto.GetMyProfileResponse;
import hanium.modic.backend.web.profile.dto.GetProfileResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService {
	private final UserEntityRepository userRepository;
	private final PostEntityRepository postRepository;
	private final FollowEntityRepository followRepository;

	// 내 프로필 조회(코인 함께 조회)
	public GetMyProfileResponse getMyProfile(final UserEntity user) {
		final long postCount = postRepository.countByUserId(user.getId()); // TODO: 추후 개선 필요, count 쿼리 없애는 방법
		final long followingCount = followRepository.countByMyId(user.getId()); // TODO: 추후 개선 필요
		final long followerCount = followRepository.countByFollowingId(user.getId()); // TODO: 추후 개선 필요

		return new GetMyProfileResponse(
			user.getEmail(),
			user.getName(),
			"프로필 이미지 url", // TODO: 프로필 이미지 URL을 실제로 가져오는 로직 추가 필요
			postCount,
			followerCount,
			followingCount,
			user.getCoin() // TODO: 코인에 대한 락 고려 필요
		);
	}

	// 사용자 프로필 조회
	public GetProfileResponse getProfile(final long userId) {
		UserEntity user = userRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		final long postCount = postRepository.countByUserId(userId); // TODO: 추후 개선 필요, count 쿼리 없애는 방법
		final long followingCount = followRepository.countByMyId(userId); // TODO: 추후 개선 필요
		final long followerCount = followRepository.countByFollowingId(userId); // TODO: 추후 개선 필요

		return new GetProfileResponse(
			user.getEmail(),
			user.getName(),
			"프로필 이미지 url", // TODO: 프로필 이미지 URL을 실제로 가져오는 로직 추가 필요
			postCount,
			followerCount,
			followingCount
		);
	}
}
