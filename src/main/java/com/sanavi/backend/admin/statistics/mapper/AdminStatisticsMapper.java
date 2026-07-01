package com.sanavi.backend.admin.statistics.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.common.dto.TrendPointDto;

// 관리자 통계 전용 매퍼 — member/match/match_bid/member_lawyers 테이블을 읽기 전용으로 집계
// 각 도메인 매퍼(MemberMapper 등)와는 별개 — admin 통계 페이지 담당자가 다른 도메인 담당자의
// 파일을 건드리지 않고 독립적으로 작업할 수 있도록 분리함
@Mapper
public interface AdminStatisticsMapper {

    // 회원 — 일별(최근 7일)/월별(최근 6개월) 가입·이탈 추이
    List<TrendPointDto> selectSignupTrend(@Param("range") String range);
    List<TrendPointDto> selectWithdrawalTrend(@Param("range") String range);
    int countTotalMembers();
    int countMembersBySubscribe(@Param("subscribe") int subscribe);
    int countAiCountExhausted();

    // 관리자 다중필터 조합 검색 — 구독여부·유저타입·직업으로 후보 유저를 먼저 추림 (ai-api 호출 전 단계)
    List<String> selectMemberCandidates(
            @Param("subscribe") Integer subscribe,
            @Param("role") String role,
            @Param("job") String job,
            @Param("limit") int limit);

    // 직업 키워드 TOP N 후보 생성용 원본 — member.job은 자유입력이라 단어 분리는 서비스 레이어(Java)에서 처리
    List<String> selectAllJobs();

    // 매칭 — 전체/상태별 의뢰글 수, 입찰 평균가
    int countTotalMatches();
    int countMatchesByStatus(@Param("status") String status);
    Double selectAverageBidPrice();

    // 변호사 풀 — 지역/전문분야 분포, 평균 경력
    int countTotalLawyers();
    List<TrendPointDto> selectLawyerCountBySido();
    List<TrendPointDto> selectLawyerCountBySpecialty();
    Double selectAverageExperienceYears();
}
