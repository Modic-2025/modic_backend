package hanium.modic.backend.web.vote.dto.response;

public record VoteParticipationResponse(Long voteId, boolean isCorrectAnswer, int currentStreak,
										boolean receivedTicket) {

	public static VoteParticipationResponse of(Long voteId, boolean isCorrectAnswer, int currentStreak,
		boolean receivedTicket) {
		return new VoteParticipationResponse(voteId, isCorrectAnswer, currentStreak, receivedTicket);
	}
}