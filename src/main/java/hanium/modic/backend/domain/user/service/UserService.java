package hanium.modic.backend.domain.user.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.common.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.user.dto.UserCreateResponse;
import hanium.modic.backend.web.user.dto.UserInfoResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserEntityRepository userEntityRepository;

	private final BCryptPasswordEncoder passwordEncoder;

	private final LockManager lockManager;

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

	// 코인 충전
	public void chargeCoin(final long userId, final long coin) {
		try {
			lockManager.userLock(userId, () -> {
				addCoin(userId, coin);
			});
		} catch (LockException e) {
			// Todo: 추후 결제 포함될 시, 결제 취소 로직 필요
			throw new AppException(USER_COIN_TRANSFER_FAIL_EXCEPTION);
		}
	}

	// 코인 양도
	// Todo: 양도자, 수신자 모두 락거는 기능 필요
	public void transferCoin(final long fromUserId, final long toUserId, long coin) throws AppException {
		if (fromUserId == toUserId) {
			throw new AppException(USER_COIN_TRANSFER_SAME_USER_EXCEPTION);
		}

		try {
			lockManager.multipleUserLock(List.of(fromUserId, toUserId), () -> {
				// 출금
				addCoin(fromUserId, -coin);
				// 입금
				addCoin(toUserId, coin);
			});
		} catch (LockException e) {
			throw new AppException(USER_COIN_TRANSFER_FAIL_EXCEPTION);
		}

	}

	// 코인 소비
	public void consumeCoin(final long userId, long coin) throws AppException {
		try {
			lockManager.userLock(userId, () -> {
				addCoin(userId, -coin);
			});
		} catch (LockException e) {
			throw new AppException(USER_COIN_TRANSFER_FAIL_EXCEPTION);
		}

	}

	// 코인 추가, 트랜잭션은 lockManager에 의해 관리됨
	private void addCoin(final long userId, final long coin) {
		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));

		user.addCoin(coin);
		userEntityRepository.save(user);
	}

}
