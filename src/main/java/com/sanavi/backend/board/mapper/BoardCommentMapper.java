package com.sanavi.backend.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.board.dto.BoardCommentRequestDto;
import com.sanavi.backend.board.dto.BoardCommentResponseDto;

@Mapper
public interface BoardCommentMapper {

    List<BoardCommentResponseDto> selectCommentsByBoardId(
            @Param("boardId") int boardId
    );

    int insertComment(
            @Param("boardId") int boardId,
            @Param("request") BoardCommentRequestDto request
    );

    int softDeleteComment(
            @Param("boardId") int boardId,
            @Param("commentId") int commentId,
            @Param("userId") String userId
    );
}