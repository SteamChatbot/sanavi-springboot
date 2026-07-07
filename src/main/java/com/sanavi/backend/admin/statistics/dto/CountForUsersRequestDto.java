package com.sanavi.backend.admin.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// ai-api POST /api/admin/analysis/count-for-users 요청 바디 — main_db에서 이미 추려낸 userIds를 넘김
@Getter
@AllArgsConstructor
public class CountForUsersRequestDto {
    private String range;
    private List<String> userIds;
}
