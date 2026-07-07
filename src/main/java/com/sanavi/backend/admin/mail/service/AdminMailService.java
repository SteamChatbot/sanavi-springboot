package com.sanavi.backend.admin.mail.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sanavi.backend.admin.mail.dto.MailAudienceFilter;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.member.mapper.MemberMapper;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 책임: 관리자 대량 메일 — 대상 조회 + 병렬 발송
// MatchNotificationService와 동일한 @Async + MimeMessageHelper 패턴 재사용.
// 메일 1건당 SMTP 발송이 약 14초 걸린다고 실측돼 있어(MatchNotificationService 주석 참고) 순차 발송은 안 되고
// 고정 스레드풀로 병렬 처리 — 수십~수백명 규모 기준 동시 5개면 충분(Gmail 무료 계정 일일 발송한도 ~500건 이내)
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMailService {

    private static final int PARALLELISM = 5;

    private final JavaMailSender mailSender;
    private final MemberMapper memberMapper;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public int getAudienceCount(MailAudienceFilter filter) {
        return memberMapper.countMailTargets(filter);
    }

    public List<String> getDistinctJobs() {
        return memberMapper.findDistinctJobs();
    }

    // 컨트롤러가 호출 즉시 반환(대상 인원수만 먼저 응답)하고, 실제 발송은 이 메서드 안에서 비동기로 진행
    @Async
    public void sendBulkMail(MailAudienceFilter filter, String subject, String htmlBody) {
        List<Member> targets = memberMapper.searchMailTargets(filter);

        log.info("action=BULK_MAIL_SEND target_count={} result=START", targets.size());

        ExecutorService executor = Executors.newFixedThreadPool(PARALLELISM);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Member member : targets) {
                futures.add(executor.submit(() -> sendOne(member, subject, htmlBody)));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    // 개별 발송 실패는 sendOne 내부에서 이미 로그로 남김 — 여기서는 전체 흐름을 막지 않기 위해 무시
                }
            }
        } finally {
            executor.shutdown();
        }

        log.info("action=BULK_MAIL_SEND target_count={} result=COMPLETE", targets.size());
    }

    private void sendOne(Member member, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(member.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);

            log.info("action=BULK_MAIL_SEND target_user_id={} result=SUCCESS", member.getUserId());
        } catch (Exception e) {
            log.error(
                    "action=BULK_MAIL_SEND target_user_id={} result=FAIL exception={}",
                    member.getUserId(), e.getClass().getSimpleName());
        }
    }
}
