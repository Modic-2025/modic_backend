package hanium.modic.backend.domain.post.entityCreator;

import static org.mockito.Mockito.*;

import org.mockito.Mockito;

import hanium.modic.backend.domain.post.entity.PostEntity;

public class PostCreator {

	public static PostEntity createMockPost(Long id) {
		PostEntity post = PostEntity.builder()
			.title("테스트 게시글 " + id)
			.description("테스트 설명 " + id)
			.commercialPrice(10000L)
			.nonCommercialPrice(5000L)
			.build();

		PostEntity spyPost = Mockito.spy(post);
		when(spyPost.getId()).thenReturn(id);

		return spyPost;
	}
}
