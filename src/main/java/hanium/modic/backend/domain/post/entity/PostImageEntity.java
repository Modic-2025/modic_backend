package hanium.modic.backend.domain.post.entity;

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

@Table(name = "post_image")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImageEntity extends Image {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = true)
	private Long postId;

	@Builder
	public PostImageEntity(
		String imagePath,
		String imageUrl,
		String fullImageName,
		String imageName,
		ImageExtension extension,
		ImagePrefix imagePurpose,
		PostEntity postEntity
	) {
		super(imagePath, imageUrl, fullImageName, imageName, extension, imagePurpose);

		if (postEntity == null) {
			this.postId = null;
		} else {
			this.postId = postEntity.getId();
		}
	}

	// 포스트ID 변경
	public void updatePost(PostEntity postEntity) {
		// 이미 Post에 속해있는 이미지에 대해 다시 Post를 설정할 경우 예외 발생
		// 만약 PostId가 있지만 PostEntity값과 같으면 예외발생x
		// Image에 해당하는 Post가 없었을 경우는 무관하지만, 있는 상황에서 다시 설정하려하는 것은 도용이다.
		if (postId != null && !Objects.equals(this.postId, postEntity.getId())) {
			throw new AppException(IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION);

		}
		this.postId = postEntity.getId();

	}
}
