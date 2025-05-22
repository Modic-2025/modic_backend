package hanium.modic.backend.domain.post.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.EntityNotFoundException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostEntityRepository postEntityRepository;

	private final PostImageEntityRepository postImageEntityRepository;
	private final PostImageService postImageService;

	private static final String SORT_CRITERIA = "id";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	@Transactional
	public Long createPost(final String title, final String description, final Long commercialPrice,
		final Long nonCommercialPrice,
		final List<Long> imageIds) {

		PostEntity postEntity = PostEntity.builder()
			.title(title)
			.description(description)
			.commercialPrice(commercialPrice)
			.nonCommercialPrice(nonCommercialPrice)
			.build();

		PostEntity post = postEntityRepository.save(postEntity);

		List<PostImageEntity> list = imageIds.stream()
			.map(imageId -> postImageEntityRepository.findById(imageId)
				.orElseThrow(() -> new EntityNotFoundException(ErrorCode.IMAGE_NOT_FOUND_EXCEPTION)))
			.peek(postImageEntity -> postImageEntity.updatePost(postEntity))
			.toList();

		postImageEntityRepository.saveAll(list);

		return post.getId();
	}

	@Transactional(readOnly = true)
	public GetPostResponse getPost(final Long id) {
		PostEntity postEntity = postEntityRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException(ErrorCode.POST_NOT_FOUND_EXCEPTION));

		List<PostImageEntity> postImages = postImageEntityRepository.findAllByPostId(id);

		return GetPostResponse.from(postEntity, postImages);
	}

	@Transactional(readOnly = true)
	public PageResponse<GetPostResponse> getPosts(final String sort, final int page, final int size) {

		// Todo: sort 기능 추가

		Pageable pageable = PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA);
		Page<PostEntity> posts = postEntityRepository.findAll(pageable);

		if (posts.isEmpty()) {
			throw new EntityNotFoundException(ErrorCode.POST_NOT_FOUND_EXCEPTION);
		}

		Page<GetPostResponse> responsePages = posts.map(post -> {
			List<PostImageEntity> postImages = postImageEntityRepository.findAllByPostId(post.getId());

			return GetPostResponse.from(post, postImages);
		});

		return PageResponse.of(responsePages);
	}

	@Transactional
	public void deletePost(Long postId) {
		PostEntity post = postEntityRepository.findById(postId)
			.orElseThrow(() -> new EntityNotFoundException(ErrorCode.POST_NOT_FOUND_EXCEPTION));

		postImageEntityRepository.findAllByPostId(postId)
			.forEach(postImageEntity -> postImageService.deleteImage(postImageEntity.getId()));

		postEntityRepository.delete(post);
	}

	@Transactional
	public void updatePost(
		final Long id,
		final String title,
		final String description,
		final Long commercialPrice,
		final Long nonCommercialPrice,
		final List<Long> imageIds
	) {
		PostEntity post = postEntityRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException(ErrorCode.POST_NOT_FOUND_EXCEPTION));

		post.updateTitle(title);
		post.updateDescription(description);
		post.updateCommercialPrice(commercialPrice);
		post.updateNonCommercialPrice(nonCommercialPrice);
		postEntityRepository.save(post);

		List<PostImageEntity> postImages = postImageEntityRepository.findAllByPostId(id);

		// imageIds에 포함되지 않은 이미지 삭제
		List<PostImageEntity> deleteImages = postImages.stream()
			.filter(postImageEntity -> !imageIds.contains(postImageEntity.getId()))
			.toList();
		postImageService.deleteImages(deleteImages);

		// 새로 추가된 이미지에 PostId 업데이트
		postImageEntityRepository.findAllByIds(imageIds)
			.forEach(postImageEntity -> postImageEntity.updatePost(post));
	}
}