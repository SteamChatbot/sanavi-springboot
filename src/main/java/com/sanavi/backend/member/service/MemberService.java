package com.sanavi.backend.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanavi.backend.common.exception.MemberNotFoundException;
import com.sanavi.backend.member.dto.MemberResponseDto;
import com.sanavi.backend.member.dto.MemberUpdateRequestDto;
import com.sanavi.backend.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;

    // 회원 정보 조회는 빈번하게 호출될 수 있으므로 성공 로그는 남기지 않음
    @Transactional(readOnly = true)
    public MemberResponseDto getMemberInfo(String userId) {
        MemberResponseDto member = memberMapper.findMemberInfoByUserId(userId);

        if (member == null) {
            log.warn(
                    "action=MEMBER_INFO_READ target_type=member target_user_id={} result=FAIL reason=MEMBER_NOT_FOUND",
                    userId);

            throw new MemberNotFoundException("회원 정보를 찾을 수 없습니다.");
        }

        return member;
    }

    // 회원 정보 수정은 개인정보 변경 이벤트이므로 감사 로그를 남김
    @Transactional
    public MemberResponseDto updateMemberInfo(
            String userId,
            MemberUpdateRequestDto request) {
        MemberResponseDto existingMember = memberMapper.findMemberInfoByUserId(userId);

        if (existingMember == null) {
            log.warn(
                    "action=MEMBER_INFO_UPDATE target_type=member target_user_id={} result=FAIL reason=MEMBER_NOT_FOUND",
                    userId);

            throw new MemberNotFoundException("회원 정보를 찾을 수 없습니다.");
        }

        int result = memberMapper.updateMemberInfo(userId, request);

        if (result != 1) {
            log.error(
                    "action=MEMBER_INFO_UPDATE target_type=member target_user_id={} result=FAIL reason=UPDATE_FAILED",
                    userId);

            throw new IllegalStateException("회원 정보 수정에 실패했습니다.");
        }

        log.info(
                "action=MEMBER_INFO_UPDATE target_type=member target_user_id={} result=SUCCESS",
                userId);

        return memberMapper.findMemberInfoByUserId(userId);
    }
}