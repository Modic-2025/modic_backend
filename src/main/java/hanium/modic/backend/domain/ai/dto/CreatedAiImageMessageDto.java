package hanium.modic.backend.domain.ai.dto;

import hanium.modic.backend.domain.image.domain.ImageExtension;

public record CreatedAiImageMessageDto(
	String requestId,
	String imageUrl,
	String imagePath,
	String fullImageName,
	String imageName,
	ImageExtension extension
) {
}