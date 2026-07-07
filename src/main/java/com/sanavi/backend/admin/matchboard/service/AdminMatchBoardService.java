package com.sanavi.backend.admin.matchboard.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.admin.matchboard.dto.AdminMatchBidResponseDto;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchBoardActionHistory;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchBoardListRequestDto;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchBoardPageResponseDto;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchBoardTargetInfo;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchPostResponseDto;
import com.sanavi.backend.admin.matchboard.mapper.AdminMatchBoardMapper;
import com.sanavi.backend.admin.role.service.AdminPermissionGuard;
import com.sanavi.backend.admin.role.service.AdminRolePolicy;
import com.sanavi.backend.member.dto.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 관리자 의뢰글 게시판관리 서비스
// 의뢰글/입찰 목록 조회, 강제마감, 삭제, 복구를 담당한다
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMatchBoardService {

    private static final String ACTION_MATCH_POST_CLOSE = "MATCH_POST_CLOSE";
    private static final String ACTION_MATCH_POST_DELETE = "MATCH_POST_DELETE";
    private static final String ACTION_MATCH_POST_RESTORE = "MATCH_POST_RESTORE";
    private static final String ACTION_MATCH_BID_DELETE = "MATCH_BID_DELETE";
    private static final String ACTION_MATCH_BID_RESTORE = "MATCH_BID_RESTORE";

    private final AdminMatchBoardMapper adminMatchBoardMapper;
    private final AdminPermissionGuard adminPermissionGuard;

    @Transactional(readOnly = true)
    public AdminMatchBoardPageResponseDto<AdminMatchPostResponseDto> getPosts(
            AdminMatchBoardListRequestDto request,
            String adminUserId) {

        adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.MATCH_BOARD_MANAGE);

        request.normalize();

        int total = adminMatchBoardMapper.countPosts(request);
        List<AdminMatchPostResponseDto> contents = total == 0
                ? List.of()
                : adminMatchBoardMapper.selectPosts(request);

        log.info(
                "action=ADMIN_MATCH_POST_LIST admin_user_id={} page={} size={} status={} deleted_status={} reported_only={} keyword={} total={} result=SUCCESS",
                adminUserId,
                request.getPage(),
                request.getSize(),
                request.getStatus(),
                request.getDeletedStatus(),
                request.getReportedOnly(),
                request.getKeyword(),
                total);

        return createPageResponse(contents, request, total);
    }

    @Transactional(readOnly = true)
    public AdminMatchBoardPageResponseDto<AdminMatchBidResponseDto> getBids(
            AdminMatchBoardListRequestDto request,
            String adminUserId) {

        adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.MATCH_BOARD_MANAGE);

        request.normalize();

        int total = adminMatchBoardMapper.countBids(request);
        List<AdminMatchBidResponseDto> contents = total == 0
                ? List.of()
                : adminMatchBoardMapper.selectBids(request);

        log.info(
                "action=ADMIN_MATCH_BID_LIST admin_user_id={} page={} size={} status={} deleted_status={} reported_only={} keyword={} total={} result=SUCCESS",
                adminUserId,
                request.getPage(),
                request.getSize(),
                request.getStatus(),
                request.getDeletedStatus(),
                request.getReportedOnly(),
                request.getKeyword(),
                total);

        return createPageResponse(contents, request, total);
    }

    @Transactional
    public void closePost(int matchId, String adminUserId) {
        Member admin = requireMatchBoardManager(adminUserId);

        AdminMatchBoardTargetInfo target = findPostTarget(matchId);

        if (Integer.valueOf(0).equals(target.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "삭제된 의뢰글은 강제마감할 수 없습니다.");
        }

        if ("CLOSED".equalsIgnoreCase(target.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 마감된 의뢰글입니다.");
        }

        if (isCancelled(target.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "취소된 의뢰글은 강제마감할 수 없습니다.");
        }

        int updated = adminMatchBoardMapper.closePost(matchId);
        validateUpdated(updated, "의뢰글 강제마감에 실패했습니다.");

        saveActionHistory(
                admin.getUserId(),
                target.getOwnerUserId(),
                ACTION_MATCH_POST_CLOSE,
                "matchId=" + matchId + ", title=" + safeText(target.getTargetTitle()));

        log.info(
                "action=MATCH_POST_CLOSE admin_user_id={} target_match_id={} target_user_id={} result=SUCCESS",
                admin.getUserId(),
                matchId,
                target.getOwnerUserId());
    }

    @Transactional
    public void deletePost(int matchId, String adminUserId) {
        Member admin = requireMatchBoardManager(adminUserId);

        AdminMatchBoardTargetInfo target = findPostTarget(matchId);

        if (Integer.valueOf(0).equals(target.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 삭제된 의뢰글입니다.");
        }

        int updated = adminMatchBoardMapper.softDeletePost(matchId);
        validateUpdated(updated, "의뢰글 삭제에 실패했습니다.");

        saveActionHistory(
                admin.getUserId(),
                target.getOwnerUserId(),
                ACTION_MATCH_POST_DELETE,
                "matchId=" + matchId + ", title=" + safeText(target.getTargetTitle()));

        log.info(
                "action=MATCH_POST_DELETE admin_user_id={} target_match_id={} target_user_id={} result=SUCCESS",
                admin.getUserId(),
                matchId,
                target.getOwnerUserId());
    }

    @Transactional
    public void restorePost(int matchId, String adminUserId) {
        Member admin = requireMatchBoardManager(adminUserId);

        AdminMatchBoardTargetInfo target = findPostTarget(matchId);

        if (Integer.valueOf(1).equals(target.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 정상 상태인 의뢰글입니다.");
        }

        int updated = adminMatchBoardMapper.restorePost(matchId);
        validateUpdated(updated, "의뢰글 복구에 실패했습니다.");

        saveActionHistory(
                admin.getUserId(),
                target.getOwnerUserId(),
                ACTION_MATCH_POST_RESTORE,
                "matchId=" + matchId + ", title=" + safeText(target.getTargetTitle()));

        log.info(
                "action=MATCH_POST_RESTORE admin_user_id={} target_match_id={} target_user_id={} result=SUCCESS",
                admin.getUserId(),
                matchId,
                target.getOwnerUserId());
    }

    @Transactional
    public void deleteBid(int bidId, String adminUserId) {
        Member admin = requireMatchBoardManager(adminUserId);

        AdminMatchBoardTargetInfo target = findBidTarget(bidId);

        if (Integer.valueOf(0).equals(target.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 삭제된 입찰 의견입니다.");
        }

        int updated = adminMatchBoardMapper.softDeleteBid(bidId);
        validateUpdated(updated, "입찰 의견 삭제에 실패했습니다.");

        saveActionHistory(
                admin.getUserId(),
                target.getOwnerUserId(),
                ACTION_MATCH_BID_DELETE,
                "bidId=" + bidId
                        + ", matchId=" + target.getMatchId()
                        + ", content=" + safeText(target.getTargetTitle()));

        log.info(
                "action=MATCH_BID_DELETE admin_user_id={} target_bid_id={} target_user_id={} result=SUCCESS",
                admin.getUserId(),
                bidId,
                target.getOwnerUserId());
    }

    @Transactional
    public void restoreBid(int bidId, String adminUserId) {
        Member admin = requireMatchBoardManager(adminUserId);

        AdminMatchBoardTargetInfo target = findBidTarget(bidId);

        if (Integer.valueOf(1).equals(target.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 정상 상태인 입찰 의견입니다.");
        }

        int updated = adminMatchBoardMapper.restoreBid(bidId);
        validateUpdated(updated, "입찰 의견 복구에 실패했습니다.");

        saveActionHistory(
                admin.getUserId(),
                target.getOwnerUserId(),
                ACTION_MATCH_BID_RESTORE,
                "bidId=" + bidId
                        + ", matchId=" + target.getMatchId()
                        + ", content=" + safeText(target.getTargetTitle()));

        log.info(
                "action=MATCH_BID_RESTORE admin_user_id={} target_bid_id={} target_user_id={} result=SUCCESS",
                admin.getUserId(),
                bidId,
                target.getOwnerUserId());
    }

    private Member requireMatchBoardManager(String adminUserId) {
        return adminPermissionGuard.requirePermission(
                adminUserId,
                AdminRolePolicy.MATCH_BOARD_MANAGE);
    }

    private AdminMatchBoardTargetInfo findPostTarget(int matchId) {
        AdminMatchBoardTargetInfo target = adminMatchBoardMapper.selectPostTarget(matchId);

        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "의뢰글을 찾을 수 없습니다.");
        }

        return target;
    }

    private AdminMatchBoardTargetInfo findBidTarget(int bidId) {
        AdminMatchBoardTargetInfo target = adminMatchBoardMapper.selectBidTarget(bidId);

        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "입찰 의견을 찾을 수 없습니다.");
        }

        return target;
    }

    private boolean isCancelled(String status) {
        return "CANCELLED".equalsIgnoreCase(status)
                || "CANCELED".equalsIgnoreCase(status);
    }

    private void validateUpdated(int updated, String message) {
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    private <T> AdminMatchBoardPageResponseDto<T> createPageResponse(
            List<T> contents,
            AdminMatchBoardListRequestDto request,
            int total) {

        int totalPages = total == 0
                ? 0
                : (int) Math.ceil((double) total / request.getSize());

        return new AdminMatchBoardPageResponseDto<>(
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
            adminMatchBoardMapper.insertActionHistory(
                    new AdminMatchBoardActionHistory(
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