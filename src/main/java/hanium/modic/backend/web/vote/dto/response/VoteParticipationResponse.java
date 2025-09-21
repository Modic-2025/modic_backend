package hanium.modic.backend.web.vote.dto.response;

public record VoteParticipationResponse(Long voteId) {
	
	public static VoteParticipationResponse of(Long voteId) {
		return new VoteParticipationResponse(voteId);
	}
}