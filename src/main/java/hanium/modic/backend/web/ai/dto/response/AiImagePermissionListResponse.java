package hanium.modic.backend.web.ai.dto.response;

import java.util.List;

public record AiImagePermissionListResponse(
	List<AiImagePermissionResponse> permissions,
	Integer totalCount
) {
}