package com.sanavi.backend.admin.role.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class AdminRoleCandidatePageResponseDto {

    private final List<AdminRoleCandidateResponseDto> content;
    private final int page;
    private final int size;
    private final int totalCount;
    private final int totalPages;

    public AdminRoleCandidatePageResponseDto(
            List<AdminRoleCandidateResponseDto> content,
            int page,
            int size,
            int totalCount) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalCount = totalCount;
        this.totalPages = (int) Math.ceil((double) totalCount / size);
    }
}