package hanium.modic.backend.common.error.exception;

import hanium.modic.backend.common.error.ErrorCode;

public class EntityNotFoundException extends AppException {

	public EntityNotFoundException(ErrorCode errorCode) {
		super(errorCode);
	}
}