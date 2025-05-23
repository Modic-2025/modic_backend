package hanium.modic.backend.domain.image.entityfactory;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;

public class ImageFactory {

	public static List<PostImageEntity> createMockPostImages(PostEntity postEntity, int count) {
		List<PostImageEntity> postImages = new ArrayList<>();
		for (int c = 1; c <= count; c++) {
			postImages.add(PostImageEntity.builder()
				.imagePath("imagePath" + c)
				.imageUrl("http://dqweq2ejh93-img" + c + ".jpg")
				.fullImageName("img" + c + ".jpg")
				.imageName("img" + c)
				.extension(ImageExtension.JPG)
				.imagePurpose(ImagePrefix.POST)
				.postEntity(postEntity)
				.build());
		}

		return postImages;
	}

	public static PostImageEntity createMockPostImage(PostEntity postEntity) {
		return PostImageEntity.builder()
			.imagePath("imagePath1")
			.imageUrl("http://dqweq2ejh93-img1.jpg")
			.fullImageName("img1.jpg")
			.imageName("img1")
			.extension(ImageExtension.JPG)
			.imagePurpose(ImagePrefix.POST)
			.postEntity(postEntity)
			.build();
	}

	public static PostImageEntity createMockPostImageWithId(PostEntity postEntity, Long postImageId) {
		PostImageEntity postImage = PostImageEntity.builder()
			.imagePath("imagePath1")
			.imageUrl("http://dqweq2ejh93-img1.jpg")
			.fullImageName("img1.jpg")
			.imageName("img1")
			.extension(ImageExtension.JPG)
			.imagePurpose(ImagePrefix.POST)
			.postEntity(postEntity)
			.build();

		PostImageEntity spyPostImage = Mockito.spy(postImage);
		when(spyPostImage.getId()).thenReturn(postImageId);

		return spyPostImage;
	}
}
