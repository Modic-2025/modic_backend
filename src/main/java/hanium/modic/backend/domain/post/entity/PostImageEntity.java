package hanium.modic.backend.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "post_image")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Builder
    public PostImageEntity(String imageUrl, PostEntity postEntity) {
        this.imageUrl = imageUrl;
        this.postId = postEntity.getId();
    }
}
