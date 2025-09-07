package hanium.modic.backend.domain.vote.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.common.property.property.VoteProperties;
import hanium.modic.backend.common.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteResultEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteResultRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.web.vote.dto.response.VoteParticipationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 투표 참여, 투표 결과 집계 로직을 담당하는 서비스
 * 분산락을 통한 동시성 제어 구현
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VotingService {

	private final SimilarityVoteRepository similarityVoteRepository;
	private final SimilarityVoteResultRepository voteResultRepository;
	private final SimilarityVoteSummaryRepository voteSummaryRepository;
	private final LockManager lockManager;
	private final VoteProperties voteProperties;

	/**
	 * 투표 참여 메서드
	 * 
	 * @param voteId   투표 ID
	 * @param userId   사용자 ID
	 * @param decision 투표 결정 (APPROVE/DENY)
	 * @return VoteParticipationResponse
	 */
	public VoteParticipationResponse participateVote(Long voteId, Long userId, VoteDecision decision) {
		try {
			lockManager.voteSummaryLock(voteId, () -> {
				// 1. 투표 존재 및 상태 확인
				SimilarityVoteEntity vote = similarityVoteRepository.findById(voteId)
					.orElseThrow(() -> new AppException(VOTE_NOT_FOUND_EXCEPTION));

				// 2. 투표 상태 확인 (IN_PROGRESS만 허용)
				if (vote.getStatus() != VoteStatus.IN_PROGRESS) {
					throw new AppException(VOTE_NOT_IN_PROGRESS_EXCEPTION);
				}

				// 3. 중복 투표 방지
				if (voteResultRepository.existsByVoteIdAndUserId(voteId, userId)) {
					throw new AppException(DUPLICATE_VOTE_EXCEPTION);
				}

				// 4. 투표 권한 검증
				validateVotePermission(voteId, userId);

				// 5. 일일 투표 제한 체크
				validateDailyVoteLimit(userId);

				// 6. 투표 결과 저장
				SimilarityVoteResultEntity voteResult = SimilarityVoteResultEntity.builder()
					.voteId(voteId)
					.userId(userId)
					.decision(decision)
					.build();
				voteResultRepository.save(voteResult);

				log.info("투표 참여: voteId={}, userId={}, decision={}", voteId, userId, decision);

				// 7. 즉시 집계 업데이트
				updateVoteSummary(voteId, decision, voteProperties.getHumanVoteWeight().intValue());

				// 8. 투표 완료 확인 및 처리
				checkAndCompleteVote(voteId);
			});
			
			// 9. 단순한 응답 생성
			return VoteParticipationResponse.of(voteId);
		} catch (LockException e) {
			log.error("투표 참여 락 획득 실패: voteId={}, userId={}", voteId, userId, e);
			throw new AppException(VOTE_UPDATE_FAIL_EXCEPTION);
		}
	}

	/**
	 * 투표 집계 업데이트 메서드 (분산락 내부에서 실행)
	 * 
	 * @param voteId   투표 ID
	 * @param decision 투표 결정
	 * @param weight   투표 가중치
	 */
	private void updateVoteSummary(Long voteId, VoteDecision decision, int weight) {
		SimilarityVoteSummaryEntity summary = voteSummaryRepository.findByVoteId(voteId)
			.orElseThrow(() -> new AppException(VOTE_SUMMARY_NOT_FOUND_EXCEPTION));

		// 가중치에 따라 집계 업데이트
		if (decision == VoteDecision.APPROVE) {
			summary.addApproveWeight((long)weight);
		} else {
			summary.addDenyWeight((long)weight);
		}

		// 최종 결정 업데이트
		summary.updateFinalDecision();

		voteSummaryRepository.save(summary);
		log.debug("투표 집계 업데이트 완료: voteId={}, decision={}, weight={}", voteId, decision, weight);
	}

	/**
	 * 투표 완료 체크 및 상태 업데이트
	 * 
	 * @param voteId 투표 ID
	 * @return 투표 완료 여부
	 */
	private boolean checkAndCompleteVote(Long voteId) {
		boolean isCompleted = voteSummaryRepository.isVoteCompleted(voteId, voteProperties.getMinTotalWeight());
		
		if (isCompleted) {
			// 투표 상태를 COMPLETED로 변경
			SimilarityVoteEntity vote = similarityVoteRepository.findById(voteId)
				.orElseThrow(() -> new AppException(VOTE_NOT_FOUND_EXCEPTION));
			
			if (vote.getStatus() == VoteStatus.IN_PROGRESS) {
				vote.updateStatus(VoteStatus.COMPLETED);
				similarityVoteRepository.save(vote);
				log.info("투표 완료 처리: voteId={}", voteId);
			}
		}
		
		return isCompleted;
	}

	/**
	 * 투표 권한 검증 (타입 안전한 방식으로 개선)
	 * 파생 이미지 생성자와 원작 이미지 소유자는 투표 불가능
	 * 
	 * @param voteId 투표 ID
	 * @param userId 사용자 ID
	 */
	private void validateVotePermission(Long voteId, Long userId) {
		// 각각의 ID를 별도로 조회 (타입 안전성 보장)
		Long creatorId = similarityVoteRepository.findDerivedImageCreatorId(voteId)
			.orElseThrow(() -> {
				log.error("파생 이미지 생성자 ID 조회 실패: voteId={}", voteId);
				return new AppException(VOTE_PERMISSION_DENIED_EXCEPTION);
			});
			
		Long ownerId = similarityVoteRepository.findOriginalImageOwnerId(voteId)
			.orElseThrow(() -> {
				log.error("원작 이미지 소유자 ID 조회 실패: voteId={}", voteId);
				return new AppException(VOTE_PERMISSION_DENIED_EXCEPTION);
			});
		
		// 명확한 변수명으로 권한 검증
		if (isRestrictedUser(userId, creatorId, ownerId)) {
			log.warn("투표 권한 거부: voteId={}, userId={}, creatorId={}, ownerId={}", 
				voteId, userId, creatorId, ownerId);
			throw new AppException(VOTE_PERMISSION_DENIED_EXCEPTION);
		}
		
		log.debug("투표 권한 검증 통과: voteId={}, userId={}", voteId, userId);
	}

	/**
	 * 권한 검증 로직을 별도 메서드로 분리 (테스트 용이성)
	 * 
	 * @param userId 현재 사용자 ID
	 * @param creatorId 파생 이미지 생성자 ID
	 * @param ownerId 원작 이미지 소유자 ID
	 * @return 제한된 사용자 여부
	 */
	private boolean isRestrictedUser(Long userId, Long creatorId, Long ownerId) {
		return userId.equals(creatorId) || userId.equals(ownerId);
	}

	/**
	 * 일일 투표 제한 체크
	 * 
	 * @param userId 사용자 ID
	 */
	private void validateDailyVoteLimit(Long userId) {
		long todayVoteCount = voteResultRepository.countTodayVotesByUserId(userId);
		
		if (todayVoteCount >= voteProperties.getMaxVotesPerUserPerDay()) {
			log.warn("일일 투표 한도 초과: userId={}, todayCount={}, maxLimit={}", 
				userId, todayVoteCount, voteProperties.getMaxVotesPerUserPerDay());
			throw new AppException(VOTE_DAILY_LIMIT_EXCEEDED_EXCEPTION);
		}
	}
}