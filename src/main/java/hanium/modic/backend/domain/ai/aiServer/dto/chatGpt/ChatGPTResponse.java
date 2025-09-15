package hanium.modic.backend.domain.ai.aiServer.dto.chatGpt;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChatGPTResponse {

	private String id;
	private String object;
	private Long created;
	private String model;
	private List<Choice> choices;
	private Usage usage;

	@Data
	@Getter
	public static class Choice {
		private int index;
		private Message message;
		private String finishReason;
	}

	@Data
	public static class Message {
		private String role;
		private String content; // JSON 형식의 감정 분석 결과를 담음
		private String refusal;
	}

	@Data
	public static class Usage {
		private int promptTokens;
		private int completionTokens;
		private int totalTokens;
		private PromptTokensDetails promptTokensDetails;
		private CompletionTokensDetails completionTokensDetails;

		@Data
		public static class PromptTokensDetails {
			private int cachedTokens;
			private int audioTokens;
		}

		@Data
		public static class CompletionTokensDetails {
			private int reasoningTokens;
			private int audioTokens;
			private int acceptedPredictionTokens;
			private int rejectedPredictionTokens;
		}
	}
}
