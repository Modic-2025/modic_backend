package hanium.modic.backend.domain.vote.service;

public record VoteRewardResult(boolean isCorrectAnswer, int currentStreak, boolean receivedTicket) {
}
