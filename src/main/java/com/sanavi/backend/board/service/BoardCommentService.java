package com.sanavi.backend.board.service;

import java.util.List;

import com.sanavi.backend.board.dto.BoardCommentRequestDto;
import com.sanavi.backend.board.dto.BoardCommentResponseDto;

public interface BoardCommentService {

    List<BoardCommentResponseDto> getComments(int boardId);

    void createComment(int boardId, BoardCommentRequestDto requestDto);

    void deleteComment(int boardId, int commentId, String userId);
}