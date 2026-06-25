package com.sanavi.backend.subscribe.controller;

import com.sanavi.backend.common.response.ApiResponse;
import com.sanavi.backend.subscribe.service.SubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Input:  userId (PathVariable) — JWT 적용 후 @AuthenticationPrincipal로 교체
// Output: ApiResponse
// 책임:   구독 상태 조회·활성화·취소
@RestController
@RequestMapping("/api/subscribe")
@RequiredArgsConstructor
public class SubscribeController {

    private final SubscribeService subscribeService;

    // 구독 상태 조회
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Integer>> getStatus(@PathVariable("userId") String userId) {
        int status = subscribeService.getSubscribeStatus(userId);
        return ResponseEntity.ok(ApiResponse.success("구독 상태 조회 성공", status));
    }

    // 구독 활성화 (Pro) — 고려사항: PG 연동 시 결제 완료 검증 후 호출되도록 변경 가능
    @PatchMapping("/{userId}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable("userId") String userId) {
        subscribeService.activate(userId);
        return ResponseEntity.ok(ApiResponse.success("구독이 활성화되었습니다.", null));
    }

    // 구독 취소 (Basic으로 다운그레이드)
    @PatchMapping("/{userId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable("userId") String userId) {
        subscribeService.cancel(userId);
        return ResponseEntity.ok(ApiResponse.success("구독이 취소되었습니다.", null));
    }
}
