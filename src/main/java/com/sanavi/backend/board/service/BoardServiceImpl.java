package com.sanavi.backend.board.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.board.dto.BoardFileDto;
import com.sanavi.backend.board.dto.BoardFileResponseDto;
import com.sanavi.backend.board.dto.BoardListResponseDto;
import com.sanavi.backend.board.dto.BoardRequestDto;
import com.sanavi.backend.board.dto.BoardResponseDto;
import com.sanavi.backend.board.mapper.BoardFileMapper;
import com.sanavi.backend.board.mapper.BoardMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;
    private final BoardFileMapper boardFileMapper;
    private final S3FileService s3FileService;

    // 격리수준: REPEATABLE_READ (MariaDB 기본값) / readOnly
    // contents(목록)와 totalCount(건수) 두 쿼리가 동일 스냅샷을 봄
    // → 사이에 INSERT가 발생해도 이 트랜잭션 안에선 반영 안 됨 — 페이지네이션 불일치 방지
    @Override
    @Transactional(readOnly = true)
    public BoardListResponseDto getBoardList(int page, int size, String keyword, String searchType) {
        int offset = (page - 1) * size;

        List<BoardResponseDto> contents = boardMapper.selectBoardList(offset, size, keyword, searchType);

        int totalCount = boardMapper.selectBoardCount(keyword, searchType);

        return new BoardListResponseDto(contents, page, size, totalCount);
    }

    @Override
    @Transactional
    public BoardResponseDto getBoardById(int id) {
        boardMapper.increaseViewCount(id);

        BoardResponseDto board = boardMapper.selectBoardById(id);

        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        board.setFiles(boardFileMapper.selectFilesByBoardId(id));

        return board;
    }

    @Override
    @Transactional
    public int createBoard(BoardRequestDto requestDto, List<MultipartFile> files) {
        boardMapper.insertBoard(requestDto);

        int boardId = requestDto.getBoardId();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                BoardFileDto uploadedFile = s3FileService.uploadBoardFile(boardId, file);

                boardFileMapper.insertBoardFile(uploadedFile);
            }
        }

        return boardId;
    }

    @Override
    @Transactional
    public void updateBoard(int id, BoardRequestDto requestDto) {
        boardMapper.updateBoard(id, requestDto);
    }

    @Override
    @Transactional
    public void deleteBoard(int id) {
        boardMapper.deleteBoard(id);
        boardFileMapper.deleteFilesByBoardId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BoardFileDto getBoardFile(int boardId, int fileId) {
        BoardFileDto file = boardFileMapper.selectFileById(boardId, fileId);

        if (file == null) {
            throw new IllegalArgumentException("파일을 찾을 수 없습니다.");
        }

        return file;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadBoardFile(int boardId, int fileId) {
        BoardFileDto file = getBoardFile(boardId, fileId);

        return s3FileService.download(file.getFilePath());
    }

    @Override
    @Transactional
    public List<BoardFileResponseDto> addBoardFiles(int boardId, List<MultipartFile> files) {
        BoardResponseDto board = boardMapper.selectBoardById(boardId);

        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                BoardFileDto uploadedFile = s3FileService.uploadBoardFile(boardId, file);

                boardFileMapper.insertBoardFile(uploadedFile);
            }
        }

        return boardFileMapper.selectFilesByBoardId(boardId);
    }

    @Override
    @Transactional
    public void deleteBoardFile(int boardId, int fileId) {
        BoardFileDto file = boardFileMapper.selectFileById(boardId, fileId);

        if (file == null) {
            throw new IllegalArgumentException("파일을 찾을 수 없습니다.");
        }

        int result = boardFileMapper.softDeleteFile(boardId, fileId);

        if (result != 1) {
            throw new IllegalStateException("파일 삭제 처리에 실패했습니다.");
        }
    }
}