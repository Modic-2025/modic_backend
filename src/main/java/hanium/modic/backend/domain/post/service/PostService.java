package hanium.modic.backend.domain.post.service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.EntityNotFoundException;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.post.dto.GetPostResponse;
import hanium.modic.backend.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostEntityRepository postEntityRepository;

    private final PostImageEntityRepository postImageEntityRepository;

    private static final String SORT_CRITERIA = "id";
    private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

    @Transactional
    public void createPost(final String title, final String description, final Long commercialPrice,
                           final Long nonCommercialPrice,
                           final List<String> imageUrls) {

        PostEntity postEntity = PostEntity.builder()
                .title(title)
                .description(description)
                .commercialPrice(commercialPrice)
                .nonCommercialPrice(nonCommercialPrice)
                .build();

        postEntityRepository.save(postEntity);

        List<PostImageEntity> postImages = imageUrls.stream()
                .map(url -> PostImageEntity.builder()
                        .postEntity(postEntity)
                        .imageUrl(url)
                        .build())
                .toList();
        postImageEntityRepository.saveAll(postImages);
    }

    public GetPostResponse getPost(final Long id) {
        PostEntity postEntity = postEntityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.POST_NOT_FOUND_EXCEPTION));

        List<String> imageUrls = postImageEntityRepository.findAllByPostId(id)
                .stream()
                .map(PostImageEntity::getImageUrl)
                .toList();

        return GetPostResponse.from(postEntity, imageUrls);
    }

    public PageResponse<GetPostResponse> getPosts(final String sort, final int page, final int size) {

        // Todo: sort 기능 추가

        Pageable pageable = PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA);
        Page<PostEntity> posts = postEntityRepository.findAll(pageable);

        if (posts.isEmpty()) {
            throw new EntityNotFoundException(ErrorCode.POST_NOT_FOUND_EXCEPTION);
        }

        Page<GetPostResponse> responsePages = posts.map(post -> {
            List<String> imageUrls = postImageEntityRepository.findAllByPostId(post.getId())
                    .stream()
                    .map(PostImageEntity::getImageUrl)
                    .toList();

            return GetPostResponse.from(post, imageUrls);
        });

        return PageResponse.of(responsePages);
    }
}