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

    @Column(name = "is_ai_derived_post", nullable = false)
    private Boolean isAiDerivedPost;

    @Column(name = "parent_post_id")
    private Long parentPostId;

    @Column(name = "derived_post_status")
    @Enumerated(EnumType.STRING)
    private PostStatus derivedPostStatus;

    @Builder
    public PostEntity(
        Long userId,
        String title,
        String description,
        Long commercialPrice,
        Long nonCommercialPrice,
        Long ticketPrice,
        Boolean isAiDerivedPost,
        Long parentPostId,
        PostStatus derivedPostStatus
    ) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.commercialPrice = commercialPrice;
        this.nonCommercialPrice = nonCommercialPrice;
        this.ticketPrice = ticketPrice;
        this.isAiDerivedPost = isAiDerivedPost != null ? isAiDerivedPost : false;
        this.parentPostId = parentPostId;
        this.derivedPostStatus = derivedPostStatus;
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
    
    /**
     * AI 파생 게시물의 상태를 업데이트합니다.
     * 투표 시스템에서 투표 완료 시 호출됩니다.
     * 
     * @param status 새로운 상태 (PENDING, APPROVED, REJECTED)
     */
    public void updateDerivedPostStatus(PostStatus status) {
        this.derivedPostStatus = status;
    }
}
