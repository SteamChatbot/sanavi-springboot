package com.sanavi.backend.subscribe.controller;

import com.sanavi.backend.common.response.ApiResponse;
import com.sanavi.backend.subscribe.service.SubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscribe")
@RequiredArgsConstructor
public class SubscribeController {

    private final SubscribeService subscribeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Integer>> getStatus(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("구독 상태 조회 성공",
                subscribeService.getSubscribeStatus(userId)));
    }

    @PatchMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@AuthenticationPrincipal String userId) {
        subscribeService.activate(userId);
        return ResponseEntity.ok(ApiResponse.success("구독이 활성화되었습니다.", null));
    }

    @PatchMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@AuthenticationPrincipal String userId) {
        subscribeService.cancel(userId);
        return ResponseEntity.ok(ApiResponse.success("구독이 취소되었습니다.", null));
    }
}
