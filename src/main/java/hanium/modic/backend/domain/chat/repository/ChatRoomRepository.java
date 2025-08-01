package hanium.modic.backend.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import hanium.modic.backend.domain.chat.entity.ChatRoomEntity;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {


	@Query("SELECT cr FROM ChatRoomEntity cr " +
		"WHERE (cr.user1Id = :userId AND cr.user1Deleted = false) " +
		"OR (cr.user2Id = :userId AND cr.user2Deleted = false)")
	List<ChatRoomEntity> findActiveRoomsByUserId(@Param("userId") Long userId);

	@Query("SELECT cr FROM ChatRoomEntity cr " +
		"WHERE ((cr.user1Id = :user1Id AND cr.user2Id = :user2Id) " +
		"OR (cr.user1Id = :user2Id AND cr.user2Id = :user1Id))")
	Optional<ChatRoomEntity> findByUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}