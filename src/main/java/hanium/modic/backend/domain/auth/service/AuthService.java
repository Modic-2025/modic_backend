package hanium.modic.backend.domain.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final BCryptPasswordEncoder passwordEncoder;

	private final UserEntityRepository userEntityRepository;

	private final JwtTokenProvider jwtTokenProvider;

	public LoginResponse login(final String email, final String password) {
		UserEntity user = userEntityRepository.findByEmail(email)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new AppException(ErrorCode.USER_PASSWORD_MISMATCH_EXCEPTION);
		}

		Token token = jwtTokenProvider.createToken(user);

		return LoginResponse.from(token);
	}
}
