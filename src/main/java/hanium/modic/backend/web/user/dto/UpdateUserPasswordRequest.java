package hanium.modic.backend.web.user.dto;

import hanium.modic.backend.common.annotation.validator.Password;

public record UpdateUserPasswordRequest(
	@Password(message = "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다.")
	String oldPassword,
	@Password(message = "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다.")
	String newPassword
) {
}
