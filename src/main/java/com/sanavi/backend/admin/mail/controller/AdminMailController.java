package com.sanavi.backend.admin.mail.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.mail.dto.MailAudienceFilter;
import com.sanavi.backend.admin.mail.dto.MailSendRequestDto;
import com.sanavi.backend.admin.mail.dto.MailSendResultDto;
import com.sanavi.backend.admin.mail.service.AdminMailService;

import lombok.RequiredArgsConstructor;

// 관리자 전용 — 대량 메일 발송(구독 감사/프로모션/공지 등)
// role_admin 권한 검사는 SecurityConfig에서 처리해야 함 — 현재 SecurityConfig가 anyRequest().permitAll()
// 상태라 활성화 전까지는 이 엔드포인트가 실제로 보호되지 않음 (CLAUDE.md "알려진 이슈" 참고)
@RestController
@RequestMapping("/api/admin/mail")
@RequiredArgsConstructor
public class AdminMailController {

    private final AdminMailService adminMailService;

    @GetMapping("/jobs")
    public List<String> getJobOptions() {
        return adminMailService.getDistinctJobs();
    }

    @GetMapping("/audience/count")
    public MailSendResultDto getAudienceCount(
            @RequestParam(name = "userIds", required = false) List<String> userIds,
            @RequestParam(name = "subscribe", required = false) Integer subscribe,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "jobs", required = false) List<String> jobs,
            @RequestParam(name = "createdFrom", required = false) LocalDate createdFrom,
            @RequestParam(name = "createdTo", required = false) LocalDate createdTo,
            @RequestParam(name = "excludeBlacklist", defaultValue = "true") boolean excludeBlacklist,
            @RequestParam(name = "excludeLawyer", defaultValue = "false") boolean excludeLawyer,
            @RequestParam(name = "excludeAlreadyPro", defaultValue = "false") boolean excludeAlreadyPro) {
        MailAudienceFilter filter = buildFilter(
                userIds, subscribe, role, jobs, createdFrom, createdTo,
                excludeBlacklist, excludeLawyer, excludeAlreadyPro);
        return new MailSendResultDto(adminMailService.getAudienceCount(filter));
    }

    @PostMapping("/send")
    public MailSendResultDto sendBulkMail(@RequestBody MailSendRequestDto request) {
        int targetCount = adminMailService.getAudienceCount(request.filter());
        adminMailService.sendBulkMail(request.filter(), request.subject(), request.htmlBody());
        return new MailSendResultDto(targetCount);
    }

    private MailAudienceFilter buildFilter(
            List<String> userIds, Integer subscribe, String role, List<String> jobs,
            LocalDate createdFrom, LocalDate createdTo,
            boolean excludeBlacklist, boolean excludeLawyer, boolean excludeAlreadyPro) {
        MailAudienceFilter filter = new MailAudienceFilter();
        filter.setUserIds(userIds);
        filter.setSubscribe(subscribe);
        filter.setRole(role);
        filter.setJobs(jobs);
        filter.setCreatedFrom(createdFrom);
        filter.setCreatedTo(createdTo);
        filter.setExcludeBlacklist(excludeBlacklist);
        filter.setExcludeLawyer(excludeLawyer);
        filter.setExcludeAlreadyPro(excludeAlreadyPro);
        return filter;
    }
}
