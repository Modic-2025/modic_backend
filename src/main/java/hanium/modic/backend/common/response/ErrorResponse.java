package hanium.modic.backend.common.response;

import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

import hanium.modic.backend.common.error.ErrorCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse extends BaseResponse {
	private final String code;
	private final String message;
	private final List<String> reason;

	private ErrorResponse(HttpStatus status, String code, String message, List<String> reason) {
		super(false, status);
		this.code = code;
		this.message = message;
		this.reason = reason;
	}

	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse(
			errorCode.getStatus(),
			errorCode.getCode(),
			errorCode.getMessage(),
			null
		);
	}

	public static ErrorResponse of(ErrorCode errorCode, List<String> reason) {
		return new ErrorResponse(
			errorCode.getStatus(),
			errorCode.getCode(),
			errorCode.getMessage(),
			reason
		);
	}
}