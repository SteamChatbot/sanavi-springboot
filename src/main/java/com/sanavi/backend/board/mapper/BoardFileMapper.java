package com.sanavi.backend.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.board.dto.BoardFileDto;
import com.sanavi.backend.board.dto.BoardFileResponseDto;

@Mapper
public interface BoardFileMapper {

    int insertBoardFile(BoardFileDto file);

    List<BoardFileResponseDto> selectFilesByBoardId(@Param("boardId") int boardId);

    BoardFileDto selectFileById(
            @Param("boardId") int boardId,
            @Param("fileId") int fileId);

    int deleteFilesByBoardId(@Param("boardId") int boardId);

    int softDeleteFile(
            @Param("boardId") int boardId,
            @Param("fileId") int fileId);
}