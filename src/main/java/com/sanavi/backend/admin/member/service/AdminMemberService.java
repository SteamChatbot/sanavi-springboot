package com.sanavi.backend.admin.member.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.admin.member.dto.AdminMemberActionHistory;
import com.sanavi.backend.admin.member.dto.AdminMemberListRequestDto;
import com.sanavi.backend.admin.member.dto.AdminMemberPageResponseDto;
import com.sanavi.backend.admin.member.dto.AdminMemberResponseDto;
import com.sanavi.backend.admin.member.dto.AdminMemberSubscriptionRequestDto;
import com.sanavi.backend.admin.member.mapper.AdminMemberMapper;
import com.sanavi.backend.admin.role.service.AdminPermissionGuard;
import com.sanavi.backend.admin.role.service.AdminRolePolicy;
import com.sanavi.backend.member.dto.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 관리자 회원상태관리 서비스
// 회원 목록 조회, 구독 변경, AI 횟수 초기화, 강제 로그아웃을 담당한다
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final AdminMemberMapper adminMemberMapper;
    private final AdminPermissionGuard adminPermissionGuard;

    @Transactional
    public AdminMemberPageResponseDto getMembers(AdminMemberListRequestDto request, String adminUserId) {
        // 만료된 로그인 제한은 목록 조회 시점에 정리해 현재 상태가 정확히 보이도록 한다
        int releasedCount = adminMemberMapper.releaseExpiredLoginRestrictions();

        adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.MEMBER_READ);
        if (releasedCount > 0) {
            log.info(
                    "action=ADMIN_MEMBER_EXPIRED_RESTRICTION_RELEASE count={} result=SUCCESS",
                    releasedCount);
        }

        String keyword = normalizeKeyword(request.getKeyword());
        String role = normalizeRole(request.getRole());
        String status = normalizeStatus(request.getStatus());
        Integer subscribe = request.getSubscribe();

        int page = Math.max(request.getPage(), 1);
        int size = Math.max(request.getSafeSize(), 1);

        int totalCount = adminMemberMapper.countMembers(
                keyword,
                role,
                subscribe,
                status);

        var members = adminMemberMapper.selectMembers(
                keyword,
                role,
                subscribe,
                status,
                request.getOffset(),
                size);

        return new AdminMemberPageResponseDto(
                members,
                page,
                size,
                totalCount);
    }

    @Transactional
    public void changeSubscription(
            String targetUserId,
            String adminUserId,
            AdminMemberSubscriptionRequestDto request) {
        Member admin = adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.SUBSCRIPTION_MANAGE);
        AdminMemberResponseDto target = requireTargetMember(targetUserId);

        if (request == null || request.getSubscribe() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "구독 상태를 선택해 주세요.");
        }

        if (request.getSubscribe() != 0 && request.getSubscribe() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "구독 상태는 Basic 또는 Pro만 가능합니다.");
        }

        if (target.getDeleted() == null || target.getDeleted() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "탈퇴 처리된 회원은 구독 상태를 변경할 수 없습니다.");
        }

        int updated = adminMemberMapper.updateSubscribe(
                targetUserId,
                request.getSubscribe());

        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "구독 상태 변경에 실패했습니다.");
        }

        insertHistory(
                targetUserId,
                admin.getUserId(),
                "SUBSCRIPTION_CHANGE",
                "관리자 구독 상태 변경",
                "subscribe=" + target.getSubscribe(),
                "subscribe=" + request.getSubscribe());

        log.info(
                "action=ADMIN_MEMBER_SUBSCRIPTION_CHANGE target_user_id={} admin_user_id={} before={} after={} result=SUCCESS",
                targetUserId,
                admin.getUserId(),
                target.getSubscribe(),
                request.getSubscribe());
    }

    @Transactional
    public void resetAiCount(String targetUserId, String adminUserId) {
        Member admin = adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.MEMBER_STATUS_MANAGE);
        AdminMemberResponseDto target = requireTargetMember(targetUserId);

        if (target.getDeleted() == null || target.getDeleted() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "탈퇴 처리된 회원은 AI 횟수를 초기화할 수 없습니다.");
        }

        int updated = adminMemberMapper.resetAiCount(targetUserId);

        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 횟수 초기화에 실패했습니다.");
        }

        insertHistory(
                targetUserId,
                admin.getUserId(),
                "AI_COUNT_RESET",
                "관리자 AI 횟수 초기화",
                "aiCount=" + target.getAiCount(),
                "aiCount=0");

        log.info(
                "action=ADMIN_MEMBER_AI_COUNT_RESET target_user_id={} admin_user_id={} before={} result=SUCCESS",
                targetUserId,
                admin.getUserId(),
                target.getAiCount());
    }

    private AdminMemberResponseDto requireTargetMember(String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대상 회원 아이디가 필요합니다.");
        }

        AdminMemberResponseDto target = adminMemberMapper.selectMemberByUserId(targetUserId);

        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 회원을 찾을 수 없습니다.");
        }

        return target;
    }

    private void insertHistory(
            String targetUserId,
            String adminUserId,
            String actionType,
            String reason,
            String beforeValue,
            String afterValue) {
        AdminMemberActionHistory history = new AdminMemberActionHistory();
        history.setTargetUserId(targetUserId);
        history.setAdminUserId(adminUserId);
        history.setReportId(null);
        history.setActionType(actionType);
        history.setActionReason(reason);
        history.setBeforeValue(beforeValue);
        history.setAfterValue(afterValue);

        adminMemberMapper.insertAdminActionHistory(history);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) {
            return null;
        }

        return role.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }

        return status.trim().toUpperCase();
    }
}