package hanium.modic.backend.domain.post.entity;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.post.enums.PostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "post")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "description", columnDefinition = "TEXT", nullable = false)
	private String description;

	@Column(name = "commercial_price", nullable = false)
	private Long commercialPrice;

	@Column(name = "non_commercial_price", nullable = false)
	private Long nonCommercialPrice;

	@Column(name = "ticket_price", nullable = false)
	private Long ticketPrice;

	@Column(name = "parent_post_id")
	private Long parentPostId;

	@Column(name = "post_status", nullable = false)
	@Enumerated(EnumType.STRING)
	private PostStatus postStatus;

	@Column(name = "thumbnail_image_id", nullable = false)
	private Long thumbnailImageId;

	@Builder
	public PostEntity(
		Long userId,
		String title,
		String description,
		Long commercialPrice,
		Long nonCommercialPrice,
		Long ticketPrice,
		Long parentPostId,
		PostStatus postStatus,
		Long thumbnailImageId
	) {
		this.userId = userId;
		this.title = title;
		this.description = description;
		this.commercialPrice = commercialPrice;
		this.nonCommercialPrice = nonCommercialPrice;
		this.ticketPrice = ticketPrice;
		this.parentPostId = parentPostId;
		this.postStatus = postStatus;
		this.thumbnailImageId = thumbnailImageId;
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public void updateCommercialPrice(Long commercialPrice) {
		this.commercialPrice = commercialPrice;
	}

	public void updateNonCommercialPrice(Long nonCommercialPrice) {
		this.nonCommercialPrice = nonCommercialPrice;
	}

	public void updateTicketPrice(Long ticketPrice) {
		this.ticketPrice = ticketPrice;
	}

	public void updateDerivedPostStatus(PostStatus status) {
		this.postStatus = status;
	}

	public void updateThumbnailImageId(Long thumbnailImageId) {
		this.thumbnailImageId = thumbnailImageId;
	}
}
