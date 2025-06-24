package hanium.modic.backend.web.ai.dto.response;

import hanium.modic.backend.domain.ai.domain.AiRequestEntity;

public record RequestAiImageGenerationResponse(
	Long imageId,
	String requestId
) {

	public static RequestAiImageGenerationResponse from(AiRequestEntity aiRequestEntity) {
		return new RequestAiImageGenerationResponse(
			aiRequestEntity.getId(),
			aiRequestEntity.getRequestId()
		);
	}
}