package com.sanavi.backend.board.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.board.dto.BoardFileDto;
import com.sanavi.backend.board.dto.BoardFileResponseDto;
import com.sanavi.backend.board.dto.BoardListResponseDto;
import com.sanavi.backend.board.dto.BoardRequestDto;
import com.sanavi.backend.board.dto.BoardResponseDto;

public interface BoardService {
    BoardListResponseDto getBoardList(int page, int size, String keyword, String searchType);

    BoardResponseDto getBoardById(int id);

    int createBoard(BoardRequestDto requestDto, List<MultipartFile> files);

    void updateBoard(int id, BoardRequestDto requestDto);

    void deleteBoard(int id);

    byte[] downloadBoardFile(int boardId, int fileId);

    BoardFileDto getBoardFile(int boardId, int fileId);

    List<BoardFileResponseDto> addBoardFiles(int boardId, List<MultipartFile> files);

    void deleteBoardFile(int boardId, int fileId);
}
