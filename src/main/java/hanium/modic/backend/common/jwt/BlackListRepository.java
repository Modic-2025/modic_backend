package hanium.modic.backend.common.jwt;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlackListRepository extends CrudRepository<BlackList, String> {
}
