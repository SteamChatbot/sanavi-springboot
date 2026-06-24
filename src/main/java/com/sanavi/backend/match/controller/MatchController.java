package com.sanavi.backend.match.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.common.dto.PageResponse;
import com.sanavi.backend.common.response.ApiResponse;
import com.sanavi.backend.common.service.S3Service;
import com.sanavi.backend.match.dto.MatchBidRequestDto;
import com.sanavi.backend.match.dto.MatchBidResponseDto;
import com.sanavi.backend.match.dto.MatchListResponseDto;
import com.sanavi.backend.match.dto.MatchFileDto;
import com.sanavi.backend.match.dto.MatchRequestDto;
import com.sanavi.backend.match.dto.MatchResponseDto;
import com.sanavi.backend.match.mapper.MatchFileMapper;
import com.sanavi.backend.match.service.MatchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final MatchFileMapper matchFileMapper;
    private final S3Service s3Service;

    // Input:  page (기본 1), size (기본 10), userId (null이면 전체 / 값 있으면 해당 유저 의뢰글만)
    // Output: PageResponse<MatchListResponseDto> — 목록 + 페이지 정보
    // 책임:   변호사는 userId 없이 전체 조회, 고객은 userId로 본인 의뢰글만 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MatchListResponseDto>>> getMatchList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "userId", required = false) String userId) {
        PageResponse<MatchListResponseDto> response = matchService.getMatchList(page, size, userId);
        return ResponseEntity.ok(ApiResponse.success("의뢰글 목록 조회 성공", response));
    }

    // Input:  matchId (PathVariable)
    // Output: MatchResponseDto — 의뢰글 상세 + 첨부파일 목록 포함
    // 책임:   의뢰글 단건 조회, member JOIN으로 의뢰자명·연락처 함께 반환
    @GetMapping("/{matchId}")
    public ResponseEntity<ApiResponse<MatchResponseDto>> getMatchById(@PathVariable("matchId") int matchId) {
        MatchResponseDto response = matchService.getMatchById(matchId);
        return ResponseEntity.ok(ApiResponse.success("의뢰글 상세 조회 성공", response));
    }

    // Input:  MatchRequestDto (form-data: userId·title·content·price·matchType) + pdf (MultipartFile — 필수)
    // Output: 200 OK (data: null)
    // 책임:   의뢰글 INSERT 후 PDF 로컬 저장·match_file INSERT, 두 작업 하나의 트랜잭션으로 처리
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> createMatch(
            @ModelAttribute MatchRequestDto requestDto,
            @RequestPart("pdf") MultipartFile pdf) throws IOException {
        matchService.createMatch(requestDto, pdf);
        return ResponseEntity.ok(ApiResponse.success("의뢰글이 등록되었습니다.", null));
    }

    // Input:  fileId (PathVariable)
    // Output: ApiResponse<String> — 15분 유효 Presigned URL
    // 책임:   비공개 S3 버킷 파일을 임시 URL로 안전하게 제공
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<ApiResponse<String>> getDownloadUrl(@PathVariable("fileId") int fileId) {
        MatchFileDto file = matchFileMapper.selectFileById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        String presignedUrl = s3Service.generatePresignedUrl(file.getFilePath(), 60 * 24 * 3);
        return ResponseEntity.ok(ApiResponse.success("다운로드 URL 발급 성공", presignedUrl));
    }

    // Input:  matchId (PathVariable), status (QueryParam — OPEN·BIDDING·CLOSED·CANCELLED)
    // Output: 200 OK (data: null)
    // 책임:   의뢰글 상태 직접 변경 (관리자·고객 취소용)
    @PatchMapping("/{matchId}/status")
    public ResponseEntity<ApiResponse<Void>> updateMatchStatus(
            @PathVariable("matchId") int matchId,
            @RequestParam("status") String status,
            @RequestParam("userId") String userId) {
        matchService.updateMatchStatus(matchId, status, userId);
        return ResponseEntity.ok(ApiResponse.success("상태가 변경되었습니다.", null));
    }

    // Input:  matchId (PathVariable)
    // Output: 200 OK (data: null)
    // 책임:   의뢰글 + 연관 첨부파일 논리 삭제 (deleted=0)
    @DeleteMapping("/{matchId}")
    public ResponseEntity<ApiResponse<Void>> deleteMatch(@PathVariable("matchId") int matchId) {
        matchService.deleteMatch(matchId);
        return ResponseEntity.ok(ApiResponse.success("의뢰글이 삭제되었습니다.", null));
    }

    // Input:  matchId (PathVariable)
    // Output: List<MatchBidResponseDto> — 입찰가 낮은 순 정렬, 변호사명·직종 포함
    // 책임:   해당 의뢰글의 유효 입찰 목록 반환
    @GetMapping("/{matchId}/bids")
    public ResponseEntity<ApiResponse<List<MatchBidResponseDto>>> getBidList(@PathVariable("matchId") int matchId) {
        List<MatchBidResponseDto> response = matchService.getBidList(matchId);
        return ResponseEntity.ok(ApiResponse.success("입찰 목록 조회 성공", response));
    }

    // Input:  matchId (PathVariable), MatchBidRequestDto (lawyerId·bidPrice·message)
    // Output: 200 OK (data: null)
    // 책임:   입찰 등록, 첫 입찰이면 의뢰글 상태 OPEN→BIDDING 자동 전환
    @PostMapping("/{matchId}/bids")
    public ResponseEntity<ApiResponse<Void>> createBid(
            @PathVariable("matchId") int matchId,
            @RequestBody MatchBidRequestDto requestDto) {
        requestDto.setMatchId(matchId);
        matchService.createBid(requestDto);
        return ResponseEntity.ok(ApiResponse.success("입찰이 등록되었습니다.", null));
    }

    // Input:  matchId (PathVariable), bidId (PathVariable)
    // Output: 200 OK (data: null)
    // 책임:   선택 입찰 SELECTED 처리, 나머지 입찰 일괄 REJECTED, 의뢰글 상태 CLOSED 변경
    @PatchMapping("/{matchId}/bids/{bidId}/select")
    public ResponseEntity<ApiResponse<Void>> selectBid(
            @PathVariable("matchId") int matchId,
            @PathVariable("bidId") int bidId) {
        matchService.selectBid(matchId, bidId);
        return ResponseEntity.ok(ApiResponse.success("입찰이 확정되었습니다.", null));
    }

    // Input:  matchId, bidId, userId (의뢰글 소유자)
    // Output: 200 OK
    // 책임:   의뢰글 작성자가 특정 입찰 거절 (REJECTED)
    @PatchMapping("/{matchId}/bids/{bidId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectBid(
            @PathVariable("matchId") int matchId,
            @PathVariable("bidId") int bidId,
            @RequestParam("userId") String userId) {
        matchService.rejectBid(matchId, bidId, userId);
        return ResponseEntity.ok(ApiResponse.success("입찰이 거절되었습니다.", null));
    }

    // Input:  bidId, lawyerId (입찰한 변호사 본인)
    // Output: 200 OK
    // 책임:   변호사 본인 입찰 취소 (논리 삭제)
    @DeleteMapping("/bids/{bidId}")
    public ResponseEntity<ApiResponse<Void>> cancelBid(
            @PathVariable("bidId") int bidId,
            @RequestParam("lawyerId") String lawyerId) {
        matchService.cancelBid(bidId, lawyerId);
        return ResponseEntity.ok(ApiResponse.success("입찰이 취소되었습니다.", null));
    }
}
