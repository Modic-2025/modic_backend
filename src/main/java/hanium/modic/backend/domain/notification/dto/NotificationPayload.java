package hanium.modic.backend.domain.notification.dto;

public record NotificationPayload(
	Long senderId,
	String senderNickname,
	String senderEmail,
	Long postId,
	String postTitle,
	Long amount,
	String reviewContent
) {

	public static NotificationPayload empty() {
		return new NotificationPayload(
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);
	}

	public static Builder builder(Long senderId, String senderNickname, String senderEmail) {
		return new Builder(senderId, senderNickname, senderEmail);
	}

	public static class Builder {
		private final Long senderId;
		private final String senderNickname;
		private final String senderEmail;

		private Long postId;
		private String postTitle;
		private Long amount;
		private String reviewContent;

		public Builder(
			Long senderId,
			String senderNickname,
			String senderEmail
		) {
			this.senderId = senderId;
			this.senderNickname = senderNickname;
			this.senderEmail = senderEmail;
		}

		public Builder postId(Long postId) {
			this.postId = postId;
			return this;
		}

		public Builder postTitle(String postTitle) {
			this.postTitle = postTitle;
			return this;
		}

		public Builder amount(Long amount) {
			this.amount = amount;
			return this;
		}

		public Builder reviewContent(String reviewContent) {
			this.reviewContent = reviewContent;
			return this;
		}

		public NotificationPayload build() {
			return new NotificationPayload(
				senderId,
				senderNickname,
				senderEmail,
				postId,
				postTitle,
				amount,
				reviewContent
			);
		}
	}
}