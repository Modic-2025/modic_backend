package hanium.modic.backend.domain.postReview.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import hanium.modic.backend.domain.postReview.entity.PostReviewImageEntity;

public interface PostReviewImageRepository extends JpaRepository<PostReviewImageEntity, Long> {

	boolean existsByImagePath(String imagePath);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM PostReviewImageEntity p WHERE p.id IN :ids")
	void deleteAllByIds(List<Long> ids);

	List<PostReviewImageEntity> findAllByPostReviewId(Long postReviewId);

	@Query("SELECT p FROM PostReviewImageEntity p WHERE p.id in :postReviewImageIds")
	List<PostReviewImageEntity> findAllByPostReviewImageIds(List<Long> postReviewImageIds);

	List<PostReviewImageEntity> findAllByPostReviewIdIn(List<Long> postReviewIds);
}