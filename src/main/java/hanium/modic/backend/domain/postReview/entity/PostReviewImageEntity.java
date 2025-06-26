package hanium.modic.backend.domain.postReview.entity;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.Objects;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.Image;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "post_review_image")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReviewImageEntity extends Image {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_review_id", nullable = true)
	private Long postReviewId;

	@Builder
	private PostReviewImageEntity(
		String imagePath,
		String imageUrl,
		String fullImageName,
		String imageName,
		ImageExtension extension,
		ImagePrefix imagePurpose,
		PostReviewEntity postReviewEntity
	) {
		super(imagePath, imageUrl, fullImageName, imageName, extension, imagePurpose);
		this.postReviewId = (postReviewEntity == null) ? null : postReviewEntity.getId();
	}

	public void updatePostReview(PostReviewEntity postReview) {
		if (postReviewId != null && !Objects.equals(this.postReviewId, postReview.getId())) {
			throw new AppException(IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION);
		}
		this.postReviewId = postReview.getId();
	}
}