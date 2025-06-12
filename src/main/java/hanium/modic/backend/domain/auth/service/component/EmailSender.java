package hanium.modic.backend.domain.auth.service.component;

import java.io.UnsupportedEncodingException;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.EmailProperty;
import hanium.modic.backend.domain.auth.service.dto.EmailDto;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

	private final EmailProperty emailProperty;

	private final JavaMailSender javaMailSender;

	public void sendEmail(final EmailDto emailDto) {
		try {
			MimeMessage message = javaMailSender.createMimeMessage();

			message.addRecipients(Message.RecipientType.TO, emailDto.getReceiverEmail());
			message.setSubject(emailDto.getSubject());
			message.setText(emailDto.getContent(), "utf-8", "html");
			message.setFrom(getInternetAddress());
			javaMailSender.send(message);
			log.info("이메일 전송 완료");
		} catch (MessagingException e) {
			throw new AppException(ErrorCode.EMAIL_SEND_ERROR);
		}
	}

	private InternetAddress getInternetAddress() {
		try {
			return new InternetAddress(emailProperty.getUsername(), EmailProperty.AUTH_PERSONAL);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
