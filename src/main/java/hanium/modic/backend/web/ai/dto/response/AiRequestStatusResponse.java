package hanium.modic.backend.web.ai.dto.response;

import hanium.modic.backend.domain.ai.enums.AiImageStatus;

public record AiRequestStatusResponse(
	AiImageStatus status
) {
}