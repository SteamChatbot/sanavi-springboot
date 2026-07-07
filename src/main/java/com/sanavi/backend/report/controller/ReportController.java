package com.sanavi.backend.report.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.common.response.ApiResponse;
import com.sanavi.backend.report.dto.ReportRequestDto;
import com.sanavi.backend.report.service.ReportService;

import lombok.RequiredArgsConstructor;

// 유저신고 접수 — 의뢰글 작성자/입찰자, 변호사찾기·입찰목록의 변호사 신고 버튼이 호출
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> reportUser(
            @RequestBody ReportRequestDto request,
            @AuthenticationPrincipal String userId) {

        reportService.reportUser(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("신고가 접수되었습니다.", null));
    }
}
