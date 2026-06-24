package com.sanavi.backend.match.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.match.dto.MatchListResponseDto;
import com.sanavi.backend.match.dto.MatchRequestDto;
import com.sanavi.backend.match.dto.MatchResponseDto;

// 책임: match_request 테이블 CRUD MyBatis 매퍼
//       MatchMapper.xml (mapper/match/) 와 바인딩
@Mapper
public interface MatchMapper {

    // Input:  offset (페이지 시작 행), size (페이지 크기), userId (null = 전체 / 값 = 해당 유저 필터)
    // Output: List<MatchListResponseDto> — 목록용 경량 DTO (content·phone 제외)
    // 책임:   페이지네이션 적용 의뢰글 목록 조회
    List<MatchListResponseDto> selectMatchList(
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("userId") String userId);

    // Input:  userId (null = 전체 카운트 / 값 = 해당 유저 카운트)
    // Output: int — 전체 의뢰글 수 (페이지네이션 totalCount 계산용)
    // 책임:   selectMatchList와 동일 조건의 카운트 쿼리
    int selectMatchCount(@Param("userId") String userId);

    // Input:  matchId (의뢰글 PK)
    // Output: MatchResponseDto — member JOIN으로 requesterName·phone 포함 (null 가능)
    // 책임:   의뢰글 단건 상세 조회
    MatchResponseDto selectMatchById(@Param("matchId") int matchId);

    // Input:  MatchRequestDto (userId·title·content·price·matchType)
    // Output: int — 영향 받은 행 수 (useGeneratedKeys로 requestDto.id에 생성 PK 주입)
    // 책임:   의뢰글 INSERT, 초기 status='OPEN' 고정
    int insertMatch(MatchRequestDto requestDto);

    // Input:  matchId, status (OPEN·BIDDING·CLOSED·CANCELLED)
    // Output: int — 영향 받은 행 수
    // 책임:   의뢰글 상태 컬럼 갱신 (논리 삭제된 행 제외)
    int updateMatchStatus(@Param("matchId") int matchId, @Param("status") String status);

    // Input:  matchId
    // Output: int — 영향 받은 행 수
    // 책임:   의뢰글 논리 삭제 (deleted=0)
    int deleteMatch(@Param("matchId") int matchId);

    // Input:  matchId
    // Output: 해당 의뢰글 작성자 userId (없으면 null)
    // 책임:   취소·삭제 시 소유권 검증용
    String selectMatchOwner(@Param("matchId") int matchId);
}
