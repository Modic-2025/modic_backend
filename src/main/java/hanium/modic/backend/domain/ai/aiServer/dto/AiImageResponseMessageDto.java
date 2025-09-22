package hanium.modic.backend.domain.ai.aiServer.dto;

import hanium.modic.backend.domain.image.domain.ImageExtension;

public record AiImageResponseMessageDto(
	boolean isSuccess,
	String requestId,
	boolean isImageGenerated, // 이미지가 생성되었는지 여부에 따라 응답이 달라진다.
	String textContext, // isImageGenerated = false, GPT의 응답
	String imagePath, // isImageGenerated = true
	String fullImageName, // isImageGenerated = true
	String imageName, // isImageGenerated = true
	ImageExtension extension, // isImageGenerated = true
	String description, // 이미지에 대한 설명, isImageGenerated = true
	String chatSummary, // 기존 채팅 요약본, isImageGenerated = true
	Boolean fromStyleImage // 원본 이미지로부터 파생되었는지 여부, isImageGenerated = true
) {
}

