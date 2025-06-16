package hanium.modic.backend.domain.follow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.follow.entity.FollowEntity;
import hanium.modic.backend.domain.user.entity.UserEntity;

public interface FollowEntityRepository extends JpaRepository<FollowEntity, Long> {

	boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

	void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

	long countFollowers(Long userId);

	long countByFollowerId(Long followerId);

	@Query("SELECT f.followerId FROM FollowEntity f WHERE f.followingId = :userId")
	Page<UserEntity> findFollowers(@Param("userId") Long userId, Pageable pageable);

	@Query("SELECT f.followingId FROM FollowEntity f WHERE f.followerId = :userId")
	Page<UserEntity> findFollowing(@Param("userId") Long userId, Pageable pageable);
}