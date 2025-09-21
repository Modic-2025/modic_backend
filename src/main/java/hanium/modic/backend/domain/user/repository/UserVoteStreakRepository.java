package hanium.modic.backend.domain.user.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import hanium.modic.backend.domain.user.entity.UserVoteStreak;

@Repository
public interface UserVoteStreakRepository extends CrudRepository<UserVoteStreak, Long> {
}