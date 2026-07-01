package com.sanavi.backend.admin.statistics.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sanavi.backend.admin.statistics.dto.AdminAnalysisComboFilterRequest;
import com.sanavi.backend.admin.statistics.dto.AdminAnalysisComboResultDto;
import com.sanavi.backend.admin.statistics.dto.AnalysisCountResultDto;
import com.sanavi.backend.admin.statistics.dto.CountForUsersRequestDto;
import com.sanavi.backend.admin.statistics.mapper.AdminStatisticsMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// searchAnalysisCombo()의 "유저를 먼저 추리고(main_db) → ai-api에 그 유저만 물어본다" 흐름만 검증하는
// 순수 단위테스트 — 실제 DB/Redis/ai-api 연결 없이 Mockito로 두 데이터 소스를 흉내냄
class AdminStatisticsServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void searchAnalysisCombo_후보유저를_추린뒤_ai_api에서_분석횟수를_받아온다() {
        // given: main_db에서 필터(구독여부=Pro, 유저타입=일반유저)로 추려진 후보 3명
        AdminStatisticsMapper mapper = mock(AdminStatisticsMapper.class);
        when(mapper.selectMemberCandidates(1, "role_user", null, 100))
                .thenReturn(List.of("user1", "user2", "user3"));

        // given: ai-api가 그 3명에 대해 돌려주는 기간 내 분석 총건수 (가짜)
        RestClient aiApiClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(aiApiClient.post()
                .uri("/api/admin/analysis/count-for-users")
                .contentType(any())
                .body(any(CountForUsersRequestDto.class))
                .retrieve()
                .body(AnalysisCountResultDto.class))
                .thenReturn(new AnalysisCountResultDto(7));

        AdminStatisticsService service = new AdminStatisticsService(mapper, aiApiClient);
        AdminAnalysisComboFilterRequest filter =
                new AdminAnalysisComboFilterRequest("week", 1, "role_user", null, 100);

        // when
        AdminAnalysisComboResultDto result = service.searchAnalysisCombo(filter);

        // then: 후보 3명(sampleSize) 중 이번 기간에 총 7건(totalAnalysisCount) 분석
        assertThat(result.getSampleSize()).isEqualTo(3);
        assertThat(result.getTotalAnalysisCount()).isEqualTo(7);
    }

    @Test
    void searchAnalysisCombo_필터에_맞는_후보가_없으면_ai_api를_호출하지_않는다() {
        // given: main_db 필터 조건에 맞는 유저가 한 명도 없음
        AdminStatisticsMapper mapper = mock(AdminStatisticsMapper.class);
        when(mapper.selectMemberCandidates(0, "role_lawyer", "간호사", 500)).thenReturn(List.of());

        RestClient aiApiClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        AdminStatisticsService service = new AdminStatisticsService(mapper, aiApiClient);
        AdminAnalysisComboFilterRequest filter =
                new AdminAnalysisComboFilterRequest("month", 0, "role_lawyer", "간호사", 500);

        // when
        AdminAnalysisComboResultDto result = service.searchAnalysisCombo(filter);

        // then: 0/0으로 바로 반환 — 빈 후보 목록으로 ai-api를 부르는 낭비 호출이 없어야 함
        assertThat(result.getSampleSize()).isZero();
        assertThat(result.getTotalAnalysisCount()).isZero();
        verify(aiApiClient, never()).post();
    }
}
