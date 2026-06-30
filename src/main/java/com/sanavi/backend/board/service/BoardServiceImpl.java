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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;
    private final BoardFileMapper boardFileMapper;
    private final S3FileService s3FileService;

    @Override
    @Transactional(readOnly = true)
    public BoardListResponseDto getBoardList(int page, int size, String keyword, String searchType) {
        int offset = (page - 1) * size;

        List<BoardResponseDto> contents =
                boardMapper.selectBoardList(offset, size, keyword, searchType);

        int totalCount = boardMapper.selectBoardCount(keyword, searchType);

        return new BoardListResponseDto(contents, page, size, totalCount);
    }

    @Override
    @Transactional
    public BoardResponseDto getBoardById(int id) {
        boardMapper.increaseViewCount(id);

        BoardResponseDto board = boardMapper.selectBoardById(id);

        if (board == null) {
            log.warn(
                    "action=BOARD_DETAIL_VIEW target_type=board target_id={} result=FAIL reason=BOARD_NOT_FOUND",
                    id
            );

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

        if (boardId <= 0) {
            log.error(
                    "action=BOARD_CREATE target_type=board result=FAIL reason=BOARD_ID_NOT_GENERATED"
            );

            throw new IllegalStateException("게시글 등록에 실패했습니다.");
        }

        int uploadedFileCount = 0;

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                BoardFileDto uploadedFile =
                        s3FileService.uploadBoardFile(boardId, file);

                boardFileMapper.insertBoardFile(uploadedFile);
                uploadedFileCount++;
            }
        }

        log.info(
                "action=BOARD_CREATE target_type=board target_id={} file_count={} result=SUCCESS",
                boardId,
                uploadedFileCount
        );

        return boardId;
    }

    @Override
    @Transactional
    public void updateBoard(int id, BoardRequestDto requestDto) {
        boardMapper.updateBoard(id, requestDto);

        log.info(
                "action=BOARD_UPDATE target_type=board target_id={} result=SUCCESS",
                id
        );
    }

    @Override
    @Transactional
    public void deleteBoard(int id) {
        boardMapper.deleteBoard(id);
        boardFileMapper.deleteFilesByBoardId(id);

        log.info(
                "action=BOARD_DELETE target_type=board target_id={} result=SUCCESS",
                id
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BoardFileDto getBoardFile(int boardId, int fileId) {
        BoardFileDto file = boardFileMapper.selectFileById(boardId, fileId);

        if (file == null) {
            log.warn(
                    "action=BOARD_FILE_READ target_type=board_file target_id={} board_id={} result=FAIL reason=FILE_NOT_FOUND",
                    fileId,
                    boardId
            );

            throw new IllegalArgumentException("파일을 찾을 수 없습니다.");
        }

        return file;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadBoardFile(int boardId, int fileId) {
        BoardFileDto file = getBoardFile(boardId, fileId);

        byte[] data = s3FileService.download(file.getFilePath());

        log.info(
                "action=BOARD_FILE_DOWNLOAD target_type=board_file target_id={} board_id={} file_size={} result=SUCCESS",
                fileId,
                boardId,
                data.length
        );

        return data;
    }

    @Override
    @Transactional
    public List<BoardFileResponseDto> addBoardFiles(int boardId, List<MultipartFile> files) {
        BoardResponseDto board = boardMapper.selectBoardById(boardId);

        if (board == null) {
            log.warn(
                    "action=BOARD_FILE_UPLOAD target_type=board target_id={} result=FAIL reason=BOARD_NOT_FOUND",
                    boardId
            );

            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        int uploadedFileCount = 0;

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                BoardFileDto uploadedFile =
                        s3FileService.uploadBoardFile(boardId, file);

                boardFileMapper.insertBoardFile(uploadedFile);
                uploadedFileCount++;
            }
        }

        log.info(
                "action=BOARD_FILE_UPLOAD target_type=board target_id={} file_count={} result=SUCCESS",
                boardId,
                uploadedFileCount
        );

        return boardFileMapper.selectFilesByBoardId(boardId);
    }

    @Override
    @Transactional
    public void deleteBoardFile(int boardId, int fileId) {
        BoardFileDto file = boardFileMapper.selectFileById(boardId, fileId);

        if (file == null) {
            log.warn(
                    "action=BOARD_FILE_DELETE target_type=board_file target_id={} board_id={} result=FAIL reason=FILE_NOT_FOUND",
                    fileId,
                    boardId
            );

            throw new IllegalArgumentException("파일을 찾을 수 없습니다.");
        }

        int result = boardFileMapper.softDeleteFile(boardId, fileId);

        if (result != 1) {
            log.warn(
                    "action=BOARD_FILE_DELETE target_type=board_file target_id={} board_id={} result=FAIL reason=SOFT_DELETE_FAILED",
                    fileId,
                    boardId
            );

            throw new IllegalStateException("파일 삭제 처리에 실패했습니다.");
        }

        log.info(
                "action=BOARD_FILE_DELETE target_type=board_file target_id={} board_id={} result=SUCCESS",
                fileId,
                boardId
        );
    }
}