package hanium.modic.backend.web.post.dto.response;

public record CreatePostResponse(
	Long postId
) {
	public static CreatePostResponse of(Long postId) {
		return new CreatePostResponse(postId);
	}
}
