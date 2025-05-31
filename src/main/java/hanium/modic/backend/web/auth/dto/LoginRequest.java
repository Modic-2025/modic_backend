package hanium.modic.backend.web.auth.dto;

import hanium.modic.backend.common.validator.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@Email(message = "유효하지 않은 이메일 형식입니다.")
	@NotBlank(message = "이메일은 필수 입력 항목입니다.")
	String email,
	@Password(message = "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다.")
	String password
) {
}
