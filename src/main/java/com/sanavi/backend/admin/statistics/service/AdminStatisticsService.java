package com.sanavi.backend.admin.statistics.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.admin.statistics.dto.AdminAnalysisComboFilterRequest;
import com.sanavi.backend.admin.statistics.dto.AdminAnalysisComboResultDto;
import com.sanavi.backend.admin.statistics.dto.AnalysisCountResultDto;
import com.sanavi.backend.admin.statistics.dto.CountForUsersRequestDto;
import com.sanavi.backend.admin.statistics.dto.LawyerStatsDto;
import com.sanavi.backend.admin.statistics.dto.MatchStatsDto;
import com.sanavi.backend.admin.statistics.dto.MemberStatsDto;
import com.sanavi.backend.admin.statistics.dto.MemberTrendPointDto;
import com.sanavi.backend.admin.statistics.mapper.AdminStatisticsMapper;
import com.sanavi.backend.common.dto.TrendPointDto;

import lombok.RequiredArgsConstructor;

// 관리자 통계 전용 서비스 — 회원/매칭/변호사풀 집계 + ai-api와 조합하는 다중필터 검색(날짜×구독여부×유저타입×직업)을 한곳에서 처리
@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final AdminStatisticsMapper adminStatisticsMapper;
    private final RestClient aiApiClient;

    // Input:  range ("daily" 최근 7일 / "monthly" 최근 6개월, 그 외는 daily 처리)
    // Output: List<MemberTrendPointDto> — 가입·이탈 추이 (빈 날짜도 0건으로 채워 연속 그래프 보장)
    @Transactional(readOnly = true)
    public List<MemberTrendPointDto> getMemberTrend(String range) {
        Map<String, Integer> signupMap = adminStatisticsMapper.selectSignupTrend(range).stream()
                .collect(Collectors.toMap(TrendPointDto::getLabel, TrendPointDto::getCount));
        Map<String, Integer> withdrawalMap = adminStatisticsMapper.selectWithdrawalTrend(range).stream()
                .collect(Collectors.toMap(TrendPointDto::getLabel, TrendPointDto::getCount));

        return generateLabels(range).stream()
                .map(label -> new MemberTrendPointDto(
                        label,
                        signupMap.getOrDefault(label, 0),
                        withdrawalMap.getOrDefault(label, 0)))
                .collect(Collectors.toList());
    }

    // SQL의 DATE_FORMAT과 동일한 포맷(yyyy-MM-dd / yyyy-MM)으로 라벨을 직접 생성
    // — 가입·이탈 어느 쪽도 데이터가 없는 날짜/월이 그래프에서 비어 보이지 않도록 보장
    private List<String> generateLabels(String range) {
        List<String> labels = new ArrayList<>();
        if ("monthly".equals(range)) {
            YearMonth current = YearMonth.now();
            for (int i = 5; i >= 0; i--) {
                labels.add(current.minusMonths(i).toString());
            }
        } else {
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                labels.add(today.minusDays(i).toString());
            }
        }
        return labels;
    }

    // Output: MemberStatsDto — 전체/Pro/Basic 회원수 + Basic 중 AI 무료횟수(3회) 소진 비율(%)
    @Transactional(readOnly = true)
    public MemberStatsDto getMemberStats() {
        int total = adminStatisticsMapper.countTotalMembers();
        int pro = adminStatisticsMapper.countMembersBySubscribe(1);
        int basic = adminStatisticsMapper.countMembersBySubscribe(0);
        int exhausted = adminStatisticsMapper.countAiCountExhausted();
        double exhaustedRate = basic > 0 ? Math.round((double) exhausted / basic * 1000) / 10.0 : 0;

        return new MemberStatsDto(total, pro, basic, exhaustedRate);
    }

    // Output: MatchStatsDto — 전체 의뢰글 중 CLOSED(성사) 비율 + 입찰 평균가
    @Transactional(readOnly = true)
    public MatchStatsDto getMatchStats() {
        int total = adminStatisticsMapper.countTotalMatches();
        int closed = adminStatisticsMapper.countMatchesByStatus("CLOSED");
        double successRate = total > 0 ? Math.round((double) closed / total * 1000) / 10.0 : 0;
        Double avgBidPrice = adminStatisticsMapper.selectAverageBidPrice();

        return new MatchStatsDto(total, closed, successRate, avgBidPrice);
    }

    // Output: LawyerStatsDto — 변호사 풀 현황 (지역/전문분야 분포, 평균 경력)
    @Transactional(readOnly = true)
    public LawyerStatsDto getLawyerStats() {
        return new LawyerStatsDto(
                adminStatisticsMapper.countTotalLawyers(),
                adminStatisticsMapper.selectLawyerCountBySido(),
                adminStatisticsMapper.selectLawyerCountBySpecialty(),
                adminStatisticsMapper.selectAverageExperienceYears());
    }

    // Output: List<TrendPointDto> — 직업 키워드 TOP N (member.job이 자유입력이라 정확히 일치하는
    // 값끼리 묶으면 너무 잘게 쪼개짐 — 공백 기준으로 단어를 뽑아 빈도로 집계. DB단이 아니라
    // 여기(Java)서 처리하는 이유: MariaDB엔 문자열을 단어로 쪼개는 표준 함수가 없어 SQL로 하면
    // 억지스러운 트릭이 필요한데, member 테이블 규모에서는 애플리케이션 레벨 집계가 더 단순하고 안전함
    @Transactional(readOnly = true)
    public List<TrendPointDto> getTopJobKeywords(int limit) {
        Map<String, Integer> wordCounts = adminStatisticsMapper.selectAllJobs().stream()
                .flatMap(job -> Arrays.stream(job.trim().split("\\s+")))
                .filter(word -> !word.isBlank())
                .collect(Collectors.groupingBy(word -> word, Collectors.summingInt(word -> 1)));

        return wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new TrendPointDto(e.getKey(), e.getValue()))
                .toList();
    }

    // ── 관리자 다중필터 조합 검색 — 날짜범위 × 구독여부 × 유저타입 × 직업 ──────────────────
    // main_db(member) ↔ ai_db(analysis_result)는 FK가 없어 "유저를 먼저 추리고" 방식으로 처리한다:
    // ① main_db에서 구독여부·유저타입·직업 조건에 맞는 후보 유저를 (검색버튼 클릭 시 고른 상한만큼) 추린 뒤
    // ② 그 유저ID 목록만 ai-api로 보내 기간 내 분석횟수 총합을 집계 — 매칭 이전에 ai_db 전체를 끌어오지 않음

    // Input:  AdminAnalysisComboFilterRequest — range/subscribe/role/job/limit을 한 객체로 묶은 요청 조건
    // Output: AdminAnalysisComboResultDto — sampleSize(조건에 맞는 후보 유저 수) + totalAnalysisCount(그 중 기간 내 분석 총건수)
    @Transactional(readOnly = true)
    public AdminAnalysisComboResultDto searchAnalysisCombo(AdminAnalysisComboFilterRequest filter) {
        // ① 데이터 가져오는 부분 (main_db 필터링) — AdminStatisticsMapper.selectMemberCandidates
        List<String> candidateUserIds = adminStatisticsMapper.selectMemberCandidates(
                filter.subscribe(), filter.role(), filter.job(), filter.limit());

        if (candidateUserIds.isEmpty()) {
            return new AdminAnalysisComboResultDto(0, 0);
        }

        // ① 데이터 가져오는 부분 (ai-api 호출) — 아래 fetchAnalysisCountForUsers()
        int totalAnalysisCount = fetchAnalysisCountForUsers(filter.range(), candidateUserIds);

        // ② main_db가 이미 조건에 맞는 유저만 골라놨기 때문에, 여기서는 별도 join/합산 없이
        // 후보 수와 ai-api가 돌려준 총건수를 그대로 묶어서 React로 보냄
        return new AdminAnalysisComboResultDto(candidateUserIds.size(), totalAnalysisCount);
    }

    // ai-api POST /api/admin/analysis/count-for-users 호출 — 이미 추려진 userIds에 대한 분석 총건수만 요청
    private int fetchAnalysisCountForUsers(String range, List<String> userIds) {
        try {
            AnalysisCountResultDto result = aiApiClient.post()
                    .uri("/api/admin/analysis/count-for-users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CountForUsersRequestDto(range, userIds))
                    .retrieve()
                    .body(AnalysisCountResultDto.class);
            return result != null ? result.getTotalCount() : 0;
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), "AI 분석 서버 요청이 올바르지 않습니다.");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결할 수 없습니다.");
        }
    }
}
