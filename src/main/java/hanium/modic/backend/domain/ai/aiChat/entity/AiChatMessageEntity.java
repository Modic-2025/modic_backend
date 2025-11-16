package hanium.modic.backend.domain.ai.aiChat.entity;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 요청과 응답을 모두 포함하는 채팅 메시지 엔티티
 * 텍스트와 이미지(aiRequestId)를 함께 관리
 */
@Entity
@Table(name = "ai_chat_messages",
	indexes = {
		@Index(name = "idx_user_post_order", columnList = "user_id, post_id, message_order"),
		@Index(name = "idx_user_post_created", columnList = "user_id, post_id, create_at")
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatMessageEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "ai_chat_room_id", nullable = false)
	private Long aiChatRoomId;

	@Column(name = "message_order", nullable = false)
	private Long messageOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "sender_type", nullable = false)
	private SenderType senderType;

	@Column(name = "text_content", columnDefinition = "TEXT", nullable = false)
	private String textContent;

	@Column(name = "ai_chat_image_id")
	private Long aiChatImageId; // 이미지 첨부 시에만 값 존재, 없으면 null, null에 따라 조회 로직 조심.

	@Column(name = "request_id", nullable = false)
	private String requestId; // ai 요청 아이디, 이 ID를 통해 SSE 연결

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AiImageStatus status = AiImageStatus.REQUEST_PENDING;

	@Builder
	public AiChatMessageEntity(
		Long userId,
		Long postId,
		Long aiChatRoomId,
		Long messageOrder,
		SenderType senderType,
		String textContent,
		Long aiChatImageId,
		String requestId,
		AiImageStatus status
	) {
		this.userId = userId;
		this.postId = postId;
		this.aiChatRoomId = aiChatRoomId;
		this.messageOrder = messageOrder;
		this.senderType = senderType;
		this.textContent = textContent;
		this.aiChatImageId = aiChatImageId;
		this.requestId = requestId;
		this.status = status;
	}

	// 요청 상태 업데이트
	public void updateStatus(AiImageStatus status) {
		this.status = status;
	}

	// 텍스트 내용 업데이트
	public void updateTextContent(String textContent) {
		this.textContent = textContent;
	}

	// 이미지를 갖고 있는지 여부
	public boolean hasImage() {
		return this.aiChatImageId != null;
	}
}