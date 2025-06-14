package hanium.modic.backend.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailCodeRequest(
	@Email(message = "유효하지 않은 이메일 형식입니다.")
	@NotBlank(message = "값을 입력해주세요.")
	String email,
	@NotBlank(message = "인증 코드를 입력해주세요.")
	String code
) {
}
