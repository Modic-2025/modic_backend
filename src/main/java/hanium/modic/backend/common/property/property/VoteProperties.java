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
}