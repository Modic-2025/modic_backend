package hanium.modic.backend.domain.postReview.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.postReview.dto.PostReviewCommentDto;
import hanium.modic.backend.domain.postReview.entity.PostReviewCommentEntity;
import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;
import hanium.modic.backend.domain.postReview.repository.PostReviewCommentRepository;
import hanium.modic.backend.domain.postReview.repository.PostReviewRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.service.UserImageService;
import hanium.modic.backend.web.postReview.dto.response.PostReviewCommentResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostReviewCommentService {

	private final PostReviewCommentRepository commentRepository;
	private final PostReviewRepository postReviewRepository;
	private final UserEntityRepository userEntityRepository;
	private final UserImageService userImageService;
	private final PostReviewCommentRepository postReviewCommentRepository;

	// 게시글 리뷰 댓글 목록 조회
	public Page<PostReviewCommentResponse> getComments(final long postReviewId, final int page, final int size) {
		// 댓글 조회
		Page<PostReviewCommentDto> prcs = postReviewCommentRepository.findAllByPostReviewIdOrderByCreateAtDesc(
			postReviewId, PageRequest.of(page, size));

		// 댓글 userId에 해당하는 유저들의 프로필 이미지 조회
		return prcs.map(prc -> {
			final Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(prc.userId());
			final boolean hasUserImage = userImageUrl.isPresent();

			return new PostReviewCommentResponse(
				prc.postReviewCommentId(),
				prc.userId(),
				prc.userName(),
				prc.createdAt(),
				prc.text(),
				hasUserImage,
				userImageUrl.orElse(null)
			);
		});
	}

	// 게시글 리뷰 댓글 생성
	@Transactional
	public void createComment(long userId, long postReviewId, String text) {
		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
		PostReviewEntity review = postReviewRepository.findById(postReviewId)
			.orElseThrow(() -> new AppException(POST_REVIEW_NOT_FOUND_EXCEPTION));

		commentRepository.save(PostReviewCommentEntity.builder()
			.user(user)
			.postReview(review)
			.text(text)
			.build());
	}

	// 게시글 리뷰 댓글 수정
	@Transactional
	public void updateComment(long userId, long commentId, String text) {
		PostReviewCommentEntity comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new AppException(POST_REVIEW_COMMENT_NOT_FOUND_EXCEPTION));

		validateMyComment(userId, comment.getUserId());
		comment.updateText(text);
	}

	// 게시글 리뷰 댓글 삭제
	@Transactional
	public void deleteComment(long userId, long commentId) {
		PostReviewCommentEntity comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new AppException(POST_REVIEW_COMMENT_NOT_FOUND_EXCEPTION));

		validateMyComment(userId, comment.getUserId());
		commentRepository.delete(comment);
	}

	// 댓글 작성자가 본인인지 확인
	private void validateMyComment(Long currentUserId, Long writerId) {
		if (!Objects.equals(currentUserId, writerId)) {
			throw new AppException(USER_ROLE_EXCEPTION);
		}
	}
}
