package hanium.modic.backend.domain.image.domain;

import static jakarta.persistence.EnumType.*;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Image {

	@Column(nullable = false)
	private String imagePath;

	@Column(nullable = false)
	private String imageUrl;

	@Column(nullable = false)
	private String fullImageName;

	@Column(nullable = false)
	private String imageName;

	@Enumerated(STRING)
	@Column(nullable = false)
	private ImageExtension extension;

	@Enumerated(STRING)
	@Column(nullable = false)
	private ImagePrefix imagePurpose;
}
