package hanium.modic.backend.domain.ai.aiServer.dto;

import java.util.List;

import hanium.modic.backend.domain.ai.aiServer.enums.ContentType;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;

/**
 * AI 서버로 전송하는 채팅 요청 DTO
 */
public record AiChatRequestDto(
	String requestId,           // 요청 ID (응답에 포함되어야 함)
	String prompt,              // 사용자가 작성한 채팅
	List<String> imagesPath,    // 사용자가 방금 채팅에 업로드한 이미지 경로들
	Long styleImageId,        // 포스트 이미지 ID
	String styleImagePath,      // 포스트 이미지 경로
	List<ChatMessage> chat,     // 채팅 내역
	String chatSummary          // 전체 채팅 요약
) {
	/**
	 * 채팅 메시지 내부 클래스
	 */
	public record ChatMessage(
		SenderType role,                    // "user" 또는 "assistant"
		List<ChatContent> contents      // 메시지 컨텐츠 리스트
	) {
	}

	/**
	 * 채팅 컨텐츠 내부 클래스
	 */
	public record ChatContent(
		ContentType type,       // "text" 또는 "image"
		String text,            // type이 "text"일 때 텍스트 내용
		String imagePath,       // type이 "image"일 때 이미지 경로
		String description,     // 이미지 설명
		Boolean fromOriginImage // 원본으로부터 파생되었는지
	) {
		/**
		 * 텍스트 컨텐츠 생성
		 */
		public static ChatContent text(String text) {
			return new ChatContent(ContentType.TEXT, text, null, null, null);
		}

		/**
		 * 이미지 컨텐츠 생성
		 */
		public static ChatContent image(String imagePath, String description, Boolean fromOriginImage) {
			return new ChatContent(ContentType.IMAGE, null, imagePath, description, fromOriginImage);
		}
	}
}