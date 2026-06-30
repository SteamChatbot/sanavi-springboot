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
import com.sanavi.backend.common.exception.MemberNotFoundException;
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
            log.warn(
                    "action=MEMBER_SIGNUP result=DENIED reason=DUPLICATE_USER_ID target_user_id={}",
                    request.getUserId());
            throw new DuplicateMemberException("이미 사용 중인 아이디입니다.");
        }

        if (memberMapper.countByEmail(request.getEmail()) > 0) {
            log.warn(
                    "action=MEMBER_SIGNUP result=DENIED reason=DUPLICATE_EMAIL target_user_id={}",
                    request.getUserId());
            throw new DuplicateMemberException("이미 사용 중인 이메일입니다.");
        }

        if (!emailVerificationService.isVerified(request.getEmail())) {
            log.warn(
                    "action=MEMBER_SIGNUP result=DENIED reason=EMAIL_NOT_VERIFIED target_user_id={}",
                    request.getUserId());
            throw new EmailVerificationException(
                    "이메일 인증이 완료되지 않았습니다.");
        }

        boolean isLawyer = "role_lawyer".equals(request.getRole());

        if (isLawyer) {
            validateLawyerSignup(request);
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

        // 클라이언트가 role_admin 같은 값을 보내도 저장되지 않도록 서버에서 고정
        member.setRole(isLawyer ? "role_lawyer" : "role_user");

        if (isLawyer) {
            member.setFirmName(request.getFirmName().trim());

            member.setSido(request.getSido());
            member.setSigungu(request.getSigungu());
            member.setRegionDetail(request.getRegionDetail());

            member.setExperienceYears(request.getExperienceYears());

            member.setSpecialty(
                    request.getSpecialty() == null || request.getSpecialty().isBlank()
                            ? "산재"
                            : request.getSpecialty().trim());
        }

        int result = memberMapper.insertMember(member);

        if (result != 1) {
            log.error(
                    "action=MEMBER_SIGNUP target_user_id={} role={} result=FAIL reason=MEMBER_INSERT_FAILED",
                    member.getUserId(),
                    member.getRole());

            throw new IllegalStateException("회원가입 처리에 실패했습니다.");
        }

        if (isLawyer) {
            int lawyerResult = memberMapper.insertMemberLawyer(member);

            if (lawyerResult != 1) {
                log.error(
                        "action=MEMBER_SIGNUP target_user_id={} role={} result=FAIL reason=LAWYER_INFO_INSERT_FAILED",
                        member.getUserId(),
                        member.getRole());

                throw new IllegalStateException("변호사 정보 등록에 실패했습니다.");
            }
        }

        emailVerificationService.deleteVerification(request.getEmail());

        // 회원가입은 감사(audit) 목적상 항상 기록 — userId로 이후 활동 추적 가능
        log.info(
                "action=MEMBER_SIGNUP target_user_id={} role={} result=SUCCESS",
                member.getUserId(),
                member.getRole());
        return new SignupResponse(member.getUserId(), member.getName());
    }

    private void validateLawyerSignup(SignupRequest request) {
        if (request.getFirmName() == null || request.getFirmName().isBlank()) {
            log.warn(
                    "action=MEMBER_SIGNUP result=DENIED reason=MISSING_FIRM_NAME target_user_id={}",
                    request.getUserId());

            throw new IllegalArgumentException("법률사무소명을 입력해 주세요.");
        }

        if (request.getExperienceYears() == null || request.getExperienceYears() < 0) {
            log.warn(
                    "action=MEMBER_SIGNUP result=DENIED reason=INVALID_EXPERIENCE_YEARS target_user_id={}",
                    request.getUserId());

            throw new IllegalArgumentException("경력 연수를 올바르게 입력해 주세요.");
        }
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberMapper.findByUserId(request.getUserId());

        if (member == null ||
                !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            // WARN: 로그인 실패는 보안 이벤트 — 반복 실패 패턴 감지용
            log.warn(
                    "action=MEMBER_LOGIN result=FAIL reason=INVALID_CREDENTIALS target_user_id={}",
                    request.getUserId());
            throw new LoginFailedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 로그인 성공도 감사 목적 기록
        log.info(
                "action=MEMBER_LOGIN target_user_id={} role={} result=SUCCESS",
                member.getUserId(),
                member.getRole());
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

    /** refresh 엔드포인트에서 새 AT 발급 시 최신 role 조회용 */
    public String findRole(String userId) {
        Member member = memberMapper.findByUserId(userId);
        if (member == null)
            throw new MemberNotFoundException("존재하지 않는 회원입니다.");
        return member.getRole();
    }
}