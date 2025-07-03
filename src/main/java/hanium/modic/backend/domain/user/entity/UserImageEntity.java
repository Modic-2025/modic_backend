package hanium.modic.backend.domain.user.entity;

import hanium.modic.backend.domain.image.domain.Image;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "user_image")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserImageEntity extends Image {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
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