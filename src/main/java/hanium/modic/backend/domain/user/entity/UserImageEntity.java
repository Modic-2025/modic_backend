package hanium.modic.backend.domain.user.entity;

import org.springframework.stereotype.Indexed;

import hanium.modic.backend.domain.image.domain.Image;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "user_image",
	indexes = {
		@Index(name = "idx_user_image_user_id", columnList = "user_id")
	})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserImageEntity extends Image {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true) // 유저별로 이미지는 하나만 존재
	private Long userId;

	@Builder
	private UserImageEntity(
		UserEntity user,
		String imagePath,
		String imageUrl,
		String fullImageName,
		String imageName,
		ImageExtension extension,
		ImagePrefix imagePurpose
	) {
		super(imagePath, imageUrl, fullImageName, imageName, extension, imagePurpose);
		this.userId = user.getId();
	}
}