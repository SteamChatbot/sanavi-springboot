package com.sanavi.backend.admin.role.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.admin.role.dto.AdminRoleMeResponseDto;
import com.sanavi.backend.admin.role.mapper.AdminRoleMapper;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 관리자 권한 검증 공통 서비스
// 현재 로그인한 관리자의 세부 역할과 권한을 확인하고, 권한 없는 접근을 차단한다
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPermissionGuard {

        private final MemberMapper memberMapper;
        private final AdminRoleMapper adminRoleMapper;

        public AdminRoleMeResponseDto getMyPermissions(String adminUserId) {
                Member admin = requireAdminMember(adminUserId);
                String adminRoleType = requireActiveAdminRoleType(admin.getUserId());

                var permissionCodes = AdminRolePolicy.getPermissionCodes(adminRoleType);

                log.info(
                                "action=ADMIN_PERMISSION_ME admin_user_id={} admin_role_type={} result=SUCCESS",
                                admin.getUserId(),
                                adminRoleType);

                return new AdminRoleMeResponseDto(
                                adminRoleType,
                                AdminRolePolicy.getRoleLabel(adminRoleType),
                                permissionCodes);
        }

        public Member requirePermission(String adminUserId, String permissionCode) {
                Member admin = requireAdminMember(adminUserId);
                String adminRoleType = requireActiveAdminRoleType(admin.getUserId());

                if (!AdminRolePolicy.hasPermission(adminRoleType, permissionCode)) {
                        log.warn(
                                        "action=ADMIN_PERMISSION_CHECK result=DENIED admin_user_id={} admin_role_type={} required_permission={}",
                                        admin.getUserId(),
                                        adminRoleType,
                                        permissionCode);

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "해당 관리자 기능에 접근할 권한이 없습니다.");
                }

                log.info(
                                "action=ADMIN_PERMISSION_CHECK result=SUCCESS admin_user_id={} admin_role_type={} required_permission={}",
                                admin.getUserId(),
                                adminRoleType,
                                permissionCode);

                return admin;
        }

        public Member requireSuperAdmin(String adminUserId) {
                Member admin = requireAdminMember(adminUserId);
                String adminRoleType = requireActiveAdminRoleType(admin.getUserId());

                if (!AdminRolePolicy.SUPER_ADMIN.equals(adminRoleType)) {
                        log.warn(
                                        "action=ADMIN_SUPER_PERMISSION_CHECK result=DENIED admin_user_id={} admin_role_type={}",
                                        admin.getUserId(),
                                        adminRoleType);

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "최고관리자 권한이 필요합니다.");
                }

                return admin;
        }

        private Member requireAdminMember(String adminUserId) {
                if (adminUserId == null || adminUserId.isBlank()) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "관리자 로그인이 필요합니다.");
                }

                Member admin = memberMapper.findByUserId(adminUserId);

                if (admin == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "관리자 정보를 확인할 수 없습니다.");
                }

                if (!"role_admin".equals(admin.getRole())) {
                        log.warn(
                                        "action=ADMIN_PERMISSION_CHECK result=DENIED reason=NOT_ROLE_ADMIN admin_user_id={} role={}",
                                        admin.getUserId(),
                                        admin.getRole());

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "관리자 권한이 필요합니다.");
                }

                if (admin.getDeleted() == null || admin.getDeleted() != 1) {
                        log.warn(
                                        "action=ADMIN_PERMISSION_CHECK result=DENIED reason=DELETED_ADMIN admin_user_id={}",
                                        admin.getUserId());

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "사용할 수 없는 관리자 계정입니다.");
                }

                return admin;
        }

        private String requireActiveAdminRoleType(String adminUserId) {
                String adminRoleType = adminRoleMapper.selectActiveAdminRoleType(adminUserId);
                if (adminRoleType == null || adminRoleType.isBlank()) {
                        log.warn("action=ADMIN_PERMISSION_CHECK result=DENIED reason=NO_ACTIVE_ADMIN_ROLE admin_user_id={}",
                                        adminUserId);
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "세부 관리자 역할이 지정되지 않았습니다.");
                }
                return adminRoleType;
        }
}