package hanium.modic.backend.domain.auth.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailDto {

	private static final String SIGN_UP_CODE_SUBJECT = "MODIC 회원가입 인증 코드";
	private static final String SIGN_UP_CODE_CONTENT = "<h1>MODIC 회원가입 인증 코드</h1><p>인증 코드는 %s 입니다.</p>";

	private static final String RESET_PASSWORD_SUBJECT = "MODIC 임시 비밀번호";
	private static final String RESET_PASSWORD_CONTENT = "<h1>MODIC 임시 비밀번호 </h1><p>임시 비밀번호는 %s 입니다.</p>";

	private String receiverEmail;

	private String subject;

	private String content;

	public static EmailDto signup(final String receiverEmail, final String code) {
		return EmailDto.builder()
			.receiverEmail(receiverEmail)
			.subject(SIGN_UP_CODE_SUBJECT)
			.content(String.format(SIGN_UP_CODE_CONTENT, code))
			.build();
	}

	public static EmailDto resetPassword(final String receiverEmail, final String code) {
		return EmailDto.builder()
			.receiverEmail(receiverEmail)
			.subject(RESET_PASSWORD_SUBJECT)
			.content(String.format(RESET_PASSWORD_CONTENT, code))
			.build();
	}
}
