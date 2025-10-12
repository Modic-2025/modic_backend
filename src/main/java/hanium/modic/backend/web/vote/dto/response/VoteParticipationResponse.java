package hanium.modic.backend.web.vote.dto.response;

public record VoteParticipationResponse(
	Long voteId,
	boolean isCorrectAnswer,
	int currentStreak,
	boolean receivedTicket,
	float approveRate,
	float denyRate
) {
	public static VoteParticipationResponse of(
		Long voteId,
		boolean isCorrectAnswer,
		int currentStreak,
		boolean receivedTicket,
		float approveRate,
		float denyRate
	) {
		return new VoteParticipationResponse(voteId, isCorrectAnswer, currentStreak, receivedTicket, approveRate,
			denyRate);
	}
}