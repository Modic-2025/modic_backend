package hanium.modic.backend.domain.ai.aiChat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;

/**
 * 채팅 메시지 리포지토리
 */
public interface AiChatMessageRepository extends JpaRepository<AiChatMessageEntity, Long> {

	/**
	 * 특정 사용자-포스트의 메시지 목록 조회 (페이지네이션, 오래된 순)
	 */
	@Query("SELECT cm FROM AiChatMessageEntity cm " +
		"WHERE cm.userId = :userId AND cm.postId = :postId " +
		"ORDER BY cm.messageOrder ASC")
	Page<AiChatMessageEntity> findByUserIdAndPostIdOrderByMessageOrderAsc(
		@Param("userId") Long userId,
		@Param("postId") Long postId,
		Pageable pageable
	);

	/**
	 * AI 컨텍스트용 최근 N개 메시지 조회
	 */
	@Query("SELECT cm FROM AiChatMessageEntity cm " +
		"WHERE cm.userId = :userId AND cm.postId = :postId " +
		"ORDER BY cm.messageOrder DESC")
	List<AiChatMessageEntity> findTopNByUserIdAndPostIdOrderByMessageOrderDesc(
		@Param("userId") Long userId,
		@Param("postId") Long postId,
		Pageable pageable);

	/**
	 * 컨텍스트 초기화 시점 이후의 메시지들만 조회
	 */
	@Query("SELECT cm FROM AiChatMessageEntity cm " +
		"WHERE cm.userId = :userId AND cm.postId = :postId " +
		"AND cm.createAt > :contextResetAt " +
		"ORDER BY cm.messageOrder DESC")
	List<AiChatMessageEntity> findByUserIdAndPostIdAfterContextReset(
		@Param("userId") Long userId,
		@Param("postId") Long postId,
		@Param("contextResetAt") LocalDateTime contextResetAt,
		Pageable pageable);

	/**
	 * 특정 사용자-포스트의 다음 메시지 순서 조회
	 */
	@Query("SELECT COALESCE(MAX(cm.messageOrder), 0) + 1 " +
		"FROM AiChatMessageEntity cm " +
		"WHERE cm.userId = :userId AND cm.postId = :postId")
	Long findNextMessageOrder(
		@Param("userId") Long userId,
		@Param("postId") Long postId);


	Optional<AiChatMessageEntity> findByRequestIdAndSenderType(String requestId, SenderType senderType);

	/**
	 * 특정 사용자-포스트의 메시지 개수 조회
	 */
	long countByUserIdAndPostId(Long userId, Long postId);
}