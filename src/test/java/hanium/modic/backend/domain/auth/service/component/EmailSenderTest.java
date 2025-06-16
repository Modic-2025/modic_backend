package hanium.modic.backend.domain.auth.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.EmailProperty;
import hanium.modic.backend.domain.auth.service.dto.EmailDto;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

	@InjectMocks
	private EmailSender emailSender;

	@Mock
	private JavaMailSender javaMailSender;

	@Mock
	private MimeMessage mimeMessage;

	@Mock
	private EmailProperty emailProperty;

	@Test
	@DisplayName("이메일 인증 코드 전송")
	void sendAuthEmailCode() throws Exception {
		// given
		final String receiver = "youth@youth.kr";
		final String code = "1234";
		final EmailDto emailDto = EmailDto.signup(receiver, code);

		given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
		given(emailProperty.getUsername()).willReturn("modic@gmail.com");

		// when
		emailSender.sendEmail(emailDto);

		// then
		verify(javaMailSender).createMimeMessage();
		verify(mimeMessage).addRecipients(Message.RecipientType.TO, receiver);
		verify(mimeMessage).setSubject(emailDto.getSubject());
		verify(mimeMessage).setText(emailDto.getContent(), "utf-8", "html");
		verify(mimeMessage).setFrom(any(InternetAddress.class));
		verify(javaMailSender).send(mimeMessage);
	}

	@Test
	@DisplayName("이메일 전송 실패 시 AppException 발생")
	void throwAppExceptionWhenSendEmailFails() throws Exception {
		// given
		EmailDto emailDto = EmailDto.signup("youth@youth.kr", "1234");

		given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
		doThrow(new MessagingException()).when(mimeMessage).setFrom(any(InternetAddress.class));

		// when & then
		assertThatThrownBy(() -> emailSender.sendEmail(emailDto))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.EMAIL_SEND_ERROR.getMessage());
	}
}