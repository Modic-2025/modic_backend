package hanium.modic.backend.domain.post.service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.EntityNotFoundException;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import java.util.List;

import hanium.modic.backend.web.dto.GetPostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostEntityRepository postEntityRepository;

    private final PostImageEntityRepository postImageEntityRepository;

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
}
