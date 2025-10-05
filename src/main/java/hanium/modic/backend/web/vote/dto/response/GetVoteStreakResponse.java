package hanium.modic.backend.web.vote.dto.response;

/**
 * 사용자 투표 연속 정답 정보를 제공하는 응답 DTO.
 */
public record GetVoteStreakResponse(
	int currentStreak,
	int rewardThreshold
) {
	/**
	 * 현재 연속 정답 수와 리워드 임계치를 기반으로 응답 객체를 생성합니다.
	 *
	 * @param currentStreak 사용자 연속 정답 수
	 * @param rewardThreshold 리워드 지급 임계치
	 * @return 연속 정답 정보를 담은 응답
	 */
	public static GetVoteStreakResponse of(int currentStreak, int rewardThreshold) {
		return new GetVoteStreakResponse(currentStreak, rewardThreshold);
	}
}
