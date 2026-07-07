package com.sanavi.backend.admin.role.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.admin.role.dto.AdminRoleActionHistory;
import com.sanavi.backend.admin.role.dto.AdminRoleAssignmentResponseDto;
import com.sanavi.backend.admin.role.dto.AdminRoleCandidateResponseDto;

@Mapper
public interface AdminRoleMapper {

    List<AdminRoleAssignmentResponseDto> selectAdminAssignments(
            @Param("keyword") String keyword);

    AdminRoleAssignmentResponseDto selectAdminAssignmentByUserId(
            @Param("userId") String userId);

    String selectActiveAdminRoleType(@Param("userId") String userId);

    int countActiveSuperAdmins();

    List<AdminRoleCandidateResponseDto> selectAdminCandidates(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    int countAdminCandidates(@Param("keyword") String keyword);

    int updateMemberRole(
            @Param("userId") String userId,
            @Param("role") String role);

    int upsertAdminRoleAssignment(
            @Param("userId") String userId,
            @Param("adminRoleType") String adminRoleType,
            @Param("adminUserId") String adminUserId);

    int deactivateAdminRoleAssignment(
            @Param("userId") String userId,
            @Param("adminUserId") String adminUserId);

    int insertAdminActionHistory(AdminRoleActionHistory history);
}