package com.sanavi.backend.member.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.admin.mail.dto.MailAudienceFilter;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.member.dto.MemberResponseDto;
import com.sanavi.backend.member.dto.MemberUpdateRequestDto;

@Mapper
public interface MemberMapper {
    int insertMember(Member member);

    int insertMemberLawyer(Member member);

    Member findByUserId(String userId);

    int countByUserId(String userId);

    int countByEmail(String email);

    MemberResponseDto findMemberInfoByUserId(String userId);

    int updateMemberInfo(
            @Param("userId") String userId,
            @Param("request") MemberUpdateRequestDto request);

    void incrementAiCount(String userId);

    // 관리자 대량 메일 — 조건에 맞는 대상 인원수/목록 (admin/mail 모듈 전용)
    int countMailTargets(MailAudienceFilter filter);

    List<Member> searchMailTargets(MailAudienceFilter filter);

    List<String> findDistinctJobs();
}
