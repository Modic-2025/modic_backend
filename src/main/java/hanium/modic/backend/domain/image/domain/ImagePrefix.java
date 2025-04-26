package hanium.modic.backend.domain.image.domain;

import lombok.Getter;

@Getter
public enum ImagePrefix {
	PROFILE("profile"),
	AI_REQUEST("ai-request"),
	AI_RESPONSE("ai-response"),
	;

	private final String prefix;

	ImagePrefix(String prefix) {
		this.prefix = prefix;
	}
}
