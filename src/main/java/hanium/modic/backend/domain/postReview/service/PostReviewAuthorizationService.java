package hanium.modic.backend.domain.postReview.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.repository.AiImagePermissionRepository;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import lombok.RequiredArgsConstructor;

/**
 * 포스트 리뷰 권한 검증 서비스
 * 사용자가 특정 그림체(Post)에 대한 리뷰를 작성할 수 있는지 검증
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReviewAuthorizationService {

	private final AiImagePermissionRepository aiImagePermissionRepository;
	private final PostEntityRepository postEntityRepository;

	/**
	 * 사용자가 특정 게시물(그림체)에 대해 리뷰를 작성할 수 있는지 확인
	 *
	 * @param userId 사용자 ID
	 * @param postId 게시물 ID
	 * @return 리뷰 작성 가능 여부
	 */
	public boolean canUserReviewPost(Long userId, Long postId) {
		// 게시물 존재 확인
		PostEntity post = postEntityRepository.findById(postId)
			.orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND_EXCEPTION));

		// 자신의 게시물에는 리뷰 작성 불가
		if (post.getUserId().equals(userId)) {
			return false;
		}

		// AiImagePermission 존재 여부 확인 (해당 그림체를 사용한 이력이 있는지)
		return aiImagePermissionRepository.existsByUserIdAndPostId(userId, postId);
	}

	/**
	 * 사용자가 특정 게시물(그림체)에 대해 리뷰를 작성할 수 있는지 검증하고,
	 * 권한이 없으면 예외를 발생시킴
	 *
	 * @param userId 사용자 ID
	 * @param postId 게시물 ID
	 * @throws AppException 권한이 없는 경우
	 */
	public void validateUserCanReviewPost(Long userId, Long postId) {
		// 게시물 존재 확인
		PostEntity post = postEntityRepository.findById(postId)
			.orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND_EXCEPTION));

		// 자신의 게시물에는 리뷰 작성 불가
		if (post.getUserId().equals(userId)) {
			throw new AppException(ErrorCode.CANNOT_REVIEW_OWN_POST_EXCEPTION);
		}

		// AiImagePermission 존재 여부 확인 (해당 그림체를 사용한 이력이 있는지)
		if (!aiImagePermissionRepository.existsByUserIdAndPostId(userId, postId)) {
			throw new AppException(ErrorCode.POST_REVIEW_PERMISSION_DENIED_EXCEPTION);
		}
	}
}