package hanium.modic.backend.web.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
	@Email(message = "이메일 형식으로 요청해주세요.")
	@NotNull(message = "이메일은 필수입니다.")
	String email,

	String name,
	@NotNull(message = "비밀번호는 필수입니다.")
	String password
) {
}
