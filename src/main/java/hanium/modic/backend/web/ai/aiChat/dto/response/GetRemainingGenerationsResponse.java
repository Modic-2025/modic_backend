package hanium.modic.backend.web.ai.aiChat.dto.response;

public record GetRemainingGenerationsResponse(
	Long aiImagePermissionId,
	Integer remainingGenerations
) {
}