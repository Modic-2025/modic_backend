package hanium.modic.backend.domain.image.domain;

import lombok.Getter;

@Getter
public enum ImagePrefix {
	PROFILE("profile"),
	POST_REVIEW("post-review"),
	AI_REQUEST("ai-request"),
	AI_RESPONSE("ai-response"),
	POST("post"),
	;

	private final String prefix;

	ImagePrefix(String prefix) {
		this.prefix = prefix;
	}
}
