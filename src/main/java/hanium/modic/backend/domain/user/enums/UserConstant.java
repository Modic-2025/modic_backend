package hanium.modic.backend.domain.user.enums;

import lombok.Getter;

@Getter
public enum UserConstant {
	ANONYMOUS("익명"),
	;

	private final String name;

	UserConstant(String name) {
		this.name = name;
	}
}
