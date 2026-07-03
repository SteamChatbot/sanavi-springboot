package com.sanavi.backend.admin.mail.dto;

// 관리자 대량 메일 — 대상 인원수 미리보기(/audience/count)와 발송 시작(/send) 응답이 공유하는 형태
public record MailSendResultDto(int targetCount) {
}
