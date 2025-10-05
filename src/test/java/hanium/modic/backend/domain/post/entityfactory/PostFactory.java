package hanium.modic.backend.domain.post.entityfactory;

import static org.mockito.Mockito.*;

import org.mockito.Mockito;

import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.enums.PostStatus;
import hanium.modic.backend.domain.user.entity.UserEntity;

public class PostFactory {

	public static PostEntity createMockPostWithId(Long id, UserEntity user) {
		PostEntity post = PostEntity.builder()
			.userId(user.getId())
			.title("테스트 게시글 " + id)
			.description("테스트 설명 " + id)
			.commercialPrice(10000L)
			.nonCommercialPrice(5000L)
			.ticketPrice(3L)
			.thumbnailImageId(1L)
			.postStatus(PostStatus.ORIGINAL)
			.parentPostId(null)
			.build();

		PostEntity spyPost = Mockito.spy(post);
		when(spyPost.getId()).thenReturn(id);

		return spyPost;
	}

	public static PostEntity createMockPost(UserEntity user) {
		return PostEntity.builder()
			.userId(user.getId())
			.title("테스트 게시글")
			.description("테스트 설명")
			.commercialPrice(10000L)
			.nonCommercialPrice(5000L)
			.ticketPrice(3L)
			.thumbnailImageId(1L)
			.postStatus(PostStatus.ORIGINAL)
			.parentPostId(null)
			.build();
	}

	public static PostEntity createMockAiDerivedPostWithId(Long id, UserEntity user) {
		PostEntity post = PostEntity.builder()
			.userId(user.getId())
			.title("AI 파생 게시글 " + id)
			.description("AI로 생성된 게시글 설명 " + id)
			.commercialPrice(15000L)
			.nonCommercialPrice(8000L)
			.ticketPrice(5L)
			.thumbnailImageId(1L)
			.postStatus(PostStatus.ORIGINAL)
			.parentPostId(1L) // 기본적으로 포스트 ID 1을 부모로 설정
			.build();

		PostEntity spyPost = Mockito.spy(post);
		when(spyPost.getId()).thenReturn(id);

		return spyPost;
	}

	public static PostEntity createMockAiDerivedPostWithId(Long id, UserEntity user, Long parentPostId) {
		PostEntity post = PostEntity.builder()
			.userId(user.getId())
			.title("AI 파생 게시글 " + id)
			.description("AI로 생성된 게시글 설명 " + id)
			.commercialPrice(15000L)
			.nonCommercialPrice(8000L)
			.ticketPrice(5L)
			.thumbnailImageId(1L)
			.postStatus(PostStatus.ORIGINAL)
			.parentPostId(parentPostId)
			.build();

		PostEntity spyPost = Mockito.spy(post);
		when(spyPost.getId()).thenReturn(id);

		return spyPost;
	}

	public static PostEntity createMockAiDerivedPost(UserEntity user) {
		return PostEntity.builder()
			.userId(user.getId())
			.title("AI 파생 게시글")
			.description("AI로 생성된 게시글 설명")
			.commercialPrice(15000L)
			.nonCommercialPrice(8000L)
			.ticketPrice(5L)
			.thumbnailImageId(1L)
			.postStatus(PostStatus.ORIGINAL)
			.parentPostId(1L) // 기본적으로 포스트 ID 1을 부모로 설정
			.build();
	}
}
