package com.sanavi.backend.board.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.board.dto.BoardCommentRequestDto;
import com.sanavi.backend.board.dto.BoardCommentResponseDto;
import com.sanavi.backend.board.service.BoardCommentService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardCommentController {

    private final BoardCommentService boardCommentService;

    @GetMapping("/{boardId}/comments")
    public ResponseEntity<ApiResponse<List<BoardCommentResponseDto>>> getComments(
            @PathVariable int boardId
    ) {
        List<BoardCommentResponseDto> response =
                boardCommentService.getComments(boardId);

        return ResponseEntity.ok(
                ApiResponse.success("댓글 목록 조회 성공", response)
        );
    }

    @PostMapping("/{boardId}/comments")
    public ResponseEntity<ApiResponse<Void>> createComment(
            @PathVariable int boardId,
            @RequestBody BoardCommentRequestDto requestDto
    ) {
        boardCommentService.createComment(boardId, requestDto);

        return ResponseEntity.ok(
                ApiResponse.success("댓글이 등록되었습니다.", null)
        );
    }

    @DeleteMapping("/{boardId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable int boardId,
            @PathVariable int commentId,
            @RequestParam String userId
    ) {
        boardCommentService.deleteComment(boardId, commentId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("댓글이 삭제되었습니다.", null)
        );
    }
}