package com.sanavi.backend.board.service;

import com.sanavi.backend.board.dto.BoardListResponseDto;
import com.sanavi.backend.board.dto.BoardRequestDto;
import com.sanavi.backend.board.dto.BoardResponseDto;

public interface BoardService {
    BoardListResponseDto getBoardList(int page, int size, String keyword, String searchType);

    BoardResponseDto getBoardById(int id);

    void createBoard(BoardRequestDto requestDto);

    void updateBoard(int id, BoardRequestDto requestDto);

    void deleteBoard(int id);
}
