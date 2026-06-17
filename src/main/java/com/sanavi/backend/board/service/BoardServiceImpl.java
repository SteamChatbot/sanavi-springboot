package com.sanavi.backend.board.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sanavi.backend.board.dto.BoardListResponseDto;
import com.sanavi.backend.board.dto.BoardRequestDto;
import com.sanavi.backend.board.dto.BoardResponseDto;
import com.sanavi.backend.board.mapper.BoardMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;

    @Override
public BoardListResponseDto getBoardList(int page, int size, String keyword, String searchType) {
    int offset = (page - 1) * size;

    List<BoardResponseDto> contents =
            boardMapper.selectBoardList(offset, size, keyword, searchType);

    int totalCount =
            boardMapper.selectBoardCount(keyword, searchType);

    return new BoardListResponseDto(contents, page, size, totalCount);
}

    @Override
    public BoardResponseDto getBoardById(int id) {
        return boardMapper.selectBoardById(id);
    }

    @Override
    public void createBoard(BoardRequestDto requestDto) {
        boardMapper.insertBoard(requestDto);
    }

    @Override
    public void updateBoard(int id, BoardRequestDto requestDto) {
        boardMapper.updateBoard(id, requestDto);
    }

    @Override
    public void deleteBoard(int id) {
        boardMapper.deleteBoard(id);
    }
    
}
