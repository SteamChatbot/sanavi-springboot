package com.sanavi.backend.admin.role.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sanavi.backend.admin.role.dto.AdminPermissionResponseDto;

// 관리자 역할별 고정 권한 정책
// 권한 템플릿은 DB가 아니라 코드에서 관리해 임의 변경 위험을 줄인다
public final class AdminRolePolicy {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String OPERATIONS_ADMIN = "OPERATIONS_ADMIN";
    public static final String SUPPORT_ADMIN = "SUPPORT_ADMIN";
    public static final String MEMBER_READ = "MEMBER_READ";
    public static final String MEMBER_STATUS_MANAGE = "MEMBER_STATUS_MANAGE";
    public static final String SUBSCRIPTION_MANAGE = "SUBSCRIPTION_MANAGE";

    public static final String REPORT_READ = "REPORT_READ";
    public static final String REPORT_PROCESS = "REPORT_PROCESS";
    public static final String REPORT_CONTENT_HIDE = "REPORT_CONTENT_HIDE";

    public static final String ADMIN_ROLE_MANAGE = "ADMIN_ROLE_MANAGE";

    public static final String BOARD_MANAGE = "BOARD_MANAGE";
    public static final String MATCH_BOARD_MANAGE = "MATCH_BOARD_MANAGE";

    public static final String AI_ANALYSIS_MANAGE = "AI_ANALYSIS_MANAGE";
    public static final String STATISTICS_READ = "STATISTICS_READ";

    public static final String MAIL_SEND = "MAIL_SEND";
    public static final String SYSTEM_MONITOR = "SYSTEM_MONITOR";

    public static final String ADMIN_ACTION_LOG_READ = "ADMIN_ACTION_LOG_READ";

    private static final List<AdminPermissionResponseDto.PermissionInfo> PERMISSIONS = List.of(
            new AdminPermissionResponseDto.PermissionInfo("MEMBER_READ", "회원조회", "회원 정보를 조회합니다."),
            new AdminPermissionResponseDto.PermissionInfo("MEMBER_STATUS_MANAGE", "회원상태관리", "회원 상태와 AI 사용횟수를 관리합니다."),
            new AdminPermissionResponseDto.PermissionInfo("SUBSCRIPTION_MANAGE", "구독관리", "회원 구독 상태를 변경합니다."),
            new AdminPermissionResponseDto.PermissionInfo("REPORT_READ", "신고조회", "신고 내역을 조회합니다."),
            new AdminPermissionResponseDto.PermissionInfo("REPORT_PROCESS", "신고처리", "신고를 로그인 제한, 강제탈퇴, 반려 처리합니다."),
            new AdminPermissionResponseDto.PermissionInfo("REPORT_CONTENT_HIDE", "신고 원문 숨김", "신고 건에 연결된 원문을 숨김 처리합니다."),
            new AdminPermissionResponseDto.PermissionInfo("ADMIN_ROLE_MANAGE", "권한관리", "관리자 역할을 부여하거나 해제합니다."),
            new AdminPermissionResponseDto.PermissionInfo("BOARD_MANAGE", "게시판관리", "게시글을 숨김, 복구, 삭제 처리합니다."),
            new AdminPermissionResponseDto.PermissionInfo("MATCH_BOARD_MANAGE", "의뢰글게시판관리", "의뢰글 게시판을 관리합니다."),
            new AdminPermissionResponseDto.PermissionInfo("AI_ANALYSIS_MANAGE", "AI 분석 관리", "AI 분석 관련 관리자 기능을 사용합니다."),
            new AdminPermissionResponseDto.PermissionInfo("STATISTICS_READ", "통계조회", "관리자 통계 화면을 조회합니다."),
            new AdminPermissionResponseDto.PermissionInfo("MAIL_SEND", "메일발송", "고객 대상 안내 메일을 발송합니다."),
            new AdminPermissionResponseDto.PermissionInfo("SYSTEM_MONITOR", "시스템 모니터링", "시스템 상태를 확인합니다."),
            new AdminPermissionResponseDto.PermissionInfo("ADMIN_ACTION_LOG_READ", "관리자활동로그조회", "관리자 조치 이력을 조회합니다."));

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            SUPER_ADMIN,
            PERMISSIONS.stream().map(AdminPermissionResponseDto.PermissionInfo::getCode).toList(),

            OPERATIONS_ADMIN,
            List.of(
                    "MEMBER_READ",
                    "MEMBER_STATUS_MANAGE",
                    "SUBSCRIPTION_MANAGE",
                    "REPORT_READ",
                    "REPORT_PROCESS",
                    "REPORT_CONTENT_HIDE",
                    "BOARD_MANAGE",
                    "MATCH_BOARD_MANAGE",
                    "AI_ANALYSIS_MANAGE",
                    "STATISTICS_READ",
                    "MAIL_SEND",
                    "ADMIN_ACTION_LOG_READ"),

            SUPPORT_ADMIN,
            List.of(
                    "MEMBER_READ",
                    "REPORT_READ",
                    "REPORT_PROCESS",
                    "REPORT_CONTENT_HIDE",
                    "AI_ANALYSIS_MANAGE",
                    "STATISTICS_READ",
                    "MAIL_SEND"));

    private AdminRolePolicy() {
    }

    public static boolean isValidRoleType(String roleType) {
        return Set.of(SUPER_ADMIN, OPERATIONS_ADMIN, SUPPORT_ADMIN).contains(roleType);
    }

    public static String getRoleLabel(String roleType) {
        return switch (roleType) {
            case SUPER_ADMIN -> "최고관리자";
            case OPERATIONS_ADMIN -> "운영관리자";
            case SUPPORT_ADMIN -> "고객지원관리자";
            default -> "미지정";
        };
    }

    public static AdminPermissionResponseDto getPermissions() {
        List<AdminPermissionResponseDto.RoleTemplate> roles = List.of(
                new AdminPermissionResponseDto.RoleTemplate(
                        SUPER_ADMIN,
                        "최고관리자",
                        "모든 관리자 기능과 권한관리 기능을 사용할 수 있습니다.",
                        ROLE_PERMISSIONS.get(SUPER_ADMIN)),

                new AdminPermissionResponseDto.RoleTemplate(
                        OPERATIONS_ADMIN,
                        "운영관리자",
                        "서비스 운영에 필요한 회원, 신고, 게시판, 통계 기능을 사용할 수 있습니다.",
                        ROLE_PERMISSIONS.get(OPERATIONS_ADMIN)),

                new AdminPermissionResponseDto.RoleTemplate(
                        SUPPORT_ADMIN,
                        "고객지원관리자",
                        "고객 응대에 필요한 회원조회, 신고처리, 메일발송 기능을 사용할 수 있습니다.",
                        ROLE_PERMISSIONS.get(SUPPORT_ADMIN)));

        return new AdminPermissionResponseDto(roles, PERMISSIONS);
    }

    public static List<String> getPermissionCodes(String roleType) {
        return ROLE_PERMISSIONS.getOrDefault(roleType, List.of());
    }

    public static boolean hasPermission(String roleType, String permissionCode) {
        if (roleType == null || permissionCode == null) {
            return false;
        }

        return getPermissionCodes(roleType).contains(permissionCode);
    }

}