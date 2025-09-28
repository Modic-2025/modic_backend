package hanium.modic.backend.web.ai.aiChat.dto.response;

import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;

public record AiRequestStatusResponse(
	AiImageStatus status
) {
}