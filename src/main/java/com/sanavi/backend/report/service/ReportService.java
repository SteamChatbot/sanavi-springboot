package com.sanavi.backend.report.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.member.mapper.MemberMapper;
import com.sanavi.backend.report.dto.Report;
import com.sanavi.backend.report.dto.ReportRequestDto;
import com.sanavi.backend.report.mapper.ReportMapper;

import lombok.RequiredArgsConstructor;

// 유저신고 — 의뢰글 작성자/입찰자, 변호사찾기·입찰목록의 변호사 등 회원을 대상으로 신고 접수
// 신고 처리(블랙리스트 등록/강제탈퇴/반려)는 관리자 신고관리 화면에서 별도로 진행 — 이 서비스는 접수만 책임진다
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "욕설/비방", "허위사실유포", "시세조작", "성적콘텐츠포함", "스팸/광고", "기타");

    private final ReportMapper reportMapper;
    private final MemberMapper memberMapper;

    @Transactional
    public void reportUser(String reporterId, ReportRequestDto request) {
        if (reporterId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String reportedUserId = request.getReportedUserId();
        String category = request.getCategory();
        String detail = request.getDetail();

        if (reportedUserId == null || reportedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 대상 회원을 확인할 수 없습니다.");
        }

        if (reportedUserId.equals(reporterId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인을 신고할 수 없습니다.");
        }

        if (category == null || !ALLOWED_CATEGORIES.contains(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 신고 카테고리입니다.");
        }

        if ("기타".equals(category) && (detail == null || detail.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기타 사유는 상세 내용을 입력해야 합니다.");
        }

        if (memberMapper.countByUserId(reportedUserId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다.");
        }

        Report report = new Report();
        report.setReportedUserId(reportedUserId);
        report.setReportUserId(reporterId);
        report.setCategory(category);
        report.setDetail(detail);
        report.setCreatedAt(LocalDateTime.now());

        reportMapper.insertReport(report);
    }
}
