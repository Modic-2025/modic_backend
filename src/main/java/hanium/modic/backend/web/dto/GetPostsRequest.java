package hanium.modic.backend.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetPostsRequest(
        String sort,
        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,
        @Min(value = 10, message = "페이지 크기는 최소 10이어야 합니다.")
        @Max(value = 20, message = "페이지 크기는 최대 20이어야 합니다.")
        Integer size
) {
    public static GetPostsRequest of(String sort, Integer page, Integer size) {
        return new GetPostsRequest(sort, page, size);
    }

    public GetPostsRequest {
        // 기본값 설정
        page = (page == null) ? 0 : page;
        size = (size == null) ? 10 : size;
    }
}