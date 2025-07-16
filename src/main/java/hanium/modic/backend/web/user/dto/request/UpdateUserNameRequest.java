package hanium.modic.backend.web.user.dto.request;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserNameRequest(
	@NotNull(message = "이름은 필수입니다.")
	@Length(min = 2, max = 20, message = "이름은 2자 이상 20자 이하로 입력해주세요.")
	String name
) {
}
