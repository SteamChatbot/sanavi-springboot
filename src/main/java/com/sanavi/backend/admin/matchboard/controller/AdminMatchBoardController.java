package com.sanavi.backend.admin.matchboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.matchboard.dto.AdminMatchBidResponseDto;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchBoardListRequestDto;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchBoardPageResponseDto;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchPostResponseDto;
import com.sanavi.backend.admin.matchboard.service.AdminMatchBoardService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 의뢰글 게시판관리 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/matches")
public class AdminMatchBoardController {

    private final AdminMatchBoardService adminMatchBoardService;

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<AdminMatchBoardPageResponseDto<AdminMatchPostResponseDto>>> getPosts(
            @ModelAttribute AdminMatchBoardListRequestDto request,
            @AuthenticationPrincipal String adminUserId) {

        AdminMatchBoardPageResponseDto<AdminMatchPostResponseDto> response = adminMatchBoardService.getPosts(request,
                adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 의뢰글 목록을 조회했습니다.", response));
    }

    @GetMapping("/bids")
    public ResponseEntity<ApiResponse<AdminMatchBoardPageResponseDto<AdminMatchBidResponseDto>>> getBids(
            @ModelAttribute AdminMatchBoardListRequestDto request,
            @AuthenticationPrincipal String adminUserId) {

        AdminMatchBoardPageResponseDto<AdminMatchBidResponseDto> response = adminMatchBoardService.getBids(request,
                adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 입찰 의견 목록을 조회했습니다.", response));
    }

    @PatchMapping("/posts/{matchId}/close")
    public ResponseEntity<ApiResponse<Void>> closePost(
            @PathVariable int matchId,
            @AuthenticationPrincipal String adminUserId) {

        adminMatchBoardService.closePost(matchId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("의뢰글이 강제마감되었습니다.", null));
    }

    @PatchMapping("/posts/{matchId}/delete")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable int matchId,
            @AuthenticationPrincipal String adminUserId) {

        adminMatchBoardService.deletePost(matchId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("의뢰글이 삭제되었습니다.", null));
    }

    @PatchMapping("/posts/{matchId}/restore")
    public ResponseEntity<ApiResponse<Void>> restorePost(
            @PathVariable int matchId,
            @AuthenticationPrincipal String adminUserId) {

        adminMatchBoardService.restorePost(matchId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("의뢰글이 복구되었습니다.", null));
    }

    @PatchMapping("/bids/{bidId}/delete")
    public ResponseEntity<ApiResponse<Void>> deleteBid(
            @PathVariable int bidId,
            @AuthenticationPrincipal String adminUserId) {

        adminMatchBoardService.deleteBid(bidId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("입찰 의견이 삭제되었습니다.", null));
    }

    @PatchMapping("/bids/{bidId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreBid(
            @PathVariable int bidId,
            @AuthenticationPrincipal String adminUserId) {

        adminMatchBoardService.restoreBid(bidId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("입찰 의견이 복구되었습니다.", null));
    }
}