# Post Tree API 설계

## 개요
특정 포스트를 포함하여 하위 트리를 모두 조회하는 API입니다. 포스트 간의 부모-자식 관계를 통해 트리 구조를 파악할 수 있는 데이터를 제공하며, 프론트엔드에서 트리를 구성할 수 있도록 설계되었습니다.

## API 명세

### Endpoint
```
GET /api/v1/posts/{postId}/tree
```

### Path Parameters
- `postId` (Long): 트리의 루트가 될 포스트 ID

### Response
```json
{
  "data": [
    {
      "postId": 1,
      "title": "Original Post Title",
      "parentPostId": null,
      "representativeImageUrl": "https://example.com/image1.jpg",
      "postStatus": "COMPLETED"
    },
    {
      "postId": 2,
      "title": "Derived Post Title 1",
      "parentPostId": 1,
      "representativeImageUrl": "https://example.com/image2.jpg",
      "postStatus": "PROCESSING"
    },
    {
      "postId": 3,
      "title": "Derived Post Title 2",
      "parentPostId": 1,
      "representativeImageUrl": "https://example.com/image3.jpg",
      "postStatus": "COMPLETED"
    }
  ]
}
```

### Response 필드 설명
- `postId`: 포스트의 고유 ID
- `title`: 포스트 제목
- `parentPostId`: 부모 포스트 ID (루트 포스트의 경우 null)
- `representativeImageUrl`: 포스트의 대표 이미지 URL (첫 번째 이미지)
- `postStatus`: 포스트 상태 (PENDING, PROCESSING, COMPLETED, FAILED)

## 비즈니스 로직

### 트리 조회 방식
1. 요청된 `postId`를 루트로 하는 모든 하위 포스트를 재귀적으로 조회
2. 각 포스트의 첫 번째 이미지를 대표 이미지로 설정
3. 포스트 상태(PostStatus)와 함께 응답
4. 프론트엔드에서 `parentPostId`를 통해 트리 구조 생성

### 대표 이미지 선택 규칙
- 포스트에 연결된 이미지 중 첫 번째 이미지를 대표 이미지로 사용
- 이미지가 없는 경우 `representativeImageUrl`은 null로 응답
- 이미지 URL은 CloudFront 서명된 URL로 제공

### PostStatus 값
- `PENDING`: 대기 중
- `PROCESSING`: 처리 중
- `COMPLETED`: 완료
- `FAILED`: 실패

## 구현 계획

### 1. DTO 클래스
```java
public record GetPostTreeResponse(
    Long postId,
    String title,
    Long parentPostId,
    String representativeImageUrl,
    PostStatus postStatus
)
```

### 2. Repository 메서드
```java
// 특정 포스트의 모든 하위 포스트를 재귀적으로 조회
List<PostEntity> findAllDescendantsByPostId(Long postId);
```

### 3. Service 메서드
```java
public List<GetPostTreeResponse> getPostTree(Long postId);
```

### 4. Controller 메서드
```java
@GetMapping("/{postId}/tree")
public ResponseEntity<ApiResponse<List<GetPostTreeResponse>>> getPostTree(
    @PathVariable Long postId
);
```

## 성능 고려사항

### 데이터베이스 최적화
- 재귀 쿼리 또는 CTE(Common Table Expression) 사용
- 이미지 조회를 위한 배치 쿼리 적용
- 인덱스: `parent_post_id`에 인덱스 필요

### 캐싱 전략
- 자주 조회되는 트리 구조는 Redis 캐싱 고려
- 포스트 상태 변경 시 관련 캐시 무효화

## 에러 처리

### 가능한 에러 상황
- 존재하지 않는 포스트 ID 요청: `POST_NOT_FOUND_EXCEPTION`
- 권한이 없는 포스트 접근: `ACCESS_DENIED_EXCEPTION`
- 시스템 에러: `INTERNAL_SERVER_ERROR`

## 보안 고려사항
- 비공개 포스트에 대한 접근 권한 검증
- 이미지 URL의 서명된 URL 생성으로 보안 강화