package hanium.modic.backend.domain.ai.domain;

import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.image.domain.Image;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "ai_request")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRequestEntity extends Image {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "request_id", nullable = false)
	private String requestId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AiImageStatus status = AiImageStatus.PENDING;

	@Builder
	public AiRequestEntity(
		String imagePath,
		String imageUrl,
		String fullImageName,
		String imageName,
		ImageExtension extension,
		ImagePrefix imagePurpose,
		String requestId,
		AiImageStatus status) {
		super(imagePath, imageUrl, fullImageName, imageName, extension, imagePurpose);
		this.requestId = requestId;
		this.status = status != null ? status : AiImageStatus.PENDING;
	}
}