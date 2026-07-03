package com.sanavi.backend.analysis.controller;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanavi.backend.common.annotation.LogAction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

// 관리자 전용 — ai-api의 /api/admin/analysis 프록시 (전체 회원 분석이력 조회/통계/강제삭제)
// role_admin 권한 검사는 SecurityConfig에서 처리해야 함 — 현재 SecurityConfig가 anyRequest().permitAll()
// 상태라 활성화 전까지는 이 엔드포인트가 실제로 보호되지 않음 (CLAUDE.md "알려진 이슈" 참고)
@RestController
@RequestMapping("/api/admin/analysis")
@RequiredArgsConstructor
public class AdminAnalysisController {

    private final RestClient aiApiClient;
    private final ObjectMapper objectMapper;

    @LogAction(action = "관리자 분석이력 조회", domain = "AI", key = "'page=' + #page + ' keyword=' + #keyword")
    @GetMapping
    public ResponseEntity<Object> getAdminAnalysisList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "scoreFilter", required = false) String scoreFilter) {
        try {
            Object response = aiApiClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/admin/analysis")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .queryParamIfPresent("keyword", Optional.ofNullable(keyword))
                            .queryParamIfPresent("scoreFilter", Optional.ofNullable(scoreFilter))
                            .build())
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

    @GetMapping("/stats")
    public ResponseEntity<Object> getAdminAnalysisStats(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "scoreFilter", required = false) String scoreFilter) {
        try {
            Object response = aiApiClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/admin/analysis/stats")
                            .queryParamIfPresent("keyword", Optional.ofNullable(keyword))
                            .queryParamIfPresent("scoreFilter", Optional.ofNullable(scoreFilter))
                            .build())
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

    @LogAction(action = "관리자 분석결과 강제삭제", domain = "AI", key = "'taskId=' + #taskId")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> forceDeleteAnalysis(@PathVariable("taskId") String taskId) {
        try {
            aiApiClient.delete()
                    .uri("/api/admin/analysis/{taskId}", taskId)
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

    @GetMapping("/trend")
    public ResponseEntity<Object> getAnalysisTrend(
            @RequestParam(name = "range", defaultValue = "daily") String range) {
        try {
            Object response = aiApiClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/admin/analysis/trend")
                            .queryParam("range", range)
                            .build())
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

    @GetMapping("/ranking")
    public ResponseEntity<Object> getAnalysisRanking(
            @RequestParam(name = "by", defaultValue = "disease") String by,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        try {
            Object response = aiApiClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/admin/analysis/ranking")
                            .queryParam("by", by)
                            .queryParam("limit", limit)
                            .build())
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

    private String extractDetail(String body) {
        try {
            return objectMapper.readTree(body).path("detail").asText(
                    objectMapper.readTree(body).path("message").asText("요청 처리 실패"));
        } catch (Exception ignored) {
            return "요청 처리 실패";
        }
    }
}
