package com.sanavi.backend.admin.role.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.admin.role.dto.AdminPermissionResponseDto;
import com.sanavi.backend.admin.role.dto.AdminRoleActionHistory;
import com.sanavi.backend.admin.role.dto.AdminRoleAssignmentResponseDto;
import com.sanavi.backend.admin.role.dto.AdminRoleCandidatePageResponseDto;
import com.sanavi.backend.admin.role.dto.AdminRoleCandidateRequestDto;
import com.sanavi.backend.admin.role.dto.AdminRoleRevokeRequestDto;
import com.sanavi.backend.admin.role.dto.AdminRoleUpdateRequestDto;
import com.sanavi.backend.admin.role.mapper.AdminRoleMapper;
import com.sanavi.backend.member.dto.Member;
import com.sanavi.backend.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 관리자 권한관리 서비스
// 관리자 역할 조회, 후보 조회, 역할 부여/변경, 관리자 해제를 담당한다
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final AdminRoleMapper adminRoleMapper;
    private final MemberMapper memberMapper;

    @Transactional(readOnly = true)
    public List<AdminRoleAssignmentResponseDto> getAssignments(String keyword, String adminUserId) {
        requireSuperAdmin(adminUserId);

        String normalizedKeyword = normalizeKeyword(keyword);
        List<AdminRoleAssignmentResponseDto> assignments = adminRoleMapper.selectAdminAssignments(normalizedKeyword);

        assignments.forEach(this::applyRoleLabel);

        log.info(
                "action=ADMIN_ROLE_ASSIGNMENT_LIST admin_user_id={} result=SUCCESS count={}",
                adminUserId,
                assignments.size());

        return assignments;
    }

    @Transactional(readOnly = true)
    public AdminRoleCandidatePageResponseDto getCandidates(
            AdminRoleCandidateRequestDto request,
            String adminUserId) {
        requireSuperAdmin(adminUserId);

        String keyword = normalizeKeyword(request.getKeyword());

        int page = Math.max(request.getPage(), 1);
        int size = Math.max(request.getSafeSize(), 1);

        int totalCount = adminRoleMapper.countAdminCandidates(keyword);

        var candidates = adminRoleMapper.selectAdminCandidates(
                keyword,
                request.getOffset(),
                size);

        log.info(
                "action=ADMIN_ROLE_CANDIDATE_LIST admin_user_id={} result=SUCCESS count={}",
                adminUserId,
                candidates.size());

        return new AdminRoleCandidatePageResponseDto(
                candidates,
                page,
                size,
                totalCount);
    }

    @Transactional(readOnly = true)
    public AdminPermissionResponseDto getPermissions(String adminUserId) {
        requireSuperAdmin(adminUserId);

        log.info(
                "action=ADMIN_ROLE_PERMISSION_TEMPLATE admin_user_id={} result=SUCCESS",
                adminUserId);

        return AdminRolePolicy.getPermissions();
    }

    @Transactional
    public void assignOrChangeRole(
            String targetUserId,
            String adminUserId,
            AdminRoleUpdateRequestDto request) {
        Member admin = requireSuperAdmin(adminUserId);
        Member target = requireTargetMember(targetUserId);
        validateUpdateRequest(request);

        String nextRoleType = request.getAdminRoleType().trim().toUpperCase();
        String reason = request.getReason().trim();

        if ("role_lawyer".equals(target.getRole())) {
            log.warn(
                    "action=ADMIN_ROLE_CHANGE result=DENIED reason=LAWYER_ACCOUNT target_user_id={} admin_user_id={}",
                    targetUserId,
                    adminUserId);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "변호사 계정은 관리자 계정으로 승격할 수 없습니다.");
        }

        if (target.getDeleted() == null || target.getDeleted() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "탈퇴 처리된 회원은 관리자로 지정할 수 없습니다.");
        }

        AdminRoleAssignmentResponseDto current = adminRoleMapper.selectAdminAssignmentByUserId(targetUserId);

        String beforeRoleType = current == null || current.getActive() == null || current.getActive() != 1
                ? "NONE"
                : current.getAdminRoleType();

        validateSuperAdminSafetyOnChange(targetUserId, admin.getUserId(), beforeRoleType, nextRoleType);

        int memberUpdated = adminRoleMapper.updateMemberRole(targetUserId, "role_admin");

        if (memberUpdated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "회원 관리자 권한 변경에 실패했습니다.");
        }

        int assignmentUpdated = adminRoleMapper.upsertAdminRoleAssignment(
                targetUserId,
                nextRoleType,
                admin.getUserId());

        if (assignmentUpdated < 1) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "관리자 역할 저장에 실패했습니다.");
        }

        String actionType = "NONE".equals(beforeRoleType)
                ? "ADMIN_ROLE_ASSIGN"
                : "ADMIN_ROLE_CHANGE";

        insertHistory(
                targetUserId,
                admin.getUserId(),
                actionType,
                reason,
                "adminRoleType=" + beforeRoleType,
                "adminRoleType=" + nextRoleType);

        log.info(
                "action={} target_user_id={} admin_user_id={} before={} after={} result=SUCCESS",
                actionType,
                targetUserId,
                admin.getUserId(),
                beforeRoleType,
                nextRoleType);
    }

    @Transactional
    public void revokeRole(
            String targetUserId,
            String adminUserId,
            AdminRoleRevokeRequestDto request) {
        Member admin = requireSuperAdmin(adminUserId);
        validateRevokeRequest(request);

        AdminRoleAssignmentResponseDto current = adminRoleMapper.selectAdminAssignmentByUserId(targetUserId);

        if (current == null || current.getActive() == null || current.getActive() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "활성화된 관리자 역할을 찾을 수 없습니다.");
        }

        if (admin.getUserId().equals(targetUserId)) {
            log.warn(
                    "action=ADMIN_ROLE_REVOKE result=DENIED reason=SELF_REVOKE target_user_id={} admin_user_id={}",
                    targetUserId,
                    admin.getUserId());

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "본인의 관리자 권한은 직접 해제할 수 없습니다.");
        }

        if (AdminRolePolicy.SUPER_ADMIN.equals(current.getAdminRoleType())
                && adminRoleMapper.countActiveSuperAdmins() <= 1) {
            log.warn(
                    "action=ADMIN_ROLE_REVOKE result=DENIED reason=LAST_SUPER_ADMIN target_user_id={} admin_user_id={}",
                    targetUserId,
                    admin.getUserId());

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "최고관리자는 최소 1명 이상 유지되어야 합니다.");
        }

        int assignmentUpdated = adminRoleMapper.deactivateAdminRoleAssignment(
                targetUserId,
                admin.getUserId());

        if (assignmentUpdated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "관리자 역할 해제에 실패했습니다.");
        }

        int memberUpdated = adminRoleMapper.updateMemberRole(targetUserId, "role_user");

        if (memberUpdated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "회원 권한 변경에 실패했습니다.");
        }

        insertHistory(
                targetUserId,
                admin.getUserId(),
                "ADMIN_ROLE_REVOKE",
                request.getReason().trim(),
                "adminRoleType=" + current.getAdminRoleType(),
                "adminRoleType=NONE, memberRole=role_user");

        log.info(
                "action=ADMIN_ROLE_REVOKE target_user_id={} admin_user_id={} before={} result=SUCCESS",
                targetUserId,
                admin.getUserId(),
                current.getAdminRoleType());
    }

    private Member requireSuperAdmin(String adminUserId) {
        if (adminUserId == null || adminUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 로그인이 필요합니다.");
        }

        Member admin = memberMapper.findByUserId(adminUserId);

        if (admin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 정보를 확인할 수 없습니다.");
        }

        if (!"role_admin".equals(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }

        String adminRoleType = adminRoleMapper.selectActiveAdminRoleType(adminUserId);

        if (!AdminRolePolicy.SUPER_ADMIN.equals(adminRoleType)) {
            log.warn(
                    "action=ADMIN_ROLE_ACCESS result=DENIED reason=NOT_SUPER_ADMIN admin_user_id={} admin_role_type={}",
                    adminUserId,
                    adminRoleType);

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "최고관리자 권한이 필요합니다.");
        }

        return admin;
    }

    private Member requireTargetMember(String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대상 회원 아이디가 필요합니다.");
        }

        Member target = memberMapper.findByUserId(targetUserId);

        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 회원을 찾을 수 없습니다.");
        }

        return target;
    }

    private void validateUpdateRequest(AdminRoleUpdateRequestDto request) {
        if (request == null || request.getAdminRoleType() == null || request.getAdminRoleType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "관리자 역할을 선택해 주세요.");
        }

        String roleType = request.getAdminRoleType().trim().toUpperCase();

        if (!AdminRolePolicy.isValidRoleType(roleType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 관리자 역할입니다.");
        }

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경 사유를 입력해 주세요.");
        }

        if (request.getReason().trim().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경 사유는 500자 이하로 입력해 주세요.");
        }
    }

    private void validateRevokeRequest(AdminRoleRevokeRequestDto request) {
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해제 사유를 입력해 주세요.");
        }

        if (request.getReason().trim().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해제 사유는 500자 이하로 입력해 주세요.");
        }
    }

    private void validateSuperAdminSafetyOnChange(
            String targetUserId,
            String adminUserId,
            String beforeRoleType,
            String nextRoleType) {
        if (targetUserId.equals(adminUserId)
                && AdminRolePolicy.SUPER_ADMIN.equals(beforeRoleType)
                && !AdminRolePolicy.SUPER_ADMIN.equals(nextRoleType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "본인의 최고관리자 권한은 직접 낮출 수 없습니다.");
        }

        if (AdminRolePolicy.SUPER_ADMIN.equals(beforeRoleType)
                && !AdminRolePolicy.SUPER_ADMIN.equals(nextRoleType)
                && adminRoleMapper.countActiveSuperAdmins() <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "최고관리자는 최소 1명 이상 유지되어야 합니다.");
        }
    }

    private void insertHistory(
            String targetUserId,
            String adminUserId,
            String actionType,
            String reason,
            String beforeValue,
            String afterValue) {
        AdminRoleActionHistory history = new AdminRoleActionHistory();
        history.setTargetUserId(targetUserId);
        history.setAdminUserId(adminUserId);
        history.setReportId(null);
        history.setActionType(actionType);
        history.setActionReason(reason);
        history.setBeforeValue(beforeValue);
        history.setAfterValue(afterValue);

        adminRoleMapper.insertAdminActionHistory(history);
    }

    private void applyRoleLabel(AdminRoleAssignmentResponseDto assignment) {
        assignment.setAdminRoleLabel(
                AdminRolePolicy.getRoleLabel(assignment.getAdminRoleType()));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }
}