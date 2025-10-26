package hanium.modic.backend.web.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetUserPasswordRequest(
	@Email(message = "유효하지 않은 이메일 형식입니다.")
	@NotBlank(message = "이메일은 필수 입력 항목입니다.")
	String email,
	@NotBlank(message = "인증 코드를 입력해주세요.")
	String code
) {
}
