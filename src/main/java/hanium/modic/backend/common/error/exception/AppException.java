package hanium.modic.backend.common.error.exception;

import hanium.modic.backend.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

	private final ErrorCode errorCode;

	public AppException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}