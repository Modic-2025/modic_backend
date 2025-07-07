package hanium.modic.backend.domain.user.service;

import org.hibernate.validator.constraints.Length;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.user.dto.UserCreateResponse;
import hanium.modic.backend.web.user.dto.UserInfoResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserEntityRepository userEntityRepository;

	private final BCryptPasswordEncoder passwordEncoder;

	// 회원가입
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

	// 이메일 중복 검사
	private void checkDuplicateEmail(final String email) {
		if (userEntityRepository.existsByEmail(email)) {
			throw new AppException(ErrorCode.USER_EMAIL_DUPLICATED_EXCEPTION);
		}
	}

	// 회원 정보 조회
	public UserInfoResponse getUserInfo(UserEntity user) {
		return UserInfoResponse.from(user);
	}

	// 유저 이름 변경
	@Transactional
	public void updateUserName(
		final long id,
		final String name
	) {
		UserEntity user = userEntityRepository.findById(id)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));
		user.updateName(name);
	}

	// 유저 비밀번호 변경
	@Transactional
	public void updateUserPassword(
		final long id,
		final String oldPassword,
		final String newPassword
	) {
		UserEntity user = userEntityRepository.findById(id)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		// 기존 비밀번호 일치 확인
		if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
			throw new AppException(ErrorCode.USER_PASSWORD_MISMATCH_EXCEPTION);
		}

		// 비밀번호 변경
		final String encodedNewPassword = passwordEncoder.encode(newPassword);
		user.updatePassword(encodedNewPassword);
	}
}
