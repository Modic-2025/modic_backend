package hanium.modic.backend.web.auth.dto;

import hanium.modic.backend.domain.auth.dto.Token;

public record LoginResponse(
	String accessToken,
	String refreshToken
) {
	public static LoginResponse from(Token token) {
		return new LoginResponse(
			token.accessToken(),
			token.refreshToken()
		);
	}
}
