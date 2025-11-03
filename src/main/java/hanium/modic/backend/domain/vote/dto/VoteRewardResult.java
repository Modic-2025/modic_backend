package hanium.modic.backend.domain.vote.dto;

public record VoteRewardResult(boolean isCorrectAnswer, int currentStreak, boolean receivedTicket) {
}
