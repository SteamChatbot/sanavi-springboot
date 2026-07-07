package com.sanavi.backend.admin.matchboard.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 관리자 의뢰글 게시판 목록 공통 페이지 응답
@Getter
@AllArgsConstructor
public class AdminMatchBoardPageResponseDto<T> {

    private final List<T> contents;

    private final int page;
    private final int size;

    private final int totalElements;
    private final int totalPages;
}