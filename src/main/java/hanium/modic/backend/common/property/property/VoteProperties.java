package hanium.modic.backend.common.property.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "vote.similarity")
public class VoteProperties {

	private Long minTotalWeight;
	private Long aiVoteWeight;
	private Long humanVoteWeight;
	private Boolean enableAiAssessment;
	private Long maxVotesPerUserPerDay;
}