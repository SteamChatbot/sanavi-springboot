package com.sanavi.backend.admin.report.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.admin.report.dto.AdminReportResponseDto;
import com.sanavi.backend.admin.report.dto.MemberAdminActionHistory;

@Mapper
public interface AdminReportMapper {

    List<AdminReportResponseDto> selectReports(
            @Param("status") String status,
            @Param("targetType") String targetType,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    int countReports(
            @Param("status") String status,
            @Param("targetType") String targetType,
            @Param("keyword") String keyword);

    AdminReportResponseDto selectReportById(@Param("reportId") Integer reportId);

    int updateReportLoginRestriction(
            @Param("reportId") Integer reportId,
            @Param("adminUserId") String adminUserId,
            @Param("reason") String reason,
            @Param("days") Integer days);

    int updateReportWithdraw(
            @Param("reportId") Integer reportId,
            @Param("adminUserId") String adminUserId,
            @Param("reason") String reason);

    int updateReportDismiss(
            @Param("reportId") Integer reportId,
            @Param("adminUserId") String adminUserId,
            @Param("reason") String reason);

    int updateMemberLoginRestriction(
            @Param("targetUserId") String targetUserId,
            @Param("adminUserId") String adminUserId,
            @Param("reason") String reason,
            @Param("days") Integer days,
            @Param("restrictedUntil") LocalDateTime restrictedUntil);

    int forceWithdrawMember(@Param("targetUserId") String targetUserId);

    int insertAdminActionHistory(MemberAdminActionHistory history);
}