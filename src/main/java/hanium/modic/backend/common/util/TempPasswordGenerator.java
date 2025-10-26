package hanium.modic.backend.common.util;

import java.security.SecureRandom;

public class TempPasswordGenerator {
	private static final String CHAR_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String CHAR_LOWER = CHAR_UPPER.toLowerCase();
	private static final String NUMBER = "0123456789";
	private static final String SPECIAL = "!@#$%^&*()-_=+";

	private static final String PASSWORD_ALLOW_BASE = CHAR_UPPER + CHAR_LOWER + NUMBER + SPECIAL;
	private static final SecureRandom random = new SecureRandom();

	public static String generateTempPassword(int length) {
		if (length < 8) throw new IllegalArgumentException("Password length must be at least 8");

		StringBuilder password = new StringBuilder(length);

		// 필수 구성요소를 각각 1개 이상 포함
		password.append(CHAR_UPPER.charAt(random.nextInt(CHAR_UPPER.length())));
		password.append(CHAR_LOWER.charAt(random.nextInt(CHAR_LOWER.length())));
		password.append(NUMBER.charAt(random.nextInt(NUMBER.length())));
		password.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

		// 나머지는 랜덤하게 채움
		for (int i = 4; i < length; i++) {
			password.append(PASSWORD_ALLOW_BASE.charAt(random.nextInt(PASSWORD_ALLOW_BASE.length())));
		}

		// 순서 섞기 (예측 불가능성 강화)
		return shuffleString(password.toString());
	}

	private static String shuffleString(String str) {
		char[] array = str.toCharArray();
		for (int i = array.length - 1; i > 0; i--) {
			int index = random.nextInt(i + 1);
			char temp = array[index];
			array[index] = array[i];
			array[i] = temp;
		}
		return new String(array);
	}
}
