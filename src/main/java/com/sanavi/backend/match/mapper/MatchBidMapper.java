package com.sanavi.backend.match.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.match.dto.MatchBidRequestDto;
import com.sanavi.backend.match.dto.MatchBidResponseDto;

// 책임: match_bid 테이블 CRUD MyBatis 매퍼
//       MatchBidMapper.xml (mapper/match/) 와 바인딩
@Mapper
public interface MatchBidMapper {

    // Input:  matchId (의뢰글 PK)
    // Output: List<MatchBidResponseDto> — 입찰가 낮은 순(ASC), member JOIN으로 변호사 정보 포함
    // 책임:   해당 의뢰글의 유효 입찰 목록 조회
    List<MatchBidResponseDto> selectBidListByMatchId(@Param("matchId") int matchId);

    // Input:  bidId (입찰 PK)
    // Output: MatchBidResponseDto (null 가능)
    // 책임:   입찰 단건 조회
    MatchBidResponseDto selectBidById(@Param("bidId") int bidId);

    // Input:  matchId, lawyerId
    // Output: int — 해당 변호사의 해당 의뢰글 활성 입찰 수
    // 책임:   중복 입찰 방지 검증용 카운트 (0 이상이면 이미 입찰한 상태)
    int countActiveBidByLawyer(@Param("matchId") int matchId, @Param("lawyerId") String lawyerId);

    // Input:  MatchBidRequestDto (matchId·lawyerId·bidPrice·message)
    // Output: int — 영향 받은 행 수 (useGeneratedKeys로 requestDto.id에 생성 PK 주입)
    // 책임:   입찰 INSERT, 초기 status='PENDING' 고정
    int insertBid(MatchBidRequestDto requestDto);

    // Input:  bidId, status (PENDING·SELECTED·REJECTED)
    // Output: int — 영향 받은 행 수
    // 책임:   입찰 상태 갱신 (확정 또는 거절 처리)
    int updateBidStatus(@Param("bidId") int bidId, @Param("status") String status);

    // Input:  matchId, selectedBidId (확정된 입찰 PK — 제외 대상)
    // Output: int — 영향 받은 행 수
    // 책임:   selectBid 호출 시 선택되지 않은 나머지 입찰을 일괄 REJECTED 처리
    int rejectOtherBids(@Param("matchId") int matchId, @Param("selectedBidId") int selectedBidId);

    // Input:  bidId
    // Output: int — 영향 받은 행 수
    // 책임:   입찰 논리 삭제 (deleted=0)
    int deleteBid(@Param("bidId") int bidId);
}
