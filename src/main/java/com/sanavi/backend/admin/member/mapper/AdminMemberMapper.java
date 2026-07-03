package com.sanavi.backend.admin.member.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.admin.member.dto.AdminMemberActionHistory;
import com.sanavi.backend.admin.member.dto.AdminMemberResponseDto;

@Mapper
public interface AdminMemberMapper {

    List<AdminMemberResponseDto> selectMembers(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("subscribe") Integer subscribe,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("size") int size);

    int countMembers(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("subscribe") Integer subscribe,
            @Param("status") String status);

    AdminMemberResponseDto selectMemberByUserId(@Param("userId") String userId);

    int releaseExpiredLoginRestrictions();

    int updateSubscribe(
            @Param("userId") String userId,
            @Param("subscribe") Integer subscribe);

    int resetAiCount(@Param("userId") String userId);

    int insertAdminActionHistory(AdminMemberActionHistory history);
}