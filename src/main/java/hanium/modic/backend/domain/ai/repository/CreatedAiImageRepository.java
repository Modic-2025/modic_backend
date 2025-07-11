package hanium.modic.backend.domain.ai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;

public interface CreatedAiImageRepository extends JpaRepository<CreatedAiImageEntity, Long> {
	/**
 * Checks whether a created AI image entity exists with the specified image path.
 *
 * @param imagePath the path of the image to check for existence
 * @return true if an entity with the given image path exists, false otherwise
 */
boolean existsByImagePath(String imagePath);

	/**
 * Retrieves a CreatedAiImageEntity by its request ID.
 *
 * @param requestId the unique identifier of the image creation request
 * @return an Optional containing the matching CreatedAiImageEntity if found, or empty if not found
 */
Optional<CreatedAiImageEntity> findByRequestId(String requestId);

	/**
 * Retrieves all AI-created image entities whose request IDs are included in the specified list.
 *
 * @param requestIds a list of request IDs to match
 * @return a list of matching CreatedAiImageEntity objects
 */
List<CreatedAiImageEntity> findAllByRequestIdIn(List<String> requestIds);
}