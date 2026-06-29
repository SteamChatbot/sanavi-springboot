package com.sanavi.backend.analysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanavi.backend.common.annotation.LogAction;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// React → backend-springboot → ai-api 프록시
// 책임: 구독 플랜별 ai_count 초과 검증 → ai-api 중계 → ai_count 증가
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final RestClient aiApiClient;
    private final MemberMapper memberMapper;
    private final ObjectMapper objectMapper;

    // userId는 토큰에서 추출 — null이면 비로그인 (횟수 제한 없음, DB 저장 안 됨)
    @LogAction(action = "분석 요청", domain = "AI",
            key = "#userId != null ? 'userId=' + #userId : 'guest'")
    @PostMapping
    public ResponseEntity<Object> requestAnalysis(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal String userId) {

        if (userId != null) {
            Member member = memberMapper.findByUserId(userId);
            if (member != null && member.getSubscribe() == 0 && member.getAiCount() >= 3) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "AI 분석 횟수(3회)를 초과했습니다. Pro 플랜으로 업그레이드하세요.");
            }
        }

        try {
            Object response = aiApiClient.post()
                    .uri("/api/analysis")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Id", userId != null ? userId : "")
                    .body(body)
                    .retrieve()
                    .body(Object.class);

            if (userId != null) memberMapper.incrementAiCount(userId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Object> getResult(@PathVariable String taskId) {
        try {
            return ResponseEntity.ok(aiApiClient.get()
                    .uri("/api/analysis/{taskId}", taskId)
                    .retrieve().body(Object.class));
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<Object> chat(@RequestBody Object body) {
        try {
            return ResponseEntity.ok(aiApiClient.post()
                    .uri("/api/analysis/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Object.class));
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Object> getHistory(@AuthenticationPrincipal String userId) {
        try {
            return ResponseEntity.ok(aiApiClient.get()
                    .uri("/api/analysis/history/{userId}", userId)
                    .retrieve().body(Object.class));
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }

    @LogAction(action = "분석 삭제", domain = "AI",
            key = "'taskId=' + #taskId + ' by=' + #userId")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Object> deleteAnalysis(
            @PathVariable String taskId,
            @AuthenticationPrincipal String userId) {
        try {
            aiApiClient.delete()
                    .uri(b -> b.path("/api/analysis/{taskId}")
                            .queryParam("userId", userId).build(taskId))
                    .retrieve().toBodilessEntity();
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }

    private String extractDetail(String body) {
        try {
            return objectMapper.readTree(body).path("detail").asText(
                    objectMapper.readTree(body).path("message").asText("요청 처리 실패"));
        } catch (Exception ignored) {
            return "요청 처리 실패";
        }
    }
}
