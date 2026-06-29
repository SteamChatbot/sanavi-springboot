package com.sanavi.backend.board.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanavi.backend.board.dto.BoardCommentRequestDto;
import com.sanavi.backend.board.dto.BoardCommentResponseDto;
import com.sanavi.backend.board.mapper.BoardCommentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardCommentServiceImpl implements BoardCommentService {

    private final BoardCommentMapper boardCommentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BoardCommentResponseDto> getComments(int boardId) {
        return boardCommentMapper.selectCommentsByBoardId(boardId);
    }

    @Override
    @Transactional
    public void createComment(int boardId, BoardCommentRequestDto requestDto) {
        validateComment(requestDto);

        int result = boardCommentMapper.insertComment(boardId, requestDto);

        if (result != 1) {
            throw new IllegalStateException("댓글 등록에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public void deleteComment(int boardId, int commentId, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("사용자 아이디가 필요합니다.");
        }

        int result = boardCommentMapper.softDeleteComment(
                boardId,
                commentId,
                userId
        );

        if (result != 1) {
            throw new IllegalStateException("댓글 삭제 권한이 없거나 이미 삭제된 댓글입니다.");
        }
    }

    private void validateComment(BoardCommentRequestDto requestDto) {
        if (requestDto.getUserId() == null || requestDto.getUserId().isBlank()) {
            throw new IllegalArgumentException("작성자 아이디가 필요합니다.");
        }

        if (requestDto.getNickname() == null || requestDto.getNickname().isBlank()) {
            throw new IllegalArgumentException("닉네임이 필요합니다.");
        }

        if (requestDto.getContent() == null || requestDto.getContent().isBlank()) {
            throw new IllegalArgumentException("댓글 내용을 입력해 주세요.");
        }

        if (requestDto.getContent().length() > 500) {
            throw new IllegalArgumentException("댓글은 500자 이하로 입력해 주세요.");
        }
    }
}