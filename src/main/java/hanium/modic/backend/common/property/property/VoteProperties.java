package hanium.modic.backend.common.property.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "vote.similarity")
public class VoteProperties {

	private Long minTotalWeight = 50L;
	private Long aiVoteWeight = 20L;
	private Long humanVoteWeight = 1L;
	private Boolean enableAiAssessment = true;
	private Long maxVotesPerUserPerDay = 100L;
}