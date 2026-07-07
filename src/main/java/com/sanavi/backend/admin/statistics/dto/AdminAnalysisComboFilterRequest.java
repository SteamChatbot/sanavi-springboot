package com.sanavi.backend.admin.statistics.dto;

// 관리자 다중필터 조합 검색 조건 — 검색 버튼 클릭 시 날짜범위·구독여부·유저타입·직업·후보상한을
// 한 객체로 묶어 컨트롤러에서 서비스로 전달 (개별 파라미터를 여러 개 넘기지 않기 위함)
public record AdminAnalysisComboFilterRequest(
        String range,       // "week"(최근7일, 기본값) | "month"(1개월) | "halfyear"(6개월)
        Integer subscribe,  // null=전체, 0=Basic, 1=Pro
        String role,        // null=전체, "role_user", "role_lawyer"
        String job,         // null/blank=전체, 부분일치 키워드 (직업 TOP N 목록에서 선택된 단어)
        int limit           // main_db에서 추릴 후보 유저 수 상한 (0 이하면 무제한="전체")
) {
}
