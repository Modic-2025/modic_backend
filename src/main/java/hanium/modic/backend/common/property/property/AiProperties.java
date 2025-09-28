package hanium.modic.backend.common.property.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

	private OpenAi openai = new OpenAi();
	private Claude claude = new Claude();

	@Getter
	@Setter
	public static class OpenAi {
		private String apiKey;
		private String model;
	}

	@Getter
	@Setter
	public static class Claude {
		private String apiKey;
		private String model;
	}
}