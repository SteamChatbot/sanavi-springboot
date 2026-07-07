package com.sanavi.backend.admin.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 관리자 게시판 조치 이력
// member_admin_action_history 테이블에 남긴다
@Getter
@AllArgsConstructor
public class AdminBoardActionHistory {

    private final String adminUserId;
    private final String targetUserId;
    private final String actionType;
    private final String reason;
}