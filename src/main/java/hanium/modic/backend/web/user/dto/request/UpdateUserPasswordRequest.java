package hanium.modic.backend.web.user.dto.request;

import hanium.modic.backend.common.annotation.validator.Password;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserPasswordRequest(
	@Password(message = "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다.")
	String oldPassword,
	@Password(message = "비밀번호는 8자 이상 20자 이하, 영문, 숫자, 특수문자를 포함해야 합니다.")
	String newPassword,
	@NotBlank(message = "토큰은 필수입니다.") String updateToken
) {
}
