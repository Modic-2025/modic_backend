package hanium.modic.backend.domain.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.jwt.BlackListRepository;
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.common.jwt.RefreshToken;
import hanium.modic.backend.common.jwt.RefreshTokenRepository;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.auth.service.component.CodeManager;
import hanium.modic.backend.domain.auth.service.component.EmailSender;
import hanium.modic.backend.domain.auth.service.dto.EmailDto;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.auth.dto.CheckEmailDuplicateResponse;
import hanium.modic.backend.web.auth.dto.LoginResponse;
import hanium.modic.backend.web.auth.dto.ReissueResponse;
import hanium.modic.backend.web.auth.dto.VerifyEmailCodeResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final BCryptPasswordEncoder passwordEncoder;

	private final UserEntityRepository userEntityRepository;

	private final JwtTokenProvider jwtTokenProvider;

	private final RefreshTokenRepository refreshTokenRepository;

	private final BlackListRepository blackListRepository;

	private final CodeManager codeManager;

	private final EmailSender emailSender;

	public LoginResponse login(final String email, final String password) {
		UserEntity user = userEntityRepository.findByEmail(email)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new AppException(ErrorCode.USER_PASSWORD_MISMATCH_EXCEPTION);
		}

		Token token = jwtTokenProvider.createToken(user);

		RefreshToken refreshToken = RefreshToken.builder()
			.userId(user.getId())
			.refreshToken(token.refreshToken())
			.build();
		refreshTokenRepository.save(refreshToken);

		return LoginResponse.from(token);
	}

	public ReissueResponse reissue(final String refreshToken) {
		if (blackListRepository.existsById(refreshToken)) {
			throw new AppException(ErrorCode.TOKEN_BLACKLISTED_EXCEPTION);
		}

		UserEntity user = jwtTokenProvider.getUser(refreshToken)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		RefreshToken savedRefreshToken = refreshTokenRepository.findById(user.getId())
			.orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND_EXCEPTION));

		if (!savedRefreshToken.getRefreshToken().equals(refreshToken)) {
			throw new AppException(ErrorCode.REFRESH_TOKEN_MISMATCH_EXCEPTION);
		}

		jwtTokenProvider.setBlackList(refreshToken);

		Token token = jwtTokenProvider.createToken(user);
		savedRefreshToken.updateRefreshToken(token.refreshToken());

		refreshTokenRepository.save(savedRefreshToken);

		return ReissueResponse.from(token);
	}

	public void sendEmailVerification(final String email) {
		if (userEntityRepository.existsByEmail(email)) {
			throw new AppException(ErrorCode.USER_EMAIL_DUPLICATED_EXCEPTION);
		}

		final String randomCode = codeManager.generateRandomCode(email);

		EmailDto emailDto = EmailDto.signup(email, randomCode);
		emailSender.sendEmail(emailDto);
	}

	public VerifyEmailCodeResponse verifyEmailCode(final String email, final String code) {
		checkEmailCode(email, code);

		return VerifyEmailCodeResponse.of(email, true);
	}

	private void checkEmailCode(final String email, final String code) {
		if (!codeManager.checkSignupCode(email, code)) {
			throw new AppException(ErrorCode.EMAIL_CODE_MISMATCH_EXCEPTION);
		}
	}

	public void checkEmailCodeAndDelete(final String email, final String code) {
		checkEmailCode(email, code);
		codeManager.deleteCode(email);
	}

	public CheckEmailDuplicateResponse checkEmailDuplicate(final String email) {
		boolean isAvailable = !userEntityRepository.existsByEmail(email);
		return CheckEmailDuplicateResponse.of(email, isAvailable);
	}
}
