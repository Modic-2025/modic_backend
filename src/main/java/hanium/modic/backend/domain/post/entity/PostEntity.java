package hanium.modic.backend.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long commercialPrice;

    private Long nonCommercialPrice;

    @Builder
    public PostEntity(String title, String description, Long commercialPrice, Long nonCommercialPrice) {
        this.title = title;
        this.description = description;
        this.commercialPrice = commercialPrice;
        this.nonCommercialPrice = nonCommercialPrice;
    }
}
