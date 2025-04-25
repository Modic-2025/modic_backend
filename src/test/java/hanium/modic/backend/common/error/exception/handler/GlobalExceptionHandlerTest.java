package hanium.modic.backend.common.error.exception.handler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

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
	
	@Test
	@DisplayName("MethodArgumentNotValidException이 발생하면 BAD_REQUEST와 적절한 에러 메시지를 반환한다")
	void handleMethodArgumentNotValidExceptionSuccess() {
		// given
		MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
		HttpHeaders headers = new HttpHeaders();
		HttpStatusCode status = HttpStatus.BAD_REQUEST;
		
		MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
		httpServletRequest.setRequestURI("/api/test");
		WebRequest webRequest = new ServletWebRequest(httpServletRequest);
		
		BindingResult bindingResult = mock(BindingResult.class);
		
		List<FieldError> fieldErrors = new ArrayList<>();
		FieldError fieldError1 = mock(FieldError.class);
		when(fieldError1.getDefaultMessage()).thenReturn("필드1 에러 메시지");
		FieldError fieldError2 = mock(FieldError.class);
		when(fieldError2.getDefaultMessage()).thenReturn("필드2 에러 메시지");
		fieldErrors.add(fieldError1);
		fieldErrors.add(fieldError2);
		
		when(ex.getBindingResult()).thenReturn(bindingResult);
		when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
		when(ex.getMessage()).thenReturn("유효성 검증 실패");
		
		// when
		ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, headers, status, webRequest);
		
		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
		
		ErrorResponse errorResponse = (ErrorResponse) response.getBody();
		assertThat(errorResponse).isNotNull();
		assertThat(errorResponse.getCode()).isEqualTo(ErrorCode.USER_INPUT_EXCEPTION.getCode());
		assertThat(errorResponse.getMessage()).isEqualTo(ErrorCode.USER_INPUT_EXCEPTION.getMessage());
		assertThat(errorResponse.getReason()).isNotNull();
		assertThat(errorResponse.getReason()).hasSize(2);
		assertThat(errorResponse.getReason()).contains("필드1 에러 메시지", "필드2 에러 메시지");
	}
	
	@Test
	@DisplayName("ConstraintViolationException이 발생하면 BAD_REQUEST와 적절한 에러 메시지를 반환한다")
	void handleConstraintViolationExceptionSuccess() {
		// given
		Set<ConstraintViolation<?>> violations = new HashSet<>();
		ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
		ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
		
		Path path1 = mock(Path.class);
		Path path2 = mock(Path.class);
		
		when(violation1.getMessage()).thenReturn("제약조건1 위반");
		when(violation1.getPropertyPath()).thenReturn(path1);
		when(path1.toString()).thenReturn("field1");
		
		when(violation2.getMessage()).thenReturn("제약조건2 위반");
		when(violation2.getPropertyPath()).thenReturn(path2);
		when(path2.toString()).thenReturn("field2");
		
		violations.add(violation1);
		violations.add(violation2);
		
		ConstraintViolationException ex = new ConstraintViolationException("제약조건 위반", violations);
		
		MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
		httpServletRequest.setRequestURI("/api/test");
		WebRequest webRequest = new ServletWebRequest(httpServletRequest);
		
		// when
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleConstraintViolation(ex, webRequest);
		
		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.USER_INPUT_EXCEPTION.getCode());
		assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.USER_INPUT_EXCEPTION.getMessage());
		assertThat(response.getBody().getReason()).isNotNull();
		assertThat(response.getBody().getReason()).hasSize(2);
		assertThat(response.getBody().getReason()).contains("field1: 제약조건1 위반", "field2: 제약조건2 위반");
	}
}