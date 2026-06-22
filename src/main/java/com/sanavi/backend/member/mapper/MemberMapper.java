package com.sanavi.backend.member.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.sanavi.backend.member.dto.Member;

@Mapper
public interface MemberMapper {
    int insertMember(Member member);

    Member findByUserId(String userId);

    int countByUserId(String userId);

    int countByEmail(String email);

    void incrementAiCount(String userId);
}
