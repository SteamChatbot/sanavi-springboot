package com.sanavi.backend.admin.mail.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 대량 메일 — 발송 대상 필터 조건. 컨트롤러 요청 파라미터를 그대로 담아 서비스/매퍼로 전달
// userIds가 있으면 다른 조건은 전부 무시하고 그 유저ID 목록만 대상으로 삼음
// (회원관리 페이지에서 회원을 직접 선택해 "선택한 회원에게 메일보내기"로 넘어오는 연동 지점 — 아직 회원관리 쪽 미구현이라 지금은 필터 UI로만 사용)
@Getter
@Setter
@NoArgsConstructor
public class MailAudienceFilter {
    private List<String> userIds;

    private Integer subscribe; // null=전체, 0=Basic, 1=Pro
    private String role; // null=전체, role_user, role_lawyer
    private List<String> jobs;
    private LocalDate createdFrom;
    private LocalDate createdTo;

    private boolean excludeBlacklist = true;
    private boolean excludeLawyer = false;
    private boolean excludeAlreadyPro = false;
}
