package hanium.modic.backend.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import hanium.modic.backend.domain.chat.entity.ChatMessageEntity;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

	Optional<ChatMessageEntity> findByMessageId(String messageId);

	@Query("SELECT cm FROM ChatMessageEntity cm " +
		"WHERE cm.chatRoomId = :chatRoomId " +
		"ORDER BY cm.createAt DESC")
	List<ChatMessageEntity> findByChatRoomOrderByCreateAtDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

	@Query("SELECT cm FROM ChatMessageEntity cm " +
		"WHERE cm.chatRoomId = :chatRoomId " +
		"AND cm.id < :lastMessageId " +
		"ORDER BY cm.createAt DESC")
	List<ChatMessageEntity> findByChatRoomWithPagination(@Param("chatRoomId") Long chatRoomId,
		@Param("lastMessageId") Long lastMessageId, Pageable pageable);

	@Query("SELECT cm FROM ChatMessageEntity cm " +
		"WHERE cm.chatRoomId = :chatRoomId " +
		"ORDER BY cm.createAt DESC " +
		"LIMIT 1")
	Optional<ChatMessageEntity> findLatestMessageByChatRoomId(@Param("chatRoomId") Long chatRoomId);

	@Query("SELECT COUNT(cm) FROM ChatMessageEntity cm " +
		"WHERE cm.chatRoomId = :chatRoomId " +
		"AND cm.senderId != :userId " +
		"AND cm.isRead = false")
	Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

	@Query("SELECT cm FROM ChatMessageEntity cm " +
		"WHERE cm.chatRoomId = :chatRoomId " +
		"AND cm.senderId != :userId " +
		"AND cm.isRead = false")
	List<ChatMessageEntity> findUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}