package com.sanavi.backend.match.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.common.dto.PageResponse;
import com.sanavi.backend.common.response.ApiResponse;
import com.sanavi.backend.common.service.S3Service;
import com.sanavi.backend.match.dto.*;
import com.sanavi.backend.match.mapper.MatchFileMapper;
import com.sanavi.backend.match.service.MatchNotificationService;
import com.sanavi.backend.match.service.MatchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final MatchNotificationService matchNotificationService;
    private final MatchFileMapper matchFileMapper;
    private final S3Service s3Service;

    // 목적: 의뢰글 목록 조회 — role에 따라 본인 것만 또는 전체 조회
    // Input:  page, size (페이지 정보)
    //         status (선택 — OPEN·BIDDING·CLOSED·CANCELLED / 미전달 시 전체)
    //         preferredRegion (선택 — 시도명 / 미전달 시 전체)
    //         minPrice (선택 — 이 금액 이상만 / 미전달 시 전체)
    // Output: PageResponse<MatchListResponseDto> — 페이지 메타 + 의뢰글 목록
    // 기능:   role_user → JWT userId로 본인 의뢰글만 / 변호사·비로그인 → 전체
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MatchListResponseDto>>> getMatchList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preferredRegion,
            @RequestParam(required = false) Integer minPrice,
            Authentication auth) {
        boolean isUser = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "role_user".equals(a.getAuthority()));
        String userId = isUser ? auth.getName() : null;
        return ResponseEntity.ok(ApiResponse.success("의뢰글 목록 조회 성공",
                matchService.getMatchList(page, size, userId, status, preferredRegion, minPrice)));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<ApiResponse<MatchResponseDto>> getMatchById(@PathVariable int matchId) {
        return ResponseEntity.ok(ApiResponse.success("의뢰글 상세 조회 성공",
                matchService.getMatchById(matchId)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> createMatch(
            @ModelAttribute MatchRequestDto requestDto,
            @RequestPart("pdf") MultipartFile pdf,
            @AuthenticationPrincipal String userId) throws IOException {
        requestDto.setUserId(userId);
        matchService.createMatch(requestDto, pdf);
        return ResponseEntity.ok(ApiResponse.success("의뢰글이 등록되었습니다.", null));
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<ApiResponse<String>> getDownloadUrl(@PathVariable int fileId) {
        MatchFileDto file = matchFileMapper.selectFileById(fileId);
        if (file == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success("다운로드 URL 발급 성공",
                s3Service.generatePresignedUrl(file.getFilePath(), 60 * 24 * 3)));
    }

    @PatchMapping("/{matchId}/status")
    public ResponseEntity<ApiResponse<Void>> updateMatchStatus(
            @PathVariable int matchId,
            @RequestParam String status,
            @AuthenticationPrincipal String userId) {
        matchService.updateMatchStatus(matchId, status, userId);
        return ResponseEntity.ok(ApiResponse.success("상태가 변경되었습니다.", null));
    }

    @DeleteMapping("/{matchId}")
    public ResponseEntity<ApiResponse<Void>> deleteMatch(@PathVariable int matchId) {
        matchService.deleteMatch(matchId);
        return ResponseEntity.ok(ApiResponse.success("의뢰글이 삭제되었습니다.", null));
    }

    @GetMapping("/{matchId}/bids")
    public ResponseEntity<ApiResponse<List<MatchBidResponseDto>>> getBidList(@PathVariable int matchId) {
        return ResponseEntity.ok(ApiResponse.success("입찰 목록 조회 성공",
                matchService.getBidList(matchId)));
    }

    @PostMapping("/{matchId}/bids")
    public ResponseEntity<ApiResponse<Void>> createBid(
            @PathVariable int matchId,
            @RequestBody MatchBidRequestDto requestDto,
            @AuthenticationPrincipal String userId) {
        requestDto.setMatchId(matchId);
        requestDto.setLawyerId(userId);
        matchService.createBid(requestDto);
        return ResponseEntity.ok(ApiResponse.success("입찰이 등록되었습니다.", null));
    }

    @PatchMapping("/{matchId}/bids/{bidId}/select")
    public ResponseEntity<ApiResponse<Void>> selectBid(
            @PathVariable int matchId,
            @PathVariable int bidId) {
        matchService.selectBid(matchId, bidId);
        matchNotificationService.sendMatchConfirmed(matchId, bidId);
        return ResponseEntity.ok(ApiResponse.success("입찰이 확정되었습니다.", null));
    }

    @PatchMapping("/{matchId}/bids/{bidId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectBid(
            @PathVariable int matchId,
            @PathVariable int bidId,
            @AuthenticationPrincipal String userId) {
        matchService.rejectBid(matchId, bidId, userId);
        return ResponseEntity.ok(ApiResponse.success("입찰이 거절되었습니다.", null));
    }

    @DeleteMapping("/bids/{bidId}")
    public ResponseEntity<ApiResponse<Void>> cancelBid(
            @PathVariable int bidId,
            @AuthenticationPrincipal String userId) {
        matchService.cancelBid(bidId, userId);
        return ResponseEntity.ok(ApiResponse.success("입찰이 취소되었습니다.", null));
    }
}
