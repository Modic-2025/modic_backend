package hanium.modic.backend.domain.user.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;

@DataJpaTest
class UserEntityRepositoryTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	// Finds users whose names contain the given keyword ignoring case.
	@Test
	@DisplayName("이름이 포함된 회원을 페이지로 조회한다")
	void findByNameContainingIgnoreCase_returnsMatchingUsers() {
		UserEntity alice = userEntityRepository.save(UserFactory.createMockUserWithoutId("Alice"));
		UserEntity albert = userEntityRepository.save(UserFactory.createMockUserWithoutId("Albert"));
		userEntityRepository.save(UserFactory.createMockUserWithoutId("Bob"));

		PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));

		Page<UserEntity> result = userEntityRepository.findByNameContainingIgnoreCase("al", pageable);

		List<String> names = result.getContent().stream()
			.map(UserEntity::getName)
			.toList();

		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(names).containsExactly(albert.getName(), alice.getName());
	}

	// Returns an empty page when no users match the keyword.
	@Test
	@DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
	void findByNameContainingIgnoreCase_returnsEmptyPageWhenNoMatch() {
		userEntityRepository.save(UserFactory.createMockUserWithoutId("Charlie"));

		PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));

		Page<UserEntity> result = userEntityRepository.findByNameContainingIgnoreCase("zzz", pageable);

		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isZero();
	}
}
