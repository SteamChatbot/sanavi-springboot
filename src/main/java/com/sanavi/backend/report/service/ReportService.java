package com.sanavi.backend.report.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.member.mapper.MemberMapper;
import com.sanavi.backend.report.dto.Report;
import com.sanavi.backend.report.dto.ReportRequestDto;
import com.sanavi.backend.report.mapper.ReportMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 유저신고 — 의뢰글 작성자/입찰자, 변호사찾기·입찰목록의 변호사 등 회원을 대상으로 신고 접수
// 신고 처리(블랙리스트 등록/강제탈퇴/반려)는 관리자 신고관리 화면에서 별도로 진행 — 이 서비스는 접수만 책임진다
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "욕설/비방", "허위사실유포", "시세조작", "성적콘텐츠포함", "스팸/광고", "기타");

    private static final Set<String> CONTENT_TARGET_TYPES = Set.of(
            "BOARD", "BOARD_COMMENT", "MATCH");

    private static final Set<String> MEMBER_TARGET_TYPES = Set.of(
            "MEMBER", "LAWYER");

    private final ReportMapper reportMapper;
    private final MemberMapper memberMapper;

    @Transactional
    public void reportUser(String reporterId, ReportRequestDto request) {
        if (reporterId == null || reporterId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 요청이 비어 있습니다.");
        }

        String targetType = normalizeTargetType(request.getTargetType());

        // 게시글/댓글/의뢰글 신고
        if (CONTENT_TARGET_TYPES.contains(targetType)) {
            reportContent(reporterId, request, targetType);
            return;
        }

        // 기존 유저/변호사 신고
        reportMember(reporterId, request, targetType);
    }

    private void reportMember(
            String reporterId,
            ReportRequestDto request,
            String targetType) {
        String reportedUserId = trimToNull(request.getReportedUserId());
        String category = request.getCategory();
        String detail = request.getDetail();

        if (reportedUserId == null) {
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

        String resolvedTargetType = resolveMemberTargetType(targetType);
        String targetId = reportedUserId;

        ensureNotDuplicate(reporterId, resolvedTargetType, targetId);

        Report report = new Report();
        report.setReportedUserId(reportedUserId);
        report.setReportUserId(reporterId);
        report.setTargetType(resolvedTargetType);
        report.setTargetId(targetId);
        report.setCategory(category);
        report.setDetail(detail);
        report.setCreatedAt(LocalDateTime.now());
        report.setStatus("PENDING");

        insertReport(report, reporterId, resolvedTargetType, targetId);

        log.info(
                "action=REPORT_CREATE target_type={} target_id={} reporter_user_id={} reported_user_id={} result=SUCCESS",
                resolvedTargetType,
                targetId,
                reporterId,
                reportedUserId);
    }

    private void reportContent(
            String reporterId,
            ReportRequestDto request,
            String targetType) {
        String targetId = trimToNull(request.getTargetId());

        if (targetId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 대상 ID가 필요합니다.");
        }

        int numericTargetId = parseNumericTargetId(targetId);

        String reportedUserId = resolveReportedUserId(targetType, numericTargetId);

        if (reportedUserId == null || reportedUserId.isBlank()) {
            log.warn(
                    "action=REPORT_CREATE target_type={} target_id={} reporter_user_id={} result=DENIED reason=TARGET_NOT_FOUND",
                    targetType,
                    targetId,
                    reporterId);

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다.");
        }

        if (reportedUserId.equals(reporterId)) {
            log.warn(
                    "action=REPORT_CREATE target_type={} target_id={} reporter_user_id={} result=DENIED reason=SELF_REPORT",
                    targetType,
                    targetId,
                    reporterId);

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인이 작성한 대상은 신고할 수 없습니다.");
        }

        ensureNotDuplicate(reporterId, targetType, targetId);

        Report report = new Report();
        report.setReportedUserId(reportedUserId);
        report.setReportUserId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setCategory("CONTENT_REPORT");
        report.setDetail(null);
        report.setCreatedAt(LocalDateTime.now());
        report.setStatus("PENDING");

        insertReport(report, reporterId, targetType, targetId);

        increaseReportCount(targetType, numericTargetId);

        log.info(
                "action=REPORT_CREATE target_type={} target_id={} reporter_user_id={} reported_user_id={} result=SUCCESS",
                targetType,
                targetId,
                reporterId,
                reportedUserId);
    }

    private String resolveReportedUserId(String targetType, int targetId) {
        return switch (targetType) {
            case "BOARD" -> reportMapper.selectBoardOwner(targetId);
            case "BOARD_COMMENT" -> reportMapper.selectBoardCommentOwner(targetId);
            case "MATCH" -> reportMapper.selectMatchOwner(targetId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 신고 대상입니다.");
        };
    }

    private void increaseReportCount(String targetType, int targetId) {
        int result = switch (targetType) {
            case "BOARD" -> reportMapper.increaseBoardReportCount(targetId);
            case "BOARD_COMMENT" -> reportMapper.increaseBoardCommentReportCount(targetId);
            case "MATCH" -> reportMapper.increaseMatchReportCount(targetId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 신고 대상입니다.");
        };

        if (result != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "신고 수 증가에 실패했습니다.");
        }
    }

    private void ensureNotDuplicate(
            String reporterId,
            String targetType,
            String targetId) {
        if (reportMapper.countDuplicateReport(reporterId, targetType, targetId) > 0) {
            log.warn(
                    "action=REPORT_CREATE target_type={} target_id={} reporter_user_id={} result=DENIED reason=DUPLICATE_REPORT",
                    targetType,
                    targetId,
                    reporterId);

            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 대상입니다.");
        }
    }

    private void insertReport(
            Report report,
            String reporterId,
            String targetType,
            String targetId) {
        try {
            reportMapper.insertReport(report);
        } catch (DuplicateKeyException e) {
            log.warn(
                    "action=REPORT_CREATE target_type={} target_id={} reporter_user_id={} result=DENIED reason=DUPLICATE_REPORT",
                    targetType,
                    targetId,
                    reporterId);

            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 대상입니다.");
        }
    }

    private int parseNumericTargetId(String targetId) {
        try {
            return Integer.parseInt(targetId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 대상 ID가 올바르지 않습니다.");
        }
    }

    private String normalizeTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return null;
        }

        return targetType.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveMemberTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return "MEMBER";
        }

        if (MEMBER_TARGET_TYPES.contains(targetType)) {
            return targetType;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 신고 대상입니다.");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}