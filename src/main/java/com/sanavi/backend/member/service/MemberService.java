package com.sanavi.backend.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanavi.backend.common.exception.MemberNotFoundException;
import com.sanavi.backend.member.dto.MemberResponseDto;
import com.sanavi.backend.member.dto.MemberUpdateRequestDto;
import com.sanavi.backend.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;

    @Transactional(readOnly = true)
    public MemberResponseDto getMemberInfo(String userId) {
        MemberResponseDto member = memberMapper.findMemberInfoByUserId(userId);

        if (member == null) {
            throw new MemberNotFoundException("회원 정보를 찾을 수 없습니다.");
        }

        return member;
    }

    @Transactional
    public MemberResponseDto updateMemberInfo(
            String userId,
            MemberUpdateRequestDto request) {
        MemberResponseDto existingMember = memberMapper.findMemberInfoByUserId(userId);

        if (existingMember == null) {
            throw new MemberNotFoundException("회원 정보를 찾을 수 없습니다.");
        }

        int result = memberMapper.updateMemberInfo(userId, request);

        if (result != 1) {
            throw new IllegalStateException("회원 정보 수정에 실패했습니다.");
        }

        return memberMapper.findMemberInfoByUserId(userId);
    }
}