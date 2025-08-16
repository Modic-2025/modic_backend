package hanium.modic.backend.domain.follow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.follow.dto.FollowerWithStatus;
import hanium.modic.backend.domain.follow.dto.FollowingWithStatus;
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

	@Modifying
	@Query(
		value = "INSERT IGNORE " +
			"INTO follows (my_id, following_id, create_at, update_at) " +
			"VALUES (:myId, :followingId, now(), now())",
		nativeQuery = true)
	void insertFollowIfExist(@Param("myId") Long myId, @Param("followingId") Long followingId);

	// 팔로워 목록 조회 (팔로우 상태 포함)
	@Query("""
		    SELECT u.id as id, u.name as name, u.email as email, u.userImageUrl as userImageUrl,
		           CASE WHEN f2.id IS NOT NULL THEN true ELSE false END as isFollowing
		    FROM FollowEntity f
		    JOIN UserEntity u ON f.myId = u.id
		    LEFT JOIN FollowEntity f2 ON f2.myId = :currentUserId AND f2.followingId = u.id
		    WHERE f.followingId = :targetUserId
		    ORDER BY f.createAt DESC
		""")
	Page<FollowerWithStatus> findFollowersWithStatusOrderByCreatedAt(
		@Param("targetUserId") Long targetUserId,
		@Param("currentUserId") Long currentUserId,
		Pageable pageable
	);

	// 팔로잉 목록 조회 (팔로우 상태 포함)
	@Query("""
		    SELECT u.id as id, u.name as name, u.email as email, u.userImageUrl as userImageUrl,
		           CASE WHEN f2.id IS NOT NULL THEN true ELSE false END as isFollowing
		    FROM FollowEntity f
		    JOIN UserEntity u ON f.followingId = u.id
		    LEFT JOIN FollowEntity f2 ON f2.myId = :currentUserId AND f2.followingId = u.id
		    WHERE f.myId = :targetUserId
		    ORDER BY f.createAt DESC
		""")
	Page<FollowingWithStatus> findFollowingsWithStatusOrderByCreatedAt(
		@Param("targetUserId") Long targetUserId,
		@Param("currentUserId") Long currentUserId,
		Pageable pageable
	);
}