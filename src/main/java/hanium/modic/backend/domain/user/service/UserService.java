package hanium.modic.backend.domain.user.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.user.dto.UserCreateResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserEntityRepository userEntityRepository;

	private final BCryptPasswordEncoder passwordEncoder;

	@Transactional
	public UserCreateResponse createUser(final String email, final String password, final String name) {
		checkDuplicateEmail(email);

		final String encodedPassword = passwordEncoder.encode(password);

		final UserEntity user = UserEntity.builder()
			.email(email)
			.password(encodedPassword)
			.name(name)
			.build();
		userEntityRepository.save(user);

		return UserCreateResponse.from(user);
	}

	private void checkDuplicateEmail(final String email) {
		if (userEntityRepository.existsByEmail(email)) {
			throw new AppException(ErrorCode.USER_EMAIL_DUPLICATED_EXCEPTION);
		}
	}
}
