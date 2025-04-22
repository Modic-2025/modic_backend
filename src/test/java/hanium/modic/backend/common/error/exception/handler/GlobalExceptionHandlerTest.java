package hanium.modic.backend.common.error.exception.handler;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.ErrorResponse;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	@InjectMocks
	private GlobalExceptionHandler globalExceptionHandler;

	@Test
	@DisplayName("AppException이 발생하면 ErrorResponse를 생성하여 반환한다")
	void handleAppExceptionSuccess() {
		// given
		ErrorCode errorCode = ErrorCode.USER_INPUT_EXCEPTION;
		AppException exception = new AppException(errorCode);

		// when
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAppException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(errorCode.getStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(errorCode.getCode());
		assertThat(response.getBody().getMessage()).isEqualTo(errorCode.getMessage());
		assertThat(response.getBody().getReason()).isNull();
	}

	@Test
	@DisplayName("처리되지 않은 예외가 발생하면 INTERNAL_SERVER_ERROR를 반환한다")
	void handleExceptionSuccess() {
		// given
		Exception exception = new Exception("처리되지 않은 예외");

		// when
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
		assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
		assertThat(response.getBody().getReason()).isNull();
	}
}