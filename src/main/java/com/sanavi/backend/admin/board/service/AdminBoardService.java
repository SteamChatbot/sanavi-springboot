package com.sanavi.backend.admin.board.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.admin.board.dto.AdminBoardActionHistory;
import com.sanavi.backend.admin.board.dto.AdminBoardCommentResponseDto;
import com.sanavi.backend.admin.board.dto.AdminBoardListRequestDto;
import com.sanavi.backend.admin.board.dto.AdminBoardPageResponseDto;
import com.sanavi.backend.admin.board.dto.AdminBoardPostResponseDto;
import com.sanavi.backend.admin.board.dto.AdminBoardTargetInfo;
import com.sanavi.backend.admin.board.mapper.AdminBoardMapper;
import com.sanavi.backend.admin.role.service.AdminPermissionGuard;
import com.sanavi.backend.admin.role.service.AdminRolePolicy;
import com.sanavi.backend.member.dto.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 관리자 게시판관리 서비스
// 게시글/댓글 목록 조회, 삭제, 복구를 담당한다
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBoardService {

    private static final String ACTION_BOARD_POST_DELETE = "BOARD_POST_DELETE";
    private static final String ACTION_BOARD_POST_RESTORE = "BOARD_POST_RESTORE";
    private static final String ACTION_BOARD_COMMENT_DELETE = "BOARD_COMMENT_DELETE";
    private static final String ACTION_BOARD_COMMENT_RESTORE = "BOARD_COMMENT_RESTORE";

    private final AdminBoardMapper adminBoardMapper;
    private final AdminPermissionGuard adminPermissionGuard;

    @Transactional(readOnly = true)
    public AdminBoardPageResponseDto<AdminBoardPostResponseDto> getPosts(
            AdminBoardListRequestDto request,
            String adminUserId) {

        adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.BOARD_MANAGE);

        request.normalize();

        int total = adminBoardMapper.countPosts(request);
        List<AdminBoardPostResponseDto> contents = total == 0
                ? List.of()
                : adminBoardMapper.selectPosts(request);

        log.info(
                "action=ADMIN_BOARD_POST_LIST admin_user_id={} page={} size={} status={} reported_only={} keyword={} total={} result=SUCCESS",
                adminUserId,
                request.getPage(),
                request.getSize(),
                request.getStatus(),
                request.getReportedOnly(),
                request.getKeyword(),
                total);

        return createPageResponse(contents, request, total);
    }

    @Transactional(readOnly = true)
    public AdminBoardPageResponseDto<AdminBoardCommentResponseDto> getComments(
            AdminBoardListRequestDto request,
            String adminUserId) {

        adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.BOARD_MANAGE);

        request.normalize();

        int total = adminBoardMapper.countComments(request);
        List<AdminBoardCommentResponseDto> contents = total == 0
                ? List.of()
                : adminBoardMapper.selectComments(request);

        log.info(
                "action=ADMIN_BOARD_COMMENT_LIST admin_user_id={} page={} size={} status={} reported_only={} keyword={} total={} result=SUCCESS",
                adminUserId,
                request.getPage(),
                request.getSize(),
                request.getStatus(),
                request.getReportedOnly(),
                request.getKeyword(),
                total);

        return createPageResponse(contents, request, total);
    }

    @Transactional
    public void deletePost(int boardId, String adminUserId) {
        Member admin = requireBoardManager(adminUserId);

        AdminBoardTargetInfo target = findPostTarget(boardId);

        if (Integer.valueOf(0).equals(target.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 삭제된 게시글입니다.");
        }

        int updated = adminBoardMapper.softDeletePost(boardId);
        validateUpdated(updated, "게시글 삭제에 실패했습니다.");

        saveActionHistory(
                admin.getUserId(),
                target.getOwnerUserId(),
                ACTION_BOARD_POST_DELETE,
                "boardId=" + boardId + ", title=" + safeText(target.getTargetTitle()));

        log.info(
                "action=BOARD_POST_DELETE admin_user_id={} target_board_id={} target_user_id={} result=SUCCESS",
                admin.getUserId(),
                boardId,
                target.getOwnerUserId());
    }

    @Transactional
    public void restorePost(int boardId, String adminUserId) {
        Member admin = requireBoardManager(adminUserId);

        AdminBoardTargetInfo target = findPostTarget(boardId);

        if (Integer.valueOf(1).equals(target.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 정상 상태인 게시글입니다.");
        }

        int updated = adminBoardMapper.restorePost(boardId);
        validateUpdated(updated, "게시글 복구에 실패했습니다.");

        saveActionHistory(
                admin.getUserId(),
                target.getOwnerUserId(),
                ACTION_BOARD_POST_RESTORE,
                "boardId=" + boardId + ", title=" + safeText(target.getTargetTitle()));

        log.info(
                "action=BOARD_POST_RESTORE admin_user_id={} target_board_id={} target_user_id={} result=SUCCESS",
                admin.getUserId(),
                boardId,
                target.getOwnerUserId());
    }

    @Transactional
    public void deleteComment(int commentId, String adminUserId) {
        Member admin = requireBoardManager(adminUserId);

        AdminBoardTargetInfo target = findCommentTarget(commentId);

        if (Integer.valueOf(0).equals(target.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 삭제된 댓글입니다.");
        }

        int updated = adminBoardMapper.softDeleteComment(commentId);
        validateUpdated(updated, "댓글 삭제에 실패했습니다.");

        saveActionHistory(
                admin.getUserId(),
                target.getOwnerUserId(),
                ACTION_BOARD_COMMENT_DELETE,
                "commentId=" + commentId + ", content=" + safeText(target.getTargetTitle()));

        log.info(
                "action=BOARD_COMMENT_DELETE admin_user_id={} target_comment_id={} target_user_id={} result=SUCCESS",
                admin.getUserId(),
                commentId,
                target.getOwnerUserId());
    }

    @Transactional
    public void restoreComment(int commentId, String adminUserId) {
        Member admin = requireBoardManager(adminUserId);

        AdminBoardTargetInfo target = findCommentTarget(commentId);

        if (Integer.valueOf(1).equals(target.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 정상 상태인 댓글입니다.");
        }

        int updated = adminBoardMapper.restoreComment(commentId);
        validateUpdated(updated, "댓글 복구에 실패했습니다.");

        saveActionHistory(
                admin.getUserId(),
                target.getOwnerUserId(),
                ACTION_BOARD_COMMENT_RESTORE,
                "commentId=" + commentId + ", content=" + safeText(target.getTargetTitle()));

        log.info(
                "action=BOARD_COMMENT_RESTORE admin_user_id={} target_comment_id={} target_user_id={} result=SUCCESS",
                admin.getUserId(),
                commentId,
                target.getOwnerUserId());
    }

    private Member requireBoardManager(String adminUserId) {
        return adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.BOARD_MANAGE);
    }

    private AdminBoardTargetInfo findPostTarget(int boardId) {
        AdminBoardTargetInfo target = adminBoardMapper.selectPostTarget(boardId);

        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return target;
    }

    private AdminBoardTargetInfo findCommentTarget(int commentId) {
        AdminBoardTargetInfo target = adminBoardMapper.selectCommentTarget(commentId);

        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }

        return target;
    }

    private void validateUpdated(int updated, String message) {
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    private <T> AdminBoardPageResponseDto<T> createPageResponse(
            List<T> contents,
            AdminBoardListRequestDto request,
            int total) {

        int totalPages = total == 0
                ? 0
                : (int) Math.ceil((double) total / request.getSize());

        return new AdminBoardPageResponseDto<>(
                contents,
                request.getPage(),
                request.getSize(),
                total,
                totalPages);
    }

    private void saveActionHistory(
            String adminUserId,
            String targetUserId,
            String actionType,
            String reason) {

        try {
            adminBoardMapper.insertActionHistory(
                    new AdminBoardActionHistory(
                            adminUserId,
                            targetUserId,
                            actionType,
                            reason));
        } catch (Exception e) {
            // 이력 저장 실패가 관리자 조치 자체를 막지 않도록 로그만 남긴다
            log.warn(
                    "action=ADMIN_ACTION_HISTORY_SAVE action_type={} admin_user_id={} target_user_id={} result=FAIL reason={}",
                    actionType,
                    adminUserId,
                    targetUserId,
                    e.getMessage());
        }
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();

        if (trimmed.length() <= 80) {
            return trimmed;
        }

        return trimmed.substring(0, 80);
    }
}