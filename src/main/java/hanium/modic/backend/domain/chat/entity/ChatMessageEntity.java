package hanium.modic.backend.domain.chat.entity;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "chat_messages")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntity extends BaseEntity {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_id", unique = true, nullable = false)
	private String messageId;

	@Column(name = "chatRoom_id", nullable = false)
	private Long chatRoomId;

	@Column(name = "sender_id", nullable = false)
	private Long senderId;

	@Lob
	@Column(name = "message", nullable = false)
	private String message;

	@Column(name = "is_read", nullable = false)
	private Boolean isRead = false;

	@Builder
	private ChatMessageEntity(String messageId, ChatRoomEntity chatRoom, UserEntity sender, String message) {
		this.messageId = messageId;
		this.chatRoomId = chatRoom.getId();
		this.senderId = sender.getId();
		this.message = message;
		this.isRead = false;
	}

	// 메시지 읽음 처리
	public void markAsRead() {
		this.isRead = true;
	}
}