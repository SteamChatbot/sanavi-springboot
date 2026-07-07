package com.sanavi.backend.auth.service;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.sanavi.backend.common.exception.DuplicateMemberException;
import com.sanavi.backend.common.exception.EmailVerificationException;
import com.sanavi.backend.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_PREFIX = "email:code:";
    private static final String VERIFIED_PREFIX = "email:verified:";

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final MemberMapper memberMapper;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendCode(String email) {
        if (email == null || email.isBlank()) {
            log.warn(
                    "action=EMAIL_CODE_SEND result=DENIED reason=MISSING_EMAIL");

            throw new EmailVerificationException("이메일을 입력해 주세요.");
        }

        if (memberMapper.countByEmail(email) > 0) {
            log.warn(
                    "action=EMAIL_CODE_SEND email_masked={} result=DENIED reason=DUPLICATE_EMAIL",
                    maskEmail(email));

            throw new DuplicateMemberException("이미 가입된 이메일입니다.");
        }

        String code = String.format(
                "%06d", // 6자리 정수
                new SecureRandom().nextInt(1_000_000));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(email);
        message.setSubject("[산내비] 이메일 인증번호");
        message.setText(
                "이메일 인증번호는 " + code + "입니다.\n"
                        + "인증번호는 5분 동안 유효합니다.");

        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.error(
                    "action=EMAIL_CODE_SEND email_masked={} result=FAIL reason=MAIL_SEND_FAILED exception={}",
                    maskEmail(email),
                    e.getClass().getSimpleName());

            throw e;
        }

        try {
            redisTemplate.opsForValue().set(
                    CODE_PREFIX + email,
                    code,
                    Duration.ofMinutes(5));
        } catch (RuntimeException e) {
            log.error(
                    "action=EMAIL_CODE_SEND email_masked={} result=FAIL reason=REDIS_SAVE_FAILED exception={}",
                    maskEmail(email),
                    e.getClass().getSimpleName());

            throw e;
        }

        log.info(
                "action=EMAIL_CODE_SEND email_masked={} result=SUCCESS",
                maskEmail(email));
    }

    public void verifyCode(String email, String code) {
        if (email == null || email.isBlank()) {
            log.warn(
                    "action=EMAIL_CODE_VERIFY result=DENIED reason=MISSING_EMAIL");

            throw new EmailVerificationException("이메일을 입력해 주세요.");
        }

        if (code == null || code.isBlank()) {
            log.warn(
                    "action=EMAIL_CODE_VERIFY email_masked={} result=DENIED reason=MISSING_CODE",
                    maskEmail(email));

            throw new EmailVerificationException("인증번호를 입력해 주세요.");
        }

        String savedCode = redisTemplate.opsForValue()
                .get(CODE_PREFIX + email);

        if (savedCode == null) {
            log.warn(
                    "action=EMAIL_CODE_VERIFY email_masked={} result=FAIL reason=CODE_EXPIRED_OR_NOT_FOUND",
                    maskEmail(email));

            throw new EmailVerificationException(
                    "인증번호가 만료되었거나 존재하지 않습니다.");
        }

        if (!savedCode.equals(code)) {
            log.warn(
                    "action=EMAIL_CODE_VERIFY email_masked={} result=FAIL reason=INVALID_CODE",
                    maskEmail(email));

            throw new EmailVerificationException(
                    "인증번호가 올바르지 않습니다.");
        }

        try {
            redisTemplate.delete(CODE_PREFIX + email);

            redisTemplate.opsForValue().set(
                    VERIFIED_PREFIX + email,
                    "true",
                    Duration.ofMinutes(30));
        } catch (RuntimeException e) {
            log.error(
                    "action=EMAIL_CODE_VERIFY email_masked={} result=FAIL reason=REDIS_UPDATE_FAILED exception={}",
                    maskEmail(email),
                    e.getClass().getSimpleName());

            throw e;
        }

        log.info(
                "action=EMAIL_CODE_VERIFY email_masked={} result=SUCCESS",
                maskEmail(email));
    }

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(VERIFIED_PREFIX + email));
    }

    public void deleteVerification(String email) {
        redisTemplate.delete(VERIFIED_PREFIX + email);

        log.debug(
                "action=EMAIL_VERIFICATION_DELETE email_masked={} result=SUCCESS",
                maskEmail(email));
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }

        int atIndex = email.indexOf("@");

        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        return localPart.charAt(0) + "***@" + domain;
    }
}