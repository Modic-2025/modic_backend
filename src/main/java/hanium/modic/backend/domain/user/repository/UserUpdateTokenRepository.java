package hanium.modic.backend.domain.user.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import hanium.modic.backend.domain.user.entity.UserUpdateToken;

@Repository
public interface UserUpdateTokenRepository extends CrudRepository<UserUpdateToken, Long> {
}
