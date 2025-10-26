package hanium.modic.backend.domain.ai.aiChat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_chat_message_order",
	uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AiChatMessageOrderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "last_order", nullable = false)
	private Long lastOrder;

	public AiChatMessageOrderEntity(Long userId, Long postId, Long lastOrder) {
		this.userId = userId;
		this.postId = postId;
		this.lastOrder = lastOrder;
	}

	/** 순번 증가 */
	public void increment() {
		this.lastOrder = this.lastOrder + 1;
	}
}