package hanium.modic.backend.domain.auth.dto;

public record Token(
	String accessToken,
	String refreshToken
) {
}
