package hanium.modic.backend.common.property.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "vote.similarity")
public class VoteProperties {

	private int minTotalWeight;
	private int aiVoteWeight;
	private int humanVoteWeight;
	private Boolean enableAiAssessment;
	private int maxVotesPerUserPerDay;

	/**
	 * 연속 정답 리워드 지급 기준 횟수 (기본값 3)
	 */
	private int streakRewardCount = 3;

	/**
	 * 연속 정답 카운트 TTL (일, 기본값 30)
	 */
	private int streakTtlDays = 30;

	/**
	 * 리워드 지급 시 증가하는 티켓 수 (기본값 1)
	 */
	private int rewardTicketCount = 1;
}