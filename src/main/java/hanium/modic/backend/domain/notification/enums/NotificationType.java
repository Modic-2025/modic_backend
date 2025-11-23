package hanium.modic.backend.domain.notification.enums;

import hanium.modic.backend.domain.notification.dto.NotificationPayload;

public enum NotificationType {

	COIN_RECEIVED {
		@Override
		public String generateTitle(NotificationPayload payload) {
			return "코인이 도착했습니다!";
		}

		@Override
		public String generateBody(NotificationPayload payload) {
			return sender(payload) + "님이 " + payload.amount() + " 코인을 보내셨습니다.";
		}
	},

	POST_PURCHASED_BY_COIN {
		@Override
		public String generateTitle(NotificationPayload payload) {
			return "'" + payload.postTitle() + "' 게시글이 구매되었습니다";
		}

		@Override
		public String generateBody(NotificationPayload payload) {
			return sender(payload)
				+ "님이 "
				+ payload.amount()
				+ " 코인으로 게시글을 구매했습니다.";
		}
	},

	POST_PURCHASED_BY_TICKET {
		@Override
		public String generateTitle(NotificationPayload payload) {
			return "'" + payload.postTitle() + "' 게시글이 구매되었습니다";
		}

		@Override
		public String generateBody(NotificationPayload payload) {
			return sender(payload)
				+ "님이 "
				+ payload.amount()
				+ " 티켓으로 게시글을 구매했습니다.";
		}
	},

	POST_REVIEWED {
		@Override
		public String generateTitle(NotificationPayload payload) {
			return "'" + payload.postTitle() + "' 게시글에 새로운 후기가 작성되었습니다.";
		}

		@Override
		public String generateBody(NotificationPayload payload) {
			return sender(payload) + "님의 후기: \"" + payload.reviewContent() + "\"";
		}
	},

	FOLLOWED {
		@Override
		public String generateTitle(NotificationPayload payload) {
			return sender(payload) + "님이 회원님을 팔로우하기 시작했습니다.";
		}

		@Override
		public String generateBody(NotificationPayload payload) {
			return sender(payload) + "님이 회원님을 새롭게 팔로우했어요.";
		}
	},

	DERIVED_POST_CREATED {
		@Override
		public String generateTitle(NotificationPayload payload) {
			return "'" + payload.postTitle() + "' 을 이용하여 2차 창작물이 등록되었습니다.";
		}

		@Override
		public String generateBody(NotificationPayload payload) {
			return sender(payload)
				+ "님이 '" + payload.postTitle() + "'을 이용하여 2차 창작물을 등록했습니다.";
		}
	},

	LIKED {
		@Override
		public String generateTitle(NotificationPayload payload) {
			return sender(payload) + "님이 '" + payload.postTitle() + "' 게시글에 좋아요를 눌렀습니다.";
		}

		@Override
		public String generateBody(NotificationPayload payload) {
			return sender(payload) + "님이 '" + payload.postTitle() + "' 게시글에 좋아요를 눌렀습니다.";
		}
	}
	;

	public abstract String generateTitle(NotificationPayload payload);

	public abstract String generateBody(NotificationPayload payload);

	// ★ 닉네임(이메일) 포맷 메서드
	protected static String sender(NotificationPayload payload) {
		if (payload.senderEmail() == null) {
			return payload.senderNickname();
		}
		return payload.senderNickname() + "(" + payload.senderEmail() + ")";
	}
}
