package hanium.modic.backend.domain.ai.aiServer.entity;

import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
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

@Table(name = "ai_chat_images")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatImageEntity extends Image {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AiImageStatus status;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "ai_chat_room_id", nullable = false)
	private Long aiChatRoomId;

	@Column(name = "from_origin_image", nullable = false)
	private Boolean fromOriginImage; // 원본 이미지로부터 파생되었는지

	@Column(name = "description", columnDefinition = "TEXT")
	private String description; // 이미지에 대해 생성된 설명

	@Builder
	public AiChatImageEntity(
		String imagePath,
		String fullImageName,
		String imageName,
		ImageExtension extension,
		ImagePrefix imagePurpose,
		AiImageStatus status,
		Long userId,
		Long postId,
		Long aiChatRoomId,
		Boolean fromOriginImage,
		String description
	) {
		super(imagePath, fullImageName, imageName, extension, imagePurpose);
		this.status = status;
		this.userId = userId;
		this.postId = postId;
		this.aiChatRoomId = aiChatRoomId;
		this.fromOriginImage = fromOriginImage;
		this.description = description;
	}
}