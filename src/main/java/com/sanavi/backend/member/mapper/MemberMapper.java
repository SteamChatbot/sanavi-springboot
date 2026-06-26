package com.sanavi.backend.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

}
