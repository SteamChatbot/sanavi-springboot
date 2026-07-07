package com.sanavi.backend.analysis.service;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

// 책임: backend-springboot가 실제로 주인인 부분만 — 구독 플랜별 ai_count 검증(main_db 소유)과
// ai-api 프록시 호출 + 성공 후 ai_count 증가. 분석 자체의 로직/데이터(analysis_result 등)는 ai_db 소유라
// ai-api 담당이며 여기서는 다루지 않음 — main_db ↔ ai_db FK 없음 규칙(CLAUDE.md) 그대로 유지.
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final RestClient aiApiClient;
    private final MemberMapper memberMapper;
    private final ObjectMapper objectMapper;

    public Object requestAnalysis(Map<String, Object> body, String userId) {
        checkQuota(userId);

        Object response = proxyCall(() -> aiApiClient.post()
                .uri("/api/analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId != null ? userId : "")
                .body(body)
                .retrieve()
                .body(Object.class));

        if (userId != null) {
            memberMapper.incrementAiCount(userId);
        }
        return response;
    }

    public Object getResult(String taskId) {
        return proxyCall(() -> aiApiClient.get()
                .uri("/api/analysis/{taskId}", taskId)
                .retrieve().body(Object.class));
    }

    public Object chat(Object body) {
        return proxyCall(() -> aiApiClient.post()
                .uri("/api/analysis/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(Object.class));
    }

    public Object getHistory(String userId) {
        return proxyCall(() -> aiApiClient.get()
                .uri("/api/analysis/history/{userId}", userId)
                .retrieve().body(Object.class));
    }

    public void deleteAnalysis(String taskId, String userId) {
        proxyCall(() -> {
            aiApiClient.delete()
                    .uri(b -> b.path("/api/analysis/{taskId}")
                            .queryParam("userId", userId).build(taskId))
                    .retrieve().toBodilessEntity();
            return null;
        });
    }

    // userId가 null이면 비로그인 — 횟수 제한 없음, DB 저장 안 됨
    private void checkQuota(String userId) {
        if (userId == null) return;

        Member member = memberMapper.findByUserId(userId);
        if (member != null && member.getSubscribe() == 0 && member.getAiCount() >= 3) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "AI 분석 횟수(3회)를 초과했습니다. Pro 플랜으로 업그레이드하세요.");
        }
    }

    // ai-api 호출 4곳에서 반복되던 예외 매핑을 한 곳으로 모음
    private <T> T proxyCall(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpStatusCodeException e) {
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
