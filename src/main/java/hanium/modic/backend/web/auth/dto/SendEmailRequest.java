package hanium.modic.backend.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailRequest(
	@Email(message = "이메일 형식이 아닙니다.")
	@NotBlank(message = "값을 입력해주세요.")
	String email
) {
}
