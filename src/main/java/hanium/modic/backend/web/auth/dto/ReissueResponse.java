package hanium.modic.backend.web.auth.dto;

import hanium.modic.backend.domain.auth.dto.Token;

public record ReissueResponse(
	String accessToken,
	String refreshToken
) {
	public static ReissueResponse from(Token token) {
		return new ReissueResponse(token.accessToken(), token.refreshToken());
	}
}
