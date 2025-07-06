package hanium.modic.backend.web.postReview.dto.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(NON_NULL)
public record PostReviewDetailResponse(
	String userName,
	boolean hasUserImage,
	String userImageUrl,
	LocalDateTime createdAt,
	Long postReviewId,
	String description,
	List<String> imageUrls
) {
}
