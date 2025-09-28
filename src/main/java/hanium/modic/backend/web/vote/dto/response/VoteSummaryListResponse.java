package hanium.modic.backend.web.vote.dto.response;

import java.util.List;

/**
 * 투표 요약 목록 응답 DTO
 * - 단일 항목 DTO인 VoteSummaryAiResponse의 리스트를 래핑합니다.
 */
public record VoteSummaryListResponse(
    List<VoteSummaryAiResponse> items
) {
    public static VoteSummaryListResponse from(List<VoteSummaryAiResponse> list) {
        return new VoteSummaryListResponse(list);
    }
}


