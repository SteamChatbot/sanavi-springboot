package com.sanavi.backend.analysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// React → backend-springboot → ai-api 프록시
// 책임: userId 검증·ai_count 체크 후 ai-api에 X-User-Id 헤더로 사용자 정보 전달
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final RestClient aiApiClient;
    private final MemberMapper memberMapper;
    private final ObjectMapper objectMapper;

    // Input:  분석 폼 데이터 + userId (React 로컬스토리지 user.userId)
    // Output: ai-api 202 응답 (task_id, status)
    // 책임:   구독 플랜별 ai_count 초과 검증 → ai-api 중계 → ai_count 증가
    @PostMapping
    public ResponseEntity<Object> requestAnalysis(@RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");

        if (userId != null && !userId.isBlank()) {
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

            if (userId != null && !userId.isBlank()) {
                memberMapper.incrementAiCount(userId);
            }

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }

    // Input:  taskId (UUID — analysis_result PK)
    // Output: AnalysisResponseDto (status, data) — ai-api 응답 그대로 중계
    // 책임:   ai-api에 분석 결과 폴링 위임
    @GetMapping("/{taskId}")
    public ResponseEntity<Object> getResult(@PathVariable String taskId) {
        try {
            Object response = aiApiClient.get()
                .uri("/api/analysis/{taskId}", taskId)
                .retrieve()
                .body(Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }

    // Input:  ChatRequestDto (context, history, question)
    // Output: ChatResponseDto (answer) — ai-api 응답 그대로 중계
    // 책임:   ai-api에 AI 추가 질의 위임
    @PostMapping("/chat")
    public ResponseEntity<Object> chat(@RequestBody Object body) {
        try {
            Object response = aiApiClient.post()
                .uri("/api/analysis/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }

    // Input:  userId (로그인 유저 ID — PathVariable)
    // Output: List<AnalysisHistoryItemDto> — ai-api 응답 그대로 중계
    // 책임:   ai-api에 이력 조회 위임
    @GetMapping("/history/{userId}")
    public ResponseEntity<Object> getHistory(@PathVariable String userId) {
        try {
            Object response = aiApiClient.get()
                .uri("/api/analysis/history/{userId}", userId)
                .retrieve()
                .body(Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }

    // Input:  taskId (PathVariable), userId (쿼리파라미터 — 소유권 검증)
    // Output: 204 No Content
    // 책임:   ai-api에 논리 삭제 위임
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Object> deleteAnalysis(
            @PathVariable String taskId,
            @RequestParam String userId) {
        try {
            aiApiClient.delete()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/analysis/{taskId}")
                    .queryParam("userId", userId)
                    .build(taskId))
                .retrieve()
                .toBodilessEntity();
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
