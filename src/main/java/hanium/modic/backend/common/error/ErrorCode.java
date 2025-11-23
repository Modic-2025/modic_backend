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
	JWT_AUTH_EXCEPTION(HttpStatus.UNAUTHORIZED, "C-004", "JWT 인증 에러"),
	TOKEN_BLACKLISTED_EXCEPTION(HttpStatus.BAD_REQUEST, "C-005", "차단된 토큰입니다."),
	REFRESH_TOKEN_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "C-006", "해당 유저에게 발급된 리프레시 토큰이 존재하지 않습니다."),
	REFRESH_TOKEN_MISMATCH_EXCEPTION(HttpStatus.UNAUTHORIZED, "C-007", "리프레시 토큰이 일치하지 않습니다."),
	USER_NOT_AUTHENTICATED_EXCEPTION(HttpStatus.UNAUTHORIZED, "C-008", "사용자가 인증되지 않았습니다."),
	// ContextHolder에서 인증 주체 타입이 잘못된 경우
	INVALID_PRINCIPAL_TYPE_EXCEPTION(HttpStatus.UNAUTHORIZED, "C-009", "잘못된 인증 주체 타입입니다."),
	// 토큰에서 파싱한 유저 타입이 잘못된 경우
	INVALID_USER_TYPE_EXCEPTION(HttpStatus.UNAUTHORIZED, "C-010", "잘못된 사용자 타입입니다."),
	INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "C-011", "잘못된 토큰 타입입니다."),
	MALFORMED_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED, "C-012", "잘못된 형식의 토큰입니다."),

	// Auth
	EMAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A-001", "이메일 전송 중 에러가 발생하였습니다."),
	EMAIL_CODE_MISMATCH_EXCEPTION(HttpStatus.BAD_REQUEST, "A-002", "이메일 인증 코드가 일치하지 않습니다."),

	// User
	USER_EMAIL_DUPLICATED_EXCEPTION(HttpStatus.CONFLICT, "U-001", "이미 사용중인 이메일입니다."),
	USER_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "U-002", "해당 유저를 찾을 수 없습니다."),
	USER_PASSWORD_MISMATCH_EXCEPTION(HttpStatus.UNAUTHORIZED, "U-003", "비밀번호가 일치하지 않습니다."),
	USER_IMAGE_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "U-007", "해당 유저의 프로필 이미지를 찾을 수 없습니다."),
	USER_UPDATE_TOKEN_INVALID_EXCEPTION(HttpStatus.BAD_REQUEST, "U-008", "토큰이 유효하지 않습니다."),
	WITHDRAWN_USER_EXCEPTION(HttpStatus.BAD_REQUEST, "U-009", "탈퇴한 유저입니다."),

	// Post
	POST_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "P-001", "해당 포스트를 찾을 수 없습니다."),
	POST_ROLE_EXCEPTION(HttpStatus.FORBIDDEN, "P-002", "포스트에 대한 권한이 없습니다."),
	POST_ACCESS_DENIED_EXCEPTION(HttpStatus.FORBIDDEN, "P-003", "포스트에 대한 접근 권한이 없습니다."),
	NOT_AI_DERIVED_POST_EXCEPTION(HttpStatus.BAD_REQUEST, "P-004", "AI 파생 포스트가 아닙니다."),
	THUMBNAIL_IMAGE_NOT_IN_IMAGE_LIST_EXCEPTION(HttpStatus.BAD_REQUEST, "P-005", "썸네일 이미지는 이미지 목록에 포함되어야 합니다."),

	// Derived Post
	DUPLICATE_DERIVED_POST_EXCEPTION(HttpStatus.CONFLICT, "DP-001", "이미 해당 이미지로 파생 포스트가 생성되었습니다."),

	// Post Like
	CANNOT_LIKE_OWN_POST_EXCEPTION(HttpStatus.BAD_REQUEST, "PL-001", "자신의 게시글에는 하트를 할 수 없습니다."),
	POST_LIKE_FAIL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "PL-002", "좋아요 처리에 실패하였습니다."),

	// Post Review
	POST_REVIEW_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "PR-001", "해당 포스트 리뷰를 찾을 수 없습니다."),
	CANNOT_REVIEW_OWN_POST_EXCEPTION(HttpStatus.BAD_REQUEST, "PR-002", "자신의 게시물에는 리뷰를 작성할 수 없습니다."),
	POST_REVIEW_PERMISSION_DENIED_EXCEPTION(HttpStatus.FORBIDDEN, "PR-003", "해당 그림체를 사용한 이력이 없어 리뷰를 작성할 수 없습니다."),

	// Post Review Comment
	POST_REVIEW_COMMENT_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "PRC-001", "해당 포스트 리뷰 댓글을 찾을 수 없습니다."),

	// Image
	IMAGE_NOT_STORE_EXCEPTION(HttpStatus.BAD_REQUEST, "I-001", "이미지가 저장되지 않았습니다."),
	IMAGE_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "I-002", "해당 이미지를 찾을 수 없습니다."),
	INVALID_IMAGE_FILE_NAME_EXCEPTION(HttpStatus.BAD_REQUEST, "I-003", "잘못된 이미지 파일 이름입니다."),
	INVALID_IMAGE_FILE_PATH_EXCEPTION(HttpStatus.BAD_REQUEST, "I-004", "잘못된 이미지 파일 경로입니다."),
	IMAGE_PATH_DUPLICATED_EXCEPTION(HttpStatus.CONFLICT, "I-005", "이미지 경로가 중복되었습니다."),
	IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION(HttpStatus.BAD_REQUEST, "I-006", "이미지를 훔칠 수 없습니다."),

	// Follow
	CANNOT_FOLLOW_SELF_EXCEPTION(HttpStatus.BAD_REQUEST, "F-001", "자기 자신을 팔로우할 수 없습니다."),
	FOLLOW_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "F-002", "해당 팔로우를 찾을 수 없습니다."),

	// Server
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S-001", "서버 내부에서 에러가 발생하였습니다."),
	S3_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S-002", "S3 서버에서 에러가 발생하였습니다."),

	// AI
	AI_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "AI-001", "해당 AI 요청을 찾을 수 없습니다."),
	CREATED_AI_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "AI-002", "생성된 AI 이미지를 찾을 수 없습니다."),
	AI_IMAGE_DATA_INCONSISTENCY(HttpStatus.INTERNAL_SERVER_ERROR, "A-003", "AI 이미지 데이터 일관성 오류가 발생했습니다."),
	AI_IMAGE_PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "AI-004", "AI 이미지 생성권을 구매한 이력이 없습니다."),
	AI_REQUEST_TICKET_PROCESSING_FAIL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "AI-005", "티켓 처리에 실패했습니다."),
	AI_REQUEST_TICKET_NOT_ENOUGH_EXCEPTION(HttpStatus.BAD_REQUEST, "AI-006", "티켓이 부족합니다."),
	REMAINING_GENERATIONS_NOT_ENOUGH_EXCEPTION(HttpStatus.BAD_REQUEST, "AI-007", "AI 이미지 생성권이 부족합니다."),
	AI_IMAGE_PERMISSION_FAIL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "AI-008", "이미지 생성권 처리에 실패하였습니다."),
	AI_IMAGE_PERMISSION_ALREADY_EXISTS_EXCEPTION(HttpStatus.CONFLICT, "AI-009", "이미 AI 이미지 생성권을 구매했습니다."),
	AI_IMAGE_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "AI-010", "생성된 AI 이미지를 찾을 수 없습니다."),
	AI_IMAGE_ACCESS_DENIED_EXCEPTION(HttpStatus.FORBIDDEN, "AI-011", "AI 이미지에 대한 권한이 없습니다."),
	AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI-012", "AI 서버와의 통신 중 에러가 발생하였습니다."),
	CANT_REGISTER_AI_IMAGE_EXCEPTION(HttpStatus.BAD_REQUEST, "AI-013", "원작자 이미지와 무관한 이미지는 파생 포스트로 등록할 수 없습니다."),
	AI_IMAGE_NOT_FROM_ORIGIN_EXCEPTION(HttpStatus.BAD_REQUEST, "AI-014", "원작자로부터 파생된 이미지가 아닙니다."),

	// AI Chat
	AI_CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "AC-001", "AI 채팅방을 찾을 수 없습니다."),
	EMPTY_CHAT_MESSAGE_EXCEPTION(HttpStatus.BAD_REQUEST, "AC-002", "채팅 메시지랑 이미지가 모두 비어있습니다."),
	AI_CHAT_MESSAGE_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "AC-003", "AI 채팅 메시지 순서 정보를 찾을 수 없습니다."),
	AI_CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "AC-004", "AI 채팅 메시지를 찾을 수 없습니다."),
	AI_CHAT_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "AC-005", "요청이 아닌 메시지는 취소할 수 없습니다."),

	// Vote
	VOTE_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "V-001", "해당 투표를 찾을 수 없습니다."),
	DUPLICATE_VOTE_EXCEPTION(HttpStatus.BAD_REQUEST, "V-002", "이미 투표에 참여했습니다."),
	VOTE_ALREADY_COMPLETED_EXCEPTION(HttpStatus.BAD_REQUEST, "V-003", "이미 완료된 투표입니다."),
	VOTE_PERMISSION_DENIED_EXCEPTION(HttpStatus.FORBIDDEN, "V-004", "투표 권한이 없습니다."),
	VOTE_NOT_IN_PROGRESS_EXCEPTION(HttpStatus.BAD_REQUEST, "V-005", "진행 중인 투표가 아닙니다."),
	VOTE_DAILY_LIMIT_EXCEEDED_EXCEPTION(HttpStatus.BAD_REQUEST, "V-006", "일일 투표 한도를 초과했습니다."),
	VOTE_SUMMARY_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "V-007", "투표 집계 정보를 찾을 수 없습니다."),
	VOTE_UPDATE_FAIL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "V-008", "투표 집계 업데이트에 실패했습니다."),
	NO_AVAILABLE_VOTES_EXCEPTION(HttpStatus.NOT_FOUND, "V-009", "참여 가능한 투표가 없습니다."),

	// Account
	ACCOUNT_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "ACC-001", "해당 계좌을 찾을 수 없습니다."),
	COIN_NOT_ENOUGH_EXCEPTION(HttpStatus.BAD_REQUEST, "ACC-002", "코인이 부족합니다."),
	COIN_TRANSFER_SAME_USER_EXCEPTION(HttpStatus.BAD_REQUEST, "ACC-003", "자신에게 코인을 송금할 수 업습니다."),
	COIN_TRANSFER_FAIL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "ACC-004", "코인 송금에 실패하였습니다."),
	EFFECTIVE_AT_CANT_NOT_BE_NULL(HttpStatus.BAD_REQUEST, "ACC-005", "유효 시간이 null일 수 없습니다.")
	;

	private final HttpStatus status;
	private final String code;
	private final String message;

}