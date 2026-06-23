package com.sanavi.backend.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.board.dto.BoardRequestDto;
import com.sanavi.backend.board.dto.BoardResponseDto;

@Mapper
public interface BoardMapper {
        List<BoardResponseDto> selectBoardList(
                        @Param("offset") int offset,
                        @Param("size") int size,
                        @Param("keyword") String keyword,
                        @Param("searchType") String searchType);

        int selectBoardCount(
                        @Param("keyword") String keyword,
                        @Param("searchType") String searchType);

        BoardResponseDto selectBoardById(@Param("boardId") int boardId);

        int insertBoard(BoardRequestDto boardRequestDto);

        int updateBoard(@Param("boardId") int boardId, @Param("board") BoardRequestDto boardRequestDto);

        int deleteBoard(@Param("boardId") int boardId);

        int increaseViewCount(@Param("id") int id);
}
