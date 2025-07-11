package hanium.modic.backend.domain.ai.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;

public interface AiRequestRepository extends JpaRepository<AiRequestEntity, Long> {
	/**
 * Retrieves an AI request entity by its unique request ID.
 *
 * @param requestId the unique identifier of the AI request
 * @return an {@code Optional} containing the found {@code AiRequestEntity}, or empty if not found
 */
Optional<AiRequestEntity> findByRequestId(String requestId);

	boolean existsByImagePath(String imagePath);

	/**
 * Checks if an AI request exists with the specified image ID and associated user ID.
 *
 * @param imageId the unique identifier of the AI request
 * @param userId the unique identifier of the user
 * @return true if an AI request with the given image ID and user ID exists, false otherwise
 */
boolean existsByIdAndUserId(Long imageId, Long userId);

	/**
 * Checks if an AI request exists with the specified request ID and user ID.
 *
 * @param requestId the unique identifier of the AI request
 * @param userId the identifier of the user associated with the request
 * @return true if an AI request with the given request ID and user ID exists, false otherwise
 */
boolean existsByRequestIdAndUserId(String requestId, Long userId);

	/**
		 * Retrieves a paginated list of AI request entities for a specific user and status, ordered by request ID in descending order.
		 *
		 * @param userId the ID of the user whose AI requests are to be retrieved
		 * @param status the status to filter AI requests by
		 * @param pageable pagination and sorting information
		 * @return a page of AI request entities matching the user ID and status, ordered by most recent request ID first
		 */
	Page<AiRequestEntity> findAllByUserIdAndStatusOrderByRequestIdDesc(Long userId, AiImageStatus status,
		Pageable pageable);
}