package com.sanavi.backend.admin.board.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.board.dto.AdminBoardCommentResponseDto;
import com.sanavi.backend.admin.board.dto.AdminBoardListRequestDto;
import com.sanavi.backend.admin.board.dto.AdminBoardPageResponseDto;
import com.sanavi.backend.admin.board.dto.AdminBoardPostResponseDto;
import com.sanavi.backend.admin.board.service.AdminBoardService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 게시판관리 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards")
public class AdminBoardController {

    private final AdminBoardService adminBoardService;

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<AdminBoardPageResponseDto<AdminBoardPostResponseDto>>> getPosts(
            @ModelAttribute AdminBoardListRequestDto request,
            @AuthenticationPrincipal String adminUserId) {

        AdminBoardPageResponseDto<AdminBoardPostResponseDto> response = adminBoardService.getPosts(request,
                adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 게시글 목록을 조회했습니다.", response));
    }

    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<AdminBoardPageResponseDto<AdminBoardCommentResponseDto>>> getComments(
            @ModelAttribute AdminBoardListRequestDto request,
            @AuthenticationPrincipal String adminUserId) {

        AdminBoardPageResponseDto<AdminBoardCommentResponseDto> response = adminBoardService.getComments(request,
                adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 댓글 목록을 조회했습니다.", response));
    }

    @PatchMapping("/posts/{boardId}/delete")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable int boardId,
            @AuthenticationPrincipal String adminUserId) {

        adminBoardService.deletePost(boardId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("게시글이 삭제되었습니다.", null));
    }

    @PatchMapping("/posts/{boardId}/restore")
    public ResponseEntity<ApiResponse<Void>> restorePost(
            @PathVariable int boardId,
            @AuthenticationPrincipal String adminUserId) {

        adminBoardService.restorePost(boardId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("게시글이 복구되었습니다.", null));
    }

    @PatchMapping("/comments/{commentId}/delete")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable int commentId,
            @AuthenticationPrincipal String adminUserId) {

        adminBoardService.deleteComment(commentId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("댓글이 삭제되었습니다.", null));
    }

    @PatchMapping("/comments/{commentId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreComment(
            @PathVariable int commentId,
            @AuthenticationPrincipal String adminUserId) {

        adminBoardService.restoreComment(commentId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("댓글이 복구되었습니다.", null));
    }
}