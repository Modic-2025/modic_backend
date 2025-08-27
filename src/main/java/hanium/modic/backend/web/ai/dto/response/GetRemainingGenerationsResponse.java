package hanium.modic.backend.web.ai.dto.response;

public record GetRemainingGenerationsResponse(
	Long aiImagePermissionId,
	Integer remainingGenerations
) {
}