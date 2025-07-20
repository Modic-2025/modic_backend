package hanium.modic.backend.domain.auth.service.component;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import hanium.modic.backend.domain.auth.repository.AuthCodeRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CodeManager {

	private static final int CODE_LENGTH = 4;

	private static final int CODE_BOUNDARY = 10;

	private final AuthCodeRepository authCodeRepository;

	public String generateRandomCode(final String email) {
		final String verificationCode = getRandomCode();
		authCodeRepository.saveCode(email, verificationCode);
		return verificationCode;
	}

	private String getRandomCode() {
		final ThreadLocalRandom random = ThreadLocalRandom.current();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < CODE_LENGTH; i++) {
			builder.append(random.nextInt(CODE_BOUNDARY));
		}
		return String.valueOf(builder);
	}

	public Boolean checkSignupCode(final String email, final String code) {
		String savedVerificationCode = authCodeRepository.getCode(email);
		return code.equals(savedVerificationCode);
	}

	public void deleteCode(final String email) {
		authCodeRepository.deleteCode(email);
	}
}
