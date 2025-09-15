package hanium.modic.backend.domain.ai.aiServer.dto;

import hanium.modic.backend.domain.image.domain.ImageExtension;

public record AiImageResponseMessageDto(
	boolean isSuccess,
	String requestId,
	String imagePath,
	String fullImageName,
	String imageName,
	ImageExtension extension,
	String description, // 이미지에 대한 설명
	String chatSummary // 기존 채팅 요약본
) {
}

