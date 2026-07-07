package com.sanavi.backend.admin.mail.dto;

// 관리자 대량 메일 발송 요청 바디 — POST /api/admin/mail/send
public record MailSendRequestDto(
        MailAudienceFilter filter,
        String subject,
        String htmlBody
) {
}
