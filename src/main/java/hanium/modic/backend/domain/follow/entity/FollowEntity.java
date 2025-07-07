package hanium.modic.backend.domain.follow.entity;

import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Todo: 추후 GRAPHDB로 변경 예정
@Entity
@Table(name = "follows",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"my_id", "following_id"})})
@Getter
@NoArgsConstructor(access = PROTECTED)
public class FollowEntity extends BaseEntity {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(name = "my_id")
	private Long myId;

	@Column(name = "following_id")
	private Long followingId;

	@Builder
	private FollowEntity(UserEntity me, UserEntity following) {
		this.myId = me.getId();
		this.followingId = following.getId();
	}
}
