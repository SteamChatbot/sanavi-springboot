package com.sanavi.backend.admin.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 조치 대상 조회용 내부 DTO
@Getter
@Setter
@NoArgsConstructor
public class AdminBoardTargetInfo {

    private Integer targetId;
    private String ownerUserId;
    private String targetTitle;
    private Integer deleted;
}