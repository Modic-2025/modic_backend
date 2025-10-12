package hanium.modic.backend.domain.user.service;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.auth.service.AuthService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.entity.UserUpdateToken;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.repository.UserUpdateTokenRepository;
import hanium.modic.backend.web.user.dto.response.UserCreateResponse;
import hanium.modic.backend.web.user.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserEntityRepository userEntityRepository;

	private final BCryptPasswordEncoder passwordEncoder;
	private final UserUpdateTokenRepository userUpdateTokenRepository;
	private final AuthService authService;
	private final UserImageService userImageService;

	// 회원가입
	@Transactional
	public UserCreateResponse createUser(
		final String email,
		final String password,
		final String name,
		final String code
	) {
		checkDuplicateEmail(email);
		authService.checkEmailCodeAndDelete(email, code);

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
		Optional<String> userImageUrl = userImageService.createImageGetUrlOptional(user.getId());
		return UserInfoResponse.of(user, userImageUrl.orElse(null));
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

	// 유저 이메일 변경 (토큰 검증)
	@Transactional
	public void updateUserEmail(final long userId, final String newEmail, final String updateToken) {
		validateUpdateToken(userId, updateToken);
		checkDuplicateEmail(newEmail);

		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));
		user.updateEmail(newEmail);
	}

	// 유저 비밀번호 변경 (토큰 검증)
	@Transactional
	public void updateUserPasswordWithToken(
		final long userId,
		final String newPassword,
		final String updateToken
	) {
		validateUpdateToken(userId, updateToken);

		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		// 비밀번호 변경
		final String encodedNewPassword = passwordEncoder.encode(newPassword);
		user.updatePassword(encodedNewPassword);
	}

	// 유저 업데이트 토큰 발급
	public String getUserUpdateToken(final long userId, final String password) {
		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		// 기존 비밀번호 일치 확인
		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new AppException(ErrorCode.USER_PASSWORD_MISMATCH_EXCEPTION);
		}

		// 토큰을 조회하며, 기존에 있으면 TTL 초기화
		userUpdateTokenRepository.deleteById(userId); // TTL 초기화
		UserUpdateToken userUpdateToken = userUpdateTokenRepository.save(
			new UserUpdateToken(userId, generateUpdateToken()));

		return userUpdateToken.getUpdateToken();
	}

	// 토큰 검증 메서드
	private void validateUpdateToken(final long userId, final String updateToken) {
		UserUpdateToken userUpdateToken = userUpdateTokenRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_UPDATE_TOKEN_INVALID_EXCEPTION));

		if (!userUpdateToken.getUpdateToken().equals(updateToken)) {
			throw new AppException(ErrorCode.USER_UPDATE_TOKEN_INVALID_EXCEPTION);
		}
	}

	// 토큰 생성 메서드
	private String generateUpdateToken() {
		// 토큰 생성 로직 (예: UUID 사용)
		return java.util.UUID.randomUUID().toString();
	}
}
