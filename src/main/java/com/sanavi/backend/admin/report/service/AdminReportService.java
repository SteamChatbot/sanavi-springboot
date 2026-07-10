package com.sanavi.backend.admin.report.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.admin.report.dto.AdminReportListRequestDto;
import com.sanavi.backend.admin.report.dto.AdminReportPageResponseDto;
import com.sanavi.backend.admin.report.dto.AdminReportProcessRequestDto;
import com.sanavi.backend.admin.report.dto.AdminReportResponseDto;
import com.sanavi.backend.admin.report.dto.MemberAdminActionHistory;
import com.sanavi.backend.admin.report.mapper.AdminReportMapper;
import com.sanavi.backend.admin.role.service.AdminPermissionGuard;
import com.sanavi.backend.admin.role.service.AdminRolePolicy;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.security.TokenService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 관리자 신고관리 서비스
// 신고 목록 조회, 로그인 제한 처리, 강제탈퇴 처리, 반려 처리를 담당한다
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private static final Set<Integer> ALLOWED_RESTRICTION_DAYS = Set.of(3, 7, 30);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AdminReportMapper adminReportMapper;
    private final TokenService tokenService;
    private final AdminPermissionGuard adminPermissionGuard;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Transactional(readOnly = true)
    public AdminReportPageResponseDto getReports(AdminReportListRequestDto request, String adminUserId) {
        String status = normalizeFilter(request.getStatus());
        String targetType = normalizeFilter(request.getTargetType());
        String keyword = normalizeKeyword(request.getKeyword());

        int page = Math.max(request.getPage(), 1);
        int size = Math.max(request.getSafeSize(), 1);

        adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.REPORT_READ);

        int totalCount = adminReportMapper.countReports(
                status,
                targetType,
                keyword);

        var reports = adminReportMapper.selectReports(
                status,
                targetType,
                keyword,
                request.getOffset(),
                size);

        return new AdminReportPageResponseDto(
                reports,
                page,
                size,
                totalCount);
    }

    @Transactional
    public AdminReportResponseDto restrictLogin(
            Integer reportId,
            String adminUserId,
            AdminReportProcessRequestDto request) {
        Member admin = adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.REPORT_PROCESS);
        validateReason(request);
        validateRestrictionDays(request.getDays());

        AdminReportResponseDto report = getPendingReport(reportId);

        LocalDateTime restrictedUntil = LocalDateTime.now().plusDays(request.getDays());

        int reportUpdated = adminReportMapper.updateReportLoginRestriction(
                reportId,
                admin.getUserId(),
                request.getReason().trim(),
                request.getDays());

        if (reportUpdated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 신고입니다.");
        }

        int memberUpdated = adminReportMapper.updateMemberLoginRestriction(
                report.getReportedUserId(),
                admin.getUserId(),
                request.getReason().trim(),
                request.getDays(),
                restrictedUntil);

        if (memberUpdated != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "조치 대상 회원을 찾을 수 없습니다.");
        }

        insertHistory(
                report.getReportedUserId(),
                admin.getUserId(),
                reportId,
                "LOGIN_RESTRICT_" + request.getDays() + "D",
                request.getReason().trim(),
                "status=PENDING",
                "status=LOGIN_RESTRICTED, until=" + restrictedUntil.format(TIME_FORMATTER));

        tokenService.deleteRefreshToken(report.getReportedUserId());

        log.info(
                "action=ADMIN_REPORT_LOGIN_RESTRICT report_id={} target_user_id={} admin_user_id={} days={} until={} result=SUCCESS",
                reportId,
                report.getReportedUserId(),
                admin.getUserId(),
                request.getDays(),
                restrictedUntil);

        report.setRestrictedUntil(restrictedUntil);
        return report;
    }

    // 로그인 제한 알림 메일 — restrictLogin() 트랜잭션이 커밋된 뒤(컨트롤러에서) 호출해야 함
    // MatchNotificationService와 동일한 이유: 발송 실패가 로그인 제한 처리 자체를 롤백시키면 안 되고,
    // SMTP 왕복(~14초)으로 관리자 요청이 블로킹되지 않게 별도 스레드에서 처리
    @Async
    public void sendLoginRestrictionEmail(
            String toEmail,
            String memberName,
            String reason,
            int days,
            LocalDateTime restrictedUntil) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("action=ADMIN_REPORT_LOGIN_RESTRICT_MAIL result=SKIP reason=NO_EMAIL");
            return;
        }

        try {
            String subject = "[산내비] 로그인 제한 안내";
            String body = buildLoginRestrictionBody(memberName, reason, days, restrictedUntil);
            sendMail(toEmail, subject, body);

            log.info(
                    "action=ADMIN_REPORT_LOGIN_RESTRICT_MAIL target_email={} days={} result=SUCCESS",
                    toEmail, days);
        } catch (Exception e) {
            log.error(
                    "action=ADMIN_REPORT_LOGIN_RESTRICT_MAIL target_email={} result=FAIL exception={}",
                    toEmail, e.getClass().getSimpleName());
        }
    }

    private void sendMail(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(senderEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    private String buildLoginRestrictionBody(
            String memberName,
            String reason,
            int days,
            LocalDateTime restrictedUntil) {
        return """
                <div style="font-family: 'Apple SD Gothic Neo', sans-serif; max-width: 560px; margin: 0 auto; color: #222;">
                  <div style="background:#1a5c38; padding: 28px 32px;">
                    <h1 style="color:#fff; font-size:20px; margin:0;">산내비</h1>
                    <p style="color:#a8d5bc; font-size:13px; margin:6px 0 0;">산업재해 보험금 청구 AI 분석 서비스</p>
                  </div>
                  <div style="padding: 36px 32px;">
                    <h2 style="font-size:18px; margin:0 0 8px;">로그인 제한 안내</h2>
                    <p style="color:#555; font-size:14px; margin:0 0 28px;">안녕하세요, <strong>%s</strong>님.<br>신고 접수 및 검토 결과에 따라 아래와 같이 로그인이 제한되었습니다.</p>
                    <table style="width:100%%; border-collapse:collapse; font-size:14px;">
                      <tr style="border-top:2px solid #1a5c38;">
                        <td style="padding:14px 0; color:#777; width:35%%;">제한 기간</td>
                        <td style="padding:14px 0; font-weight:600;">%d일</td>
                      </tr>
                      <tr style="border-top:1px solid #eee;">
                        <td style="padding:14px 0; color:#777;">제한 해제일</td>
                        <td style="padding:14px 0;">%s</td>
                      </tr>
                      <tr style="border-top:1px solid #eee;">
                        <td style="padding:14px 0; color:#777;">사유</td>
                        <td style="padding:14px 0;">%s</td>
                      </tr>
                    </table>
                    <p style="color:#999; font-size:12px; margin:28px 0 0;">문의사항이 있으시면 고객센터로 연락해 주세요.</p>
                  </div>
                </div>
                """.formatted(memberName, days, restrictedUntil.format(TIME_FORMATTER), reason);
    }

    @Transactional
    public void withdraw(
            Integer reportId,
            String adminUserId,
            AdminReportProcessRequestDto request) {
        Member admin = adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.REPORT_PROCESS);
        validateReason(request);

        AdminReportResponseDto report = getPendingReport(reportId);

        int reportUpdated = adminReportMapper.updateReportWithdraw(
                reportId,
                admin.getUserId(),
                request.getReason().trim());

        if (reportUpdated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 신고입니다.");
        }

        int memberUpdated = adminReportMapper.forceWithdrawMember(report.getReportedUserId());

        if (memberUpdated != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "조치 대상 회원을 찾을 수 없습니다.");
        }

        insertHistory(
                report.getReportedUserId(),
                admin.getUserId(),
                reportId,
                "FORCE_WITHDRAW",
                request.getReason().trim(),
                "deleted=1",
                "deleted=0");

        tokenService.deleteRefreshToken(report.getReportedUserId());

        log.info(
                "action=ADMIN_REPORT_WITHDRAW report_id={} target_user_id={} admin_user_id={} result=SUCCESS",
                reportId,
                report.getReportedUserId(),
                admin.getUserId());
    }

    @Transactional
    public void dismiss(
            Integer reportId,
            String adminUserId,
            AdminReportProcessRequestDto request) {
        Member admin = adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.REPORT_PROCESS);
        validateReason(request);

        AdminReportResponseDto report = getPendingReport(reportId);

        int reportUpdated = adminReportMapper.updateReportDismiss(
                reportId,
                admin.getUserId(),
                request.getReason().trim());

        if (reportUpdated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 신고입니다.");
        }

        insertHistory(
                report.getReportedUserId(),
                admin.getUserId(),
                reportId,
                "DISMISS_REPORT",
                request.getReason().trim(),
                "status=PENDING",
                "status=DISMISSED");

        log.info(
                "action=ADMIN_REPORT_DISMISS report_id={} target_user_id={} admin_user_id={} result=SUCCESS",
                reportId,
                report.getReportedUserId(),
                admin.getUserId());
    }

    private AdminReportResponseDto getPendingReport(Integer reportId) {
        if (reportId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 번호가 필요합니다.");
        }

        AdminReportResponseDto report = adminReportMapper.selectReportById(reportId);

        if (report == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다.");
        }

        if (!"PENDING".equals(report.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 신고입니다.");
        }

        return report;
    }

    private void validateReason(AdminReportProcessRequestDto request) {
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "처리 사유를 입력해 주세요.");
        }

        if (request.getReason().trim().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "처리 사유는 500자 이하로 입력해 주세요.");
        }
    }

    private void validateRestrictionDays(Integer days) {
        if (days == null || !ALLOWED_RESTRICTION_DAYS.contains(days)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "로그인 제한 일수는 3일, 7일, 30일만 가능합니다.");
        }
    }

    private void insertHistory(
            String targetUserId,
            String adminUserId,
            Integer reportId,
            String actionType,
            String reason,
            String beforeValue,
            String afterValue) {
        MemberAdminActionHistory history = new MemberAdminActionHistory();
        history.setTargetUserId(targetUserId);
        history.setAdminUserId(adminUserId);
        history.setReportId(reportId);
        history.setActionType(actionType);
        history.setActionReason(reason);
        history.setBeforeValue(beforeValue);
        history.setAfterValue(afterValue);

        adminReportMapper.insertAdminActionHistory(history);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }

        return value.trim().toUpperCase();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }
}