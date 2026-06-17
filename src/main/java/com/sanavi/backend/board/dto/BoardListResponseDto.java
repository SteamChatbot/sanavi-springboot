package com.sanavi.backend.board.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoardListResponseDto {
    private List<BoardResponseDto> contents;
    private int page;
    private int size;
    private int totalCount;
}