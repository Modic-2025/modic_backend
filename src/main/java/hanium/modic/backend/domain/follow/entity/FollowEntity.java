package hanium.modic.backend.domain.follow.entity;

import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

import hanium.modic.backend.domain.user.entity.UserEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Todo: 추후 GRAPHQL로 변경 예정
@Entity
@Table(name = "follows",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"follower_id", "following_id"})})
@Getter
@NoArgsConstructor(access = PROTECTED)
public class FollowEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	private Long followerId;

	private Long followingId;

	@Builder
	private FollowEntity(UserEntity follower, UserEntity following) {
		this.followerId = follower.getId();
		this.followingId = following.getId();
	}
}
