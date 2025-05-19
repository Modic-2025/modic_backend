package hanium.modic.backend.web.user.dto;

import org.hibernate.validator.constraints.Length;

import hanium.modic.backend.common.validator.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
	@Email(message = "이메일 형식으로 요청해주세요.")
	@NotNull(message = "이메일은 필수입니다.")
	String email,

	@NotNull(message = "이름은 필수입니다.")
	@Length(min = 1, max = 20, message = "이름은 2자 이상 20자 이하로 입력해주세요.")
	String name,
	@Password(message = "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다.")
	String password
) {
}
