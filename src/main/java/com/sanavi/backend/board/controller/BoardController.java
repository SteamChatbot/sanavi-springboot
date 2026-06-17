package com.sanavi.backend.board.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.board.dto.BoardListResponseDto;
import com.sanavi.backend.board.dto.BoardRequestDto;
import com.sanavi.backend.board.dto.BoardResponseDto;
import com.sanavi.backend.board.service.BoardService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<ApiResponse<BoardListResponseDto>> getBoardList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String searchType) {
        BoardListResponseDto response = boardService.getBoardList(page, size, keyword, searchType);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록 조회 성공", response));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<BoardResponseDto>> getBoardById(@PathVariable int boardId) {
        BoardResponseDto response = boardService.getBoardById(boardId);
        return ResponseEntity.ok(ApiResponse.success("게시글 상세 조회 성공", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createBoard(@RequestBody BoardRequestDto requestDto) {
        boardService.createBoard(requestDto);
        return ResponseEntity.ok(ApiResponse.success("게시글이 등록되었습니다.", null));
    }

    @PatchMapping("/{boardId}")
    public ResponseEntity<ApiResponse<Void>> updateBoard(
            @PathVariable int boardId,
            @RequestBody BoardRequestDto requestDto) {
        boardService.updateBoard(boardId, requestDto);
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다.", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable int id) {
        boardService.deleteBoard(id);
        return ResponseEntity.ok().build();
    }
}