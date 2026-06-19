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
        if (memberMapper.countByEmail(email) > 0) {
            throw new DuplicateMemberException("이미 가입된 이메일입니다.");
        }

        String code = String.format(
                "%06d",
                new SecureRandom().nextInt(1_000_000)
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(email);
        message.setSubject("[산내비] 이메일 인증번호");
        message.setText(
                "이메일 인증번호는 " + code + "입니다.\n"
                + "인증번호는 5분 동안 유효합니다."
        );

        mailSender.send(message);

        redisTemplate.opsForValue().set(
                CODE_PREFIX + email,
                code,
                Duration.ofMinutes(5)
        );
    }

    public void verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue()
                .get(CODE_PREFIX + email);

        if (savedCode == null) {
            throw new EmailVerificationException(
                    "인증번호가 만료되었거나 존재하지 않습니다."
            );
        }

        if (!savedCode.equals(code)) {
            throw new EmailVerificationException(
                    "인증번호가 올바르지 않습니다."
            );
        }

        redisTemplate.delete(CODE_PREFIX + email);

        redisTemplate.opsForValue().set(
                VERIFIED_PREFIX + email,
                "true",
                Duration.ofMinutes(30)
        );
    }

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(VERIFIED_PREFIX + email)
        );
    }

    public void deleteVerification(String email) {
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }
}