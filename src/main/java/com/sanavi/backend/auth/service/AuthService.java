package com.sanavi.backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanavi.backend.auth.dto.CheckIdResponse;
import com.sanavi.backend.auth.dto.LoginRequest;
import com.sanavi.backend.auth.dto.LoginResponse;
import com.sanavi.backend.auth.dto.SignupRequest;
import com.sanavi.backend.auth.dto.SignupResponse;
import com.sanavi.backend.common.exception.DuplicateMemberException;
import com.sanavi.backend.common.exception.EmailVerificationException;
import com.sanavi.backend.common.exception.LoginFailedException;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    // 격리수준: REPEATABLE_READ (MariaDB 기본값)
    // 중복 체크(SELECT) → INSERT 사이에 다른 트랜잭션이 같은 userId로 INSERT해도
    // DB unique 제약이 최종 방어선 — 격리수준만으론 race condition 완전 방지 불가
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (memberMapper.countByUserId(request.getUserId()) > 0) {
            throw new DuplicateMemberException("이미 사용 중인 아이디입니다.");
        }

        if (memberMapper.countByEmail(request.getEmail()) > 0) {
            throw new DuplicateMemberException("이미 사용 중인 이메일입니다.");
        }

        if (!emailVerificationService.isVerified(request.getEmail())) {
            throw new EmailVerificationException(
                    "이메일 인증이 완료되지 않았습니다.");
        }

        Member member = new Member();
        member.setUserId(request.getUserId());
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        member.setName(request.getName());
        member.setPhone(request.getPhone());
        member.setEmail(request.getEmail());
        member.setBirth(request.getBirth());
        member.setJob(request.getJob());
        member.setGender(request.getGender());

        int result = memberMapper.insertMember(member);

        if (result != 1) {
            throw new IllegalStateException("회원가입 처리에 실패했습니다.");
        }

        emailVerificationService.deleteVerification(request.getEmail());

        // 회원가입은 감사(audit) 목적상 항상 기록 — userId로 이후 활동 추적 가능
        log.info("회원가입 완료 — userId={}", member.getUserId());
        return new SignupResponse(member.getUserId(), member.getName());
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberMapper.findByUserId(request.getUserId());

        if (member == null ||
                !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            // WARN: 로그인 실패는 보안 이벤트 — 반복 실패 패턴 감지용
            log.warn("로그인 실패 — userId={}", request.getUserId());
            throw new LoginFailedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 로그인 성공도 감사 목적 기록
        log.info("로그인 성공 — userId={} role={}", member.getUserId(), member.getRole());
        return new LoginResponse(
                member.getUserId(),
                member.getName(),
                member.getRole(),
                member.getSubscribe());
    }

    public CheckIdResponse checkUserId(String userId) {
        boolean available = memberMapper.countByUserId(userId) == 0;
        return new CheckIdResponse(available);
    }
}