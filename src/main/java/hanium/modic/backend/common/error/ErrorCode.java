package hanium.modic.backend.common.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// Common
	USER_INPUT_EXCEPTION(HttpStatus.BAD_REQUEST, "C-001", "사용자 입력 오류"),
	USER_ROLE_EXCEPTION(HttpStatus.FORBIDDEN, "C-002", "유저 권한 오류"),
	AUTHENTICATION_EXCEPTION(HttpStatus.UNAUTHORIZED, "C-003", "공통 권한 에러(필터)"),

	// User
	USER_EMAIL_DUPLICATED_EXCEPTION(HttpStatus.CONFLICT, "U-001", "이미 사용중인 이메일입니다."),

	// Post
	POST_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "P-001", "해당 포스트를 찾을 수 없습니다."),

	// Image
	IMAGE_NOT_STORE_EXCEPTION(HttpStatus.BAD_REQUEST, "I-001", "이미지가 저장되지 않았습니다."),
	IMAGE_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "I-002", "해당 이미지를 찾을 수 없습니다."),
	INVALID_IMAGE_FILE_NAME_EXCEPTION(HttpStatus.BAD_REQUEST, "I-003", "잘못된 이미지 파일 이름입니다."),
	INVALID_IMAGE_FILE_PATH_EXCEPTION(HttpStatus.BAD_REQUEST, "I-004", "잘못된 이미지 파일 경로입니다."),
	IMAGE_PATH_DUPLICATED_EXCEPTION(HttpStatus.CONFLICT, "I-005", "이미지 경로가 중복되었습니다."),
	IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION(HttpStatus.BAD_REQUEST, "I-006", "이미지를 훔칠 수 없습니다."),

	// Server
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S-001", "서버 내부에서 에러가 발생하였습니다."),
	S3_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S-002", "S3 서버에서 에러가 발생하였습니다."),
	;

	private final HttpStatus status;
	private final String code;
	private final String message;
}