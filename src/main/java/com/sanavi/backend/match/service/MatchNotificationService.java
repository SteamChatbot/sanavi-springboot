package com.sanavi.backend.match.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sanavi.backend.match.dto.MatchBidResponseDto;
import com.sanavi.backend.match.dto.MatchResponseDto;
import com.sanavi.backend.match.mapper.MatchBidMapper;
import com.sanavi.backend.match.mapper.MatchMapper;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.member.mapper.MemberMapper;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 책임: 매칭 확정(selectBid) 시 의뢰인·변호사 양쪽에 HTML 이메일 발송
//       트랜잭션 밖(컨트롤러)에서 호출 — 발송 실패가 확정 결과를 롤백하지 않음
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchNotificationService {

    private final JavaMailSender mailSender;
    private final MatchMapper matchMapper;
    private final MatchBidMapper matchBidMapper;
    private final MemberMapper memberMapper;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.base-url:https://sanavi.co.kr}")
    private String baseUrl;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");

    // @Async 도입 배경:
    //   동기 발송 시 SMTP 왕복으로 클라이언트 응답이 ~14초 블로킹됨
    //   실측 로그: START MatchNotificationService.sendMatchConfirmed → 의뢰인 메일 발송 완료까지 14s
    //   → 호출 즉시 반환하고 메일 발송은 별도 스레드에서 진행
    // selectBid() 트랜잭션이 커밋된 뒤 컨트롤러에서 호출하므로 DB 데이터 정합성 보장
    // 발송 실패가 매칭 확정 결과에 영향을 주지 않도록 내부에서 예외를 흡수
    @Async
    public void sendMatchConfirmed(int matchId, int bidId) {
        try {
            MatchResponseDto match = matchMapper.selectMatchById(matchId);
            MatchBidResponseDto bid = matchBidMapper.selectBidById(bidId);
            if (match == null || bid == null) return;

            Member requester = memberMapper.findByUserId(match.getUserId());
            Member lawyer = memberMapper.findByUserId(bid.getLawyerId());
            if (requester == null || lawyer == null) return;

            String matchUrl = baseUrl + "/match/" + matchId;
            String confirmedAt = LocalDateTime.now().format(FORMATTER);

            sendToRequester(requester.getEmail(), requester.getName(),
                    bid.getLawyerName(), lawyer.getPhone(), bid.getBidPrice(), confirmedAt, matchUrl);

            sendToLawyer(lawyer.getEmail(), bid.getLawyerName(),
                    match.getRequesterName(), requester.getPhone(), bid.getBidPrice(), confirmedAt, matchUrl);

        } catch (Exception e) {
            log.error("매칭 확정 메일 발송 실패 — matchId={} bidId={} reason={}", matchId, bidId, e.getMessage());
        }
    }

    private void sendToRequester(String email, String requesterName,
                                  String lawyerName, String lawyerPhone, int bidPrice,
                                  String confirmedAt, String matchUrl) throws Exception {
        String subject = "[산내비] 변호사 매칭이 확정되었습니다";
        String body = buildRequesterBody(requesterName, lawyerName, lawyerPhone, bidPrice, confirmedAt, matchUrl);
        send(email, subject, body);
        log.info("매칭 확정 메일 발송(의뢰인) — to={}", email);
    }

    private void sendToLawyer(String email, String lawyerName,
                               String requesterName, String requesterPhone, int bidPrice,
                               String confirmedAt, String matchUrl) throws Exception {
        String subject = "[산내비] 새로운 의뢰가 확정되었습니다";
        String body = buildLawyerBody(lawyerName, requesterName, requesterPhone, bidPrice, confirmedAt, matchUrl);
        send(email, subject, body);
        log.info("매칭 확정 메일 발송(변호사) — to={}", email);
    }

    // MimeMessage: HTML 본문 지원 (SimpleMailMessage는 plain text만 가능)
    private void send(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(senderEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    // Input:  의뢰인 이름, 변호사 이름, 변호사 전화번호, 확정 금액, 확정 일시, 의뢰글 URL
    // Output: HTML 문자열 — 의뢰인 수신용
    private String buildRequesterBody(String requesterName, String lawyerName, String lawyerPhone,
                                       int bidPrice, String confirmedAt, String matchUrl) {
        return """
                <div style="font-family: 'Apple SD Gothic Neo', sans-serif; max-width: 560px; margin: 0 auto; color: #222;">
                  <div style="background:#1a5c38; padding: 28px 32px;">
                    <h1 style="color:#fff; font-size:20px; margin:0;">산내비</h1>
                    <p style="color:#a8d5bc; font-size:13px; margin:6px 0 0;">산업재해 보험금 청구 AI 분석 서비스</p>
                  </div>
                  <div style="padding: 36px 32px;">
                    <h2 style="font-size:18px; margin:0 0 8px;">변호사 매칭이 확정되었습니다</h2>
                    <p style="color:#555; font-size:14px; margin:0 0 28px;">안녕하세요, <strong>%s</strong>님.<br>아래 내용으로 변호사 매칭이 확정되었습니다.</p>
                    <table style="width:100%%; border-collapse:collapse; font-size:14px;">
                      <tr style="border-top:2px solid #1a5c38;">
                        <td style="padding:14px 0; color:#777; width:35%%;">담당 변호사</td>
                        <td style="padding:14px 0; font-weight:600;">%s 변호사</td>
                      </tr>
                      <tr style="border-top:1px solid #eee;">
                        <td style="padding:14px 0; color:#777;">변호사 연락처</td>
                        <td style="padding:14px 0;">%s</td>
                      </tr>
                      <tr style="border-top:1px solid #eee;">
                        <td style="padding:14px 0; color:#777;">확정 금액</td>
                        <td style="padding:14px 0; font-weight:600;">%,d원</td>
                      </tr>
                      <tr style="border-top:1px solid #eee; border-bottom:2px solid #1a5c38;">
                        <td style="padding:14px 0; color:#777;">확정 일시</td>
                        <td style="padding:14px 0;">%s</td>
                      </tr>
                    </table>
                    <div style="margin: 28px 0;">
                      <a href="%s" style="display:inline-block; background:#1a5c38; color:#fff; text-decoration:none; padding:12px 24px; border-radius:6px; font-size:14px;">의뢰글 바로가기</a>
                    </div>
                    <p style="font-size:13px; color:#888; line-height:1.8; border-top:1px solid #eee; padding-top:20px;">
                      앞으로의 상담 일정은 담당 변호사와 직접 조율해 주세요.<br>
                      궁금하신 점은 산내비 고객센터로 문의해 주세요.<br><br>
                      감사합니다.<br>
                      <strong>산내비 팀 드림</strong>
                    </p>
                  </div>
                </div>
                """.formatted(requesterName, lawyerName, lawyerPhone, bidPrice, confirmedAt, matchUrl);
    }

    // Input:  변호사 이름, 의뢰인 이름, 의뢰인 전화번호, 확정 금액, 확정 일시, 의뢰글 URL
    // Output: HTML 문자열 — 변호사 수신용
    private String buildLawyerBody(String lawyerName, String requesterName, String requesterPhone,
                                    int bidPrice, String confirmedAt, String matchUrl) {
        return """
                <div style="font-family: 'Apple SD Gothic Neo', sans-serif; max-width: 560px; margin: 0 auto; color: #222;">
                  <div style="background:#1a5c38; padding: 28px 32px;">
                    <h1 style="color:#fff; font-size:20px; margin:0;">산내비</h1>
                    <p style="color:#a8d5bc; font-size:13px; margin:6px 0 0;">산업재해 보험금 청구 AI 분석 서비스</p>
                  </div>
                  <div style="padding: 36px 32px;">
                    <h2 style="font-size:18px; margin:0 0 8px;">새로운 의뢰가 확정되었습니다</h2>
                    <p style="color:#555; font-size:14px; margin:0 0 28px;">안녕하세요, <strong>%s 변호사</strong>님.<br>아래 내용으로 의뢰가 확정되었습니다.</p>
                    <table style="width:100%%; border-collapse:collapse; font-size:14px;">
                      <tr style="border-top:2px solid #1a5c38;">
                        <td style="padding:14px 0; color:#777; width:35%%;">의뢰인</td>
                        <td style="padding:14px 0; font-weight:600;">%s</td>
                      </tr>
                      <tr style="border-top:1px solid #eee;">
                        <td style="padding:14px 0; color:#777;">의뢰인 연락처</td>
                        <td style="padding:14px 0;">%s</td>
                      </tr>
                      <tr style="border-top:1px solid #eee;">
                        <td style="padding:14px 0; color:#777;">확정 금액</td>
                        <td style="padding:14px 0; font-weight:600;">%,d원</td>
                      </tr>
                      <tr style="border-top:1px solid #eee; border-bottom:2px solid #1a5c38;">
                        <td style="padding:14px 0; color:#777;">확정 일시</td>
                        <td style="padding:14px 0;">%s</td>
                      </tr>
                    </table>
                    <div style="margin: 28px 0;">
                      <a href="%s" style="display:inline-block; background:#1a5c38; color:#fff; text-decoration:none; padding:12px 24px; border-radius:6px; font-size:14px;">의뢰글 바로가기</a>
                    </div>
                    <p style="font-size:13px; color:#888; line-height:1.8; border-top:1px solid #eee; padding-top:20px;">
                      빠른 시일 내에 의뢰인과 상담 일정을 조율해 주세요.<br>
                      궁금하신 점은 산내비 고객센터로 문의해 주세요.<br><br>
                      감사합니다.<br>
                      <strong>산내비 팀 드림</strong>
                    </p>
                  </div>
                </div>
                """.formatted(lawyerName, requesterName, requesterPhone, bidPrice, confirmedAt, matchUrl);
    }
}
