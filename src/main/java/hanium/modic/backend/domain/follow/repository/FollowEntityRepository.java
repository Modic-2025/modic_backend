package hanium.modic.backend.domain.follow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.follow.entity.FollowEntity;
import hanium.modic.backend.domain.user.entity.UserEntity;

public interface FollowEntityRepository extends JpaRepository<FollowEntity, Long> {

	boolean existsByMyIdAndFollowingId(Long myId, Long followingId);

	void deleteByMyIdAndFollowingId(Long myId, Long followingId);

	long countByMyId(Long myId);

	long countByFollowingId(Long followerId);

	// 내 팔로워 조회
	@Query("""
		    SELECT u FROM FollowEntity f
		    JOIN UserEntity u ON f.myId = u.id
		    WHERE f.followingId = :followingId
		    ORDER BY f.createAt DESC
		""")
	Page<UserEntity> findFollowersOrderByCreatedAt(@Param("followingId") Long followingId, Pageable pageable);

	// 내 팔로잉 조회
	@Query("""
		    SELECT u FROM FollowEntity f
		    JOIN UserEntity u ON f.followingId = u.id
		    WHERE f.myId = :followingId
		    ORDER BY f.createAt DESC
		""")
	Page<UserEntity> findFollowingOrderByCreatedAt(@Param("followingId") Long followingId, Pageable pageable);
}