package hanium.modic.backend.domain.chat.entity;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "chat_rooms")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomEntity extends BaseEntity {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user1_id", nullable = false)
	private Long user1Id;

	@Column(name = "user2_id", nullable = false)
	private Long user2Id;

	@Column(name = "user1_deleted", nullable = false)
	private Boolean user1Deleted = false;

	@Column(name = "user2_deleted", nullable = false)
	private Boolean user2Deleted = false;

	@Builder
	private ChatRoomEntity(UserEntity user1, UserEntity user2) {
		this.user1Id = user1.getId();
		this.user2Id = user2.getId();
		this.user1Deleted = false;
		this.user2Deleted = false;
	}

	// 유저 소프트 삭제
	public void deleteForUser(Long userId) {
		if (user1Id.equals(userId)) {
			this.user1Deleted = true;
		} else if (user2Id.equals(userId)) {
			this.user2Deleted = true;
		}
	}

	// 상대방 유저 반환
	public Long getOpponent(Long userId) {
		if (user1Id.equals(userId)) {
			return user2Id;
		}
		return user1Id;
	}

	// 채팅방을 모든 유저가 삭제했는지 확인
	public boolean isDeleted() {
		return user1Deleted && user2Deleted;
	}

	// 특정 유저가 채팅방을 삭제했는지 확인
	public boolean isDeletedForUser(Long userId) {
		if (user1Id.equals(userId)) {
			return user1Deleted;
		} else if (user2Id.equals(userId)) {
			return user2Deleted;
		}
		return false;
	}
}