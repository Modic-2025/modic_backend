package hanium.modic.backend.web.postReview.dto.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(NON_NULL)
public record PostReviewCommentResponse(
	Long postReviewCommentId,
	Long userId,
	String userName,
	LocalDateTime createdAt,
	String text,
	boolean hasUserImage,
	String userImageUrl
) {
}