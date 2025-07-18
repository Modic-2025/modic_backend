package hanium.modic.backend.web.auth.dto;

/**
 * 이메일 중복 확인 API 응답 DTO
 * 
 * @param email 확인한 이메일 주소
 * @param available 사용 가능 여부 (true: 사용 가능, false: 중복됨)
 */
public record CheckEmailDuplicateResponse(
	String email,
	boolean available
) {
	/**
	 * CheckEmailDuplicateResponse 객체를 생성하는 정적 팩토리 메서드
	 * 
	 * @param email 확인한 이메일 주소
	 * @param available 사용 가능 여부
	 * @return CheckEmailDuplicateResponse 객체
	 */
	public static CheckEmailDuplicateResponse of(final String email, final boolean available) {
		return new CheckEmailDuplicateResponse(email, available);
	}
}