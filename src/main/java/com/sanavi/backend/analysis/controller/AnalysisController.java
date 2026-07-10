package com.sanavi.backend.analysis.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.sanavi.backend.analysis.service.AnalysisService;
import com.sanavi.backend.common.annotation.LogAction;

import lombok.RequiredArgsConstructor;

// React → backend-springboot → ai-api 프록시
// 책임: HTTP 계층만 담당 — 구독 검증/프록시 호출/ai_count 증가 등 실제 로직은 AnalysisService
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @LogAction(action = "분석 요청", domain = "AI", key = "#userId != null ? 'userId=' + #userId : 'guest'")
    @PostMapping
    public ResponseEntity<Object> requestAnalysis(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(analysisService.requestAnalysis(body, userId));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Object> getResult(@PathVariable String taskId) {
        return ResponseEntity.ok(analysisService.getResult(taskId));
    }

    @PostMapping("/chat")
    public ResponseEntity<Object> chat(@RequestBody Object body) {
        return ResponseEntity.ok(analysisService.chat(body));
    }

    @GetMapping("/history")
    public ResponseEntity<Object> getHistory(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(analysisService.getHistory(userId));
    }

    @LogAction(action = "분석 삭제", domain = "AI", key = "'taskId=' + #taskId + ' by=' + #userId")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Object> deleteAnalysis(
            @PathVariable String taskId,
            @AuthenticationPrincipal String userId) {
        analysisService.deleteAnalysis(taskId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{taskId}/checklist")
    public ResponseEntity<Void> saveChecklistChecks(
            @PathVariable String taskId,
            @RequestBody Object body,
            @AuthenticationPrincipal String userId) {
        analysisService.saveChecklistChecks(taskId, body, userId);
        return ResponseEntity.noContent().build();
    }
}
