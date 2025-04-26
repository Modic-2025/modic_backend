package hanium.modic.backend.domain.post.entity;

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

	public void updatePost(PostEntity postEntity) {
		this.postId = postEntity.getId();
	}
}
