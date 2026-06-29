package com.sanavi.backend.match.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.common.dto.PageResponse;
import com.sanavi.backend.match.dto.MatchBidRequestDto;
import com.sanavi.backend.match.dto.MatchBidResponseDto;
import com.sanavi.backend.match.dto.MatchListResponseDto;
import com.sanavi.backend.match.dto.MatchRequestDto;
import com.sanavi.backend.match.dto.MatchResponseDto;

// 책임: 의뢰글·입찰·첨부파일 비즈니스 로직 계약 정의
public interface MatchService {

    // Input:  page, size (페이지 정보), userId (null=전체 / 값=본인 필터)
    //         status (null=전체 / OPEN·BIDDING·CLOSED·CANCELLED)
    //         preferredRegion (null=전체 / 시도명)
    //         minPrice (null/0이하=전체 / 양수=해당 금액 이상)
    // Output: PageResponse<MatchListResponseDto>
    // 책임:   offset 계산 후 목록·전체 카운트 조회, 페이지 메타 포함해 반환
    PageResponse<MatchListResponseDto> getMatchList(
            int page, int size, String userId,
            String status, String preferredRegion, Integer minPrice);

    // Input:  matchId (의뢰글 PK)
    // Output: MatchResponseDto — 첨부파일 목록 포함 (null 가능)
    // 책임:   의뢰글 단건 조회 후 match_file 목록 세팅
    MatchResponseDto getMatchById(int matchId);

    // Input:  MatchRequestDto (의뢰글 데이터), pdf (MultipartFile — 필수)
    // Output: void
    // 책임:   의뢰글 INSERT → PDF 로컬 저장 → match_file INSERT (단일 트랜잭션)
    void createMatch(MatchRequestDto requestDto, MultipartFile pdf) throws IOException;

    // Input:  matchId, status, requestUserId — CANCELLED 시 소유권 검증
    // Output: void — 소유자 불일치 시 403 예외
    // 책임:   의뢰글 상태 갱신 (취소는 본인만 가능)
    void updateMatchStatus(int matchId, String status, String requestUserId);

    // Input:  matchId
    // Output: void
    // 책임:   의뢰글 논리 삭제 + 연관 첨부파일 일괄 논리 삭제
    void deleteMatch(int matchId);

    // Input:  matchId
    // Output: List<MatchBidResponseDto> — 입찰가 낮은 순 정렬
    // 책임:   해당 의뢰글의 유효 입찰 목록 반환
    List<MatchBidResponseDto> getBidList(int matchId);

    // Input:  MatchBidRequestDto (matchId·lawyerId·bidPrice·message)
    // Output: void
    // 책임:   입찰 INSERT, 의뢰글이 OPEN 상태이면 BIDDING으로 자동 전환
    void createBid(MatchBidRequestDto requestDto);

    // Input:  matchId, bidId (확정할 입찰 PK)
    // Output: void
    // 책임:   선택 입찰 SELECTED, 나머지 REJECTED, 의뢰글 CLOSED — 세 작업 단일 트랜잭션
    void selectBid(int matchId, int bidId);

    // Input:  matchId, bidId, userId (의뢰글 소유자 검증용)
    // Output: void — 소유자 불일치 시 403
    // 책임:   의뢰글 작성자가 특정 입찰을 거절 (REJECTED 상태 변경)
    void rejectBid(int matchId, int bidId, String userId);

    // Input:  bidId, lawyerId (입찰자 본인 검증용)
    // Output: void — 본인 입찰 아니면 403
    // 책임:   변호사가 자신의 입찰을 취소 (논리 삭제)
    void cancelBid(int bidId, String lawyerId);
}
