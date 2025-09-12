package hanium.modic.backend.domain.ai.aiChat.entity;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.time.LocalDateTime;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.common.error.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
	name = "ai_chat_rooms",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_ai_chat_rooms_user_post", // 제약조건 이름
			columnNames = {"user_id", "post_id"}       // 유니크 컬럼 지정
		)
	})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatRoomEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	// 남은 이미지 생성 횟수
	@Column(name = "remaining_generations", nullable = false)
	private Integer remainingGenerations;

	// 채팅 내용 요약본
	@Column(name = "chat_summary", columnDefinition = "TEXT")
	private String chatSummary;

	// 컨텍스트(대화 내용) 초기화 시각, 이를 통해 대화 내용이 초기화된 이후의 메시지만 컨텍스트로 사용
	@Column(name = "context_reset_at")
	private LocalDateTime contextResetAt;

	@Builder
	private AiChatRoomEntity(Long userId, Long postId, Integer remainingGenerations,
		String chatSummary, LocalDateTime contextResetAt) {
		this.userId = userId;
		this.postId = postId;
		this.remainingGenerations = remainingGenerations;
		this.chatSummary = chatSummary;
		this.contextResetAt = contextResetAt;
	}

	// 남은 이미지 생성 횟수 감소, 락과 함께 사용해야 함
	public void decreaseRemainingGenerations() throws AppException {
		if (hasRemainingGenerations()) {
			this.remainingGenerations--;
		} else {
			throw new AppException(REMAINING_GENERATIONS_NOT_ENOUGH_EXCEPTION);
		}
	}

	// 이미지 생성 횟수 증가, 락과 함께 사용해야 함
	public void increaseRemainingGenerations() {
		this.remainingGenerations++;
	}

	// 남은 이미지 생성 횟수 확인
	public boolean hasRemainingGenerations() {
		return this.remainingGenerations > 0;
	}

	// 채팅 요약 업데이트
	public void updateChatSummary(String chatSummary) {
		this.chatSummary = chatSummary;
	}

	// 컨텍스트 초기화
	public void resetContext() {
		this.contextResetAt = LocalDateTime.now();
		this.chatSummary = ""; // 요약도 초기화
	}
}