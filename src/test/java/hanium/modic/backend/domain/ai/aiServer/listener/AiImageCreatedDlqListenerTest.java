package hanium.modic.backend.domain.ai.aiServer.listener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageRepository;
import hanium.modic.backend.domain.ai.aiServer.dto.AiImageResponseMessageDto;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;
import hanium.modic.backend.domain.ai.aiServer.service.AiResponseSseService;
import hanium.modic.backend.domain.image.domain.ImageExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiImageCreatedDlqListener 단위 테스트")
class AiImageCreatedDlqListenerTest {

	@Mock
	private AiChatMessageRepository aiChatMessageRepository;

	@Mock
	private AiResponseSseService aiResponseSseService;

	@InjectMocks
	private AiImageCreatedDlqListener aiImageCreatedDlqListener;

	@Test
	@DisplayName("DLQ 메시지 처리 성공 - 메시지 상태를 RESPONSE_FAILED로 변경")
	void handleFinalFailedMessage_Success() {
		// Given
		String requestId = "test-request-id-123";

		AiImageResponseMessageDto messageDto = new AiImageResponseMessageDto(
			true,
			requestId,
			true,
			null,
			"/images/test.png",
			"test_20250112.png",
			"test",
			ImageExtension.PNG,
			"Test image",
			"Chat summary",
			false
		);

		AiChatMessageEntity requestChatMessage = AiChatMessageEntity.builder()
			.userId(1L)
			.postId(100L)
			.aiChatRoomId(10L)
			.messageOrder(1L)
			.senderType(SenderType.USER)
			.textContent("Generate an image")
			.requestId(requestId)
			.status(AiImageStatus.REQUEST_PENDING)
			.build();

		Message message = new Message(new byte[0], new MessageProperties());

		when(aiChatMessageRepository.findByRequestIdAndSenderType(requestId, SenderType.USER))
			.thenReturn(Optional.of(requestChatMessage));

		// When
		aiImageCreatedDlqListener.handleFinalFailedMessage(messageDto, message);

		// Then
		assertThat(requestChatMessage.getStatus()).isEqualTo(AiImageStatus.RESPONSE_FAILED);
		verify(aiChatMessageRepository).save(requestChatMessage);
		verify(aiResponseSseService).sendToClient(eq(requestId), any());
	}

	@Test
	@DisplayName("메시지를 찾을 수 없을 때 - 저장하지 않고 종료")
	void handleFinalFailedMessage_MessageNotFound_NoSave() {
		// Given
		String requestId = "not-found-request-id";

		AiImageResponseMessageDto messageDto = new AiImageResponseMessageDto(
			false,
			requestId,
			false,
			"Error occurred",
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);

		Message message = new Message(new byte[0], new MessageProperties());

		when(aiChatMessageRepository.findByRequestIdAndSenderType(requestId, SenderType.USER))
			.thenReturn(Optional.empty());

		// When
		aiImageCreatedDlqListener.handleFinalFailedMessage(messageDto, message);

		// Then
		verify(aiChatMessageRepository, never()).save(any());
		verify(aiResponseSseService, never()).sendToClient(anyString(), any());
	}

	@Test
	@DisplayName("SSE 전송 실패 시 - 예외를 삼키고 정상 종료")
	void handleFinalFailedMessage_SseFailure_ContinuesExecution() {
		// Given
		String requestId = "test-request-id-456";

		AiImageResponseMessageDto messageDto = new AiImageResponseMessageDto(
			true,
			requestId,
			true,
			null,
			"/images/test2.png",
			"test2_20250112.png",
			"test2",
			ImageExtension.PNG,
			"Test image 2",
			"Chat summary 2",
			false
		);

		AiChatMessageEntity requestChatMessage = AiChatMessageEntity.builder()
			.userId(2L)
			.postId(200L)
			.aiChatRoomId(20L)
			.messageOrder(1L)
			.senderType(SenderType.USER)
			.textContent("Generate another image")
			.requestId(requestId)
			.status(AiImageStatus.REQUEST_PENDING)
			.build();

		Message message = new Message(new byte[0], new MessageProperties());

		when(aiChatMessageRepository.findByRequestIdAndSenderType(requestId, SenderType.USER))
			.thenReturn(Optional.of(requestChatMessage));

		doThrow(new RuntimeException("SSE connection not found"))
			.when(aiResponseSseService).sendToClient(anyString(), any());

		// When & Then - 예외가 발생해도 메서드는 정상 종료되어야 함
		assertThatCode(() -> aiImageCreatedDlqListener.handleFinalFailedMessage(messageDto, message))
			.doesNotThrowAnyException();

		// 메시지 상태는 변경되어야 함
		assertThat(requestChatMessage.getStatus()).isEqualTo(AiImageStatus.RESPONSE_FAILED);
		verify(aiChatMessageRepository).save(requestChatMessage);
	}

	@Test
	@DisplayName("이미지 생성 실패 메시지 처리 - isImageGenerated=false")
	void handleFinalFailedMessage_ImageNotGenerated() {
		// Given
		String requestId = "test-request-id-789";

		AiImageResponseMessageDto messageDto = new AiImageResponseMessageDto(
			false,
			requestId,
			false,
			"AI server failed to generate image",
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);

		AiChatMessageEntity requestChatMessage = AiChatMessageEntity.builder()
			.userId(3L)
			.postId(300L)
			.aiChatRoomId(30L)
			.messageOrder(1L)
			.senderType(SenderType.USER)
			.textContent("Generate image request")
			.requestId(requestId)
			.status(AiImageStatus.REQUEST_PENDING)
			.build();

		Message message = new Message(new byte[0], new MessageProperties());

		when(aiChatMessageRepository.findByRequestIdAndSenderType(requestId, SenderType.USER))
			.thenReturn(Optional.of(requestChatMessage));

		// When
		aiImageCreatedDlqListener.handleFinalFailedMessage(messageDto, message);

		// Then
		assertThat(requestChatMessage.getStatus()).isEqualTo(AiImageStatus.RESPONSE_FAILED);
		verify(aiChatMessageRepository).save(requestChatMessage);
		verify(aiResponseSseService).sendToClient(eq(requestId), any());
	}
}
