package hanium.modic.backend.web.auth.dto;

public record VerifyEmailCodeResponse(
	String email,
	boolean isVerified
) {
	public static VerifyEmailCodeResponse of(final String email, boolean isVerified) {
		return new VerifyEmailCodeResponse(email, isVerified);
	}
}
