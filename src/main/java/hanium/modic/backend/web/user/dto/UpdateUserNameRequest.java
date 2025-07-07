package hanium.modic.backend.web.user.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserNameRequest(
	@NotBlank(message = "이름은 필수입니다.")
	@Length(min = 1, max = 20, message = "이름은 2자 이상 20자 이하로 입력해주세요.")
	String name
) {
}
