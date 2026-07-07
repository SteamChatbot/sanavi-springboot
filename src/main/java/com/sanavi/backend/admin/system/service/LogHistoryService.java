package com.sanavi.backend.admin.system.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;

import com.sanavi.backend.admin.system.dto.DailyLogCountDto;
import com.sanavi.backend.admin.system.dto.LogEntryDto;
import com.sanavi.backend.admin.system.dto.LogExportDto;
import com.sanavi.backend.common.service.S3Service;


// 책임: S3에 저장된 과거 로그(NDJSON, S3LogAppender가 적재)를 Athena로 SQL 조회
// 아래 Glue 테이블은 AWS 콘솔에서 이미 생성 완료(2026-07-03) — 새 환경(다른 AWS 계정 등)에 옮길 때 참고용 DDL:
//   CREATE DATABASE IF NOT EXISTS sanavi_logs;
//   CREATE EXTERNAL TABLE IF NOT EXISTS sanavi_logs.sanavi_backend_logs (
//     `timestamp` string, traceId string, clientIp string,
//     level string, logger string, message string,
//     userId string, handler string, duration string
//   )
//   PARTITIONED BY (year string, month string, day string)
//   ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'  -- Athena 기본 내장
//   LOCATION 's3://sanavi-dev-files/log/sanavi-backend/'
//   TBLPROPERTIES (
//     'projection.enabled'='true',
//     'projection.year.type'='integer', 'projection.year.range'='2026,2035',
//     'projection.month.type'='integer', 'projection.month.range'='1,12', 'projection.month.digits'='2',
//     'projection.day.type'='integer', 'projection.day.range'='1,31', 'projection.day.digits'='2',
//     'storage.location.template'='s3://sanavi-dev-files/log/sanavi-backend/year=${year}/month=${month}/day=${day}/'
//   );
// 파티션 프로젝션을 쓰므로 Glue 크롤러/MSCK REPAIR TABLE 불필요 — 경로 규칙이 이미 예측 가능한 패턴이라 바로 조회 가능.
// Athena workgroup의 쿼리 결과 출력 위치는 s3://sanavi-dev-files/athena-results/ 로 설정 완료(SSE-S3 암호화 적용).
// 아래 실패 로그들은 실제로 겪었던 이슈(IAM 권한 부족, output location 미설정, 버킷 불일치)를 재현 시 바로
// 원인 파악할 수 있게 남겨둠 — AWS SDK 예외 메시지가 LoggingAspect의 일반 로그(exception=클래스명)보다 훨씬 구체적임.
@Slf4j
@Service
@RequiredArgsConstructor
public class LogHistoryService {

    private static final int MAX_RESULTS = 500;
    private static final int MAX_DAYS_BACK = 7;
    private static final long POLL_INTERVAL_MS = 300;//0.3초
    private static final long POLL_TIMEOUT_MS = 15_000;//15초
    private static final Set<String> VALID_LEVELS = Set.of("ERROR", "WARN", "INFO", "DEBUG", "TRACE");

    private final AthenaClient athenaClient;
    private final S3Service s3Service;

    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    @Value("${aws.athena.database}")
    private String database;

    @Value("${aws.athena.table}")
    private String table;

    @Value("${aws.athena.output-location}")
    private String outputLocation;

    public List<LogEntryDto> query(String dateStr, String hour, String level, String userId, String handler) {
        LocalDate date = parseAndValidateDate(dateStr);
        String validHour = validateHour(hour);
        String validLevel = validateLevel(level);

        String sql = buildSql(date, validHour, validLevel, userId, handler);
        List<Row> rows = runQuery(sql);

        List<LogEntryDto> entries = new ArrayList<>();
        for (Row row : rows) {
            List<Datum> data = row.data();
            entries.add(new LogEntryDto(
                    value(data, 0), value(data, 1), value(data, 2), value(data, 3),
                    value(data, 4), value(data, 5), value(data, 6), value(data, 7), value(data, 8)
            ));
        }
        return entries;
    }

    // 관리자 시스템 모니터링 — "일별 로그 발생 추이" 차트용. 최근 7일을 파티션(year/month/day) 조건으로 OR 묶어서
    // 하루치 전체를 내려받아 Java에서 세는 대신 Athena에서 GROUP BY로 바로 집계 — 스캔 대상이 7개 day 파티션으로 제한돼 비용이 작음.
    public List<DailyLogCountDto> getDailyTrend() {
        List<LocalDate> days = new ArrayList<>();
        for (int i = MAX_DAYS_BACK - 1; i >= 0; i--) {
            days.add(LocalDate.now().minusDays(i));
        }

        String sql = buildTrendSql(days);
        List<Row> rows = runQuery(sql);

        // date(yyyy-MM-dd) -> level -> count
        Map<String, Map<String, Long>> counts = new LinkedHashMap<>();
        for (LocalDate day : days) {
            counts.put(day.format(DateTimeFormatter.ISO_LOCAL_DATE), new LinkedHashMap<>());
        }
        for (Row row : rows) {
            List<Datum> data = row.data();
            String date = value(data, 0) + "-" + value(data, 1) + "-" + value(data, 2);
            String level = value(data, 3);
            long cnt = Long.parseLong(value(data, 4));
            counts.computeIfAbsent(date, k -> new LinkedHashMap<>()).put(level, cnt);
        }

        List<DailyLogCountDto> result = new ArrayList<>();
        for (LocalDate day : days) {
            String date = day.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Map<String, Long> byLevel = counts.getOrDefault(date, Map.of());
            long error = byLevel.getOrDefault("ERROR", 0L);
            long warn = byLevel.getOrDefault("WARN", 0L);
            long info = byLevel.getOrDefault("INFO", 0L);
            long total = byLevel.values().stream().mapToLong(Long::longValue).sum(); // DEBUG/TRACE 등 포함 실제 총합
            result.add(new DailyLogCountDto(date, error, warn, info, total));
        }
        return result;
    }

    private String buildTrendSql(List<LocalDate> days) {
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            LocalDate d = days.get(i);
            if (i > 0) where.append(" OR ");
            where.append("(year='").append(d.getYear())
                    .append("' AND month='").append(String.format("%02d", d.getMonthValue()))
                    .append("' AND day='").append(String.format("%02d", d.getDayOfMonth())).append("')");
        }

        return "SELECT year, month, day, level, COUNT(*) AS cnt FROM " + database + '.' + table
                + " WHERE (" + where + ") GROUP BY year, month, day, level";
    }

    private String buildSql(LocalDate date, String hour, String level, String userId, String handler) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT \"timestamp\", level, logger, message, traceId, clientIp, userId, handler, duration FROM ")
                .append(database).append('.').append(table)
                .append(" WHERE year='").append(date.getYear())
                .append("' AND month='").append(String.format("%02d", date.getMonthValue()))
                .append("' AND day='").append(String.format("%02d", date.getDayOfMonth())).append('\'');

        if (hour != null) {
            sql.append(" AND substr(\"timestamp\", 12, 2) = '").append(hour).append('\'');
        }
        if (level != null) {
            sql.append(" AND level = '").append(level).append('\'');
        }
        if (userId != null && !userId.isBlank()) {
            sql.append(" AND lower(userId) LIKE '%").append(escapeSqlLiteral(userId.toLowerCase())).append("%'");
        }
        if (handler != null && !handler.isBlank()) {
            sql.append(" AND lower(handler) LIKE '%").append(escapeSqlLiteral(handler.toLowerCase())).append("%'");
        }

        sql.append(" ORDER BY \"timestamp\" DESC LIMIT ").append(MAX_RESULTS);
        return sql.toString();
    }

    // 과거 로그 CSV export — Athena는 쿼리 실행 시 결과를 항상 output location에 CSV로 남기므로,
    // JSON으로 재변환하지 않고 그 CSV 파일 자체를 presigned URL로 내려주는 방식(게시판 첨부파일 다운로드와 동일한 S3Service 재사용)
    public LogExportDto exportCsv(String dateStr, String hour, String level, String userId, String handler) {
        LocalDate date = parseAndValidateDate(dateStr);
        String validHour = validateHour(hour);
        String validLevel = validateLevel(level);

        String sql = buildSql(date, validHour, validLevel, userId, handler);
        String queryExecutionId = startQuery(sql);
        waitForCompletion(queryExecutionId);

        String resultS3Uri = athenaClient.getQueryExecution(
                        GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build())
                .queryExecution().resultConfiguration().outputLocation();

        String key = extractS3Key(resultS3Uri);
        return new LogExportDto(s3Service.generatePresignedUrl(key, 30));
    }

    // "s3://sanavi-dev-files/athena-results/<queryExecutionId>.csv" -> "athena-results/<queryExecutionId>.csv"
    // S3Service가 자기 자신의 aws.s3.bucket 버킷 기준으로만 presigned URL을 발급하므로, Athena 결과 버킷이 그것과 다르면 명확히 에러 처리
    private String extractS3Key(String s3Uri) {
        String withoutScheme = s3Uri.replaceFirst("^s3://", "");
        int slash = withoutScheme.indexOf('/');
        String bucket = withoutScheme.substring(0, slash);
        if (!bucket.equals(s3Bucket)) {
            // 코드 버그가 아니라 workgroup 콘솔 설정이 잘못된 경우라 관리자가 고칠 수 있는 설정 이슈 — WARN
            log.warn("action=ATHENA_RESULT_BUCKET_MISMATCH result=FAIL expected_bucket={} actual_bucket={}",
                    s3Bucket, bucket);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Athena 쿼리결과 버킷(" + bucket + ")이 설정된 S3 버킷(" + s3Bucket + ")과 다릅니다. "
                            + "Athena workgroup의 쿼리 결과 출력 위치를 " + s3Bucket + " 버킷 안으로 다시 설정해주세요.");
        }
        return withoutScheme.substring(slash + 1);
    }

    // 작은따옴표만 이스케이프 — database/table은 설정값(사용자 입력 아님), year/month/day/hour/level은 이미 검증된 값이라 별도 escape 불필요.
    // userId/handler는 HTTP 요청 파라미터를 그대로 SQL 문자열에 넣으므로 반드시 escape.
    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private String startQuery(String sql) {
        // output-location을 안 정해뒀으면(application.yml 기본값 "") resultConfiguration을 아예 안 넘김 —
        // 그러면 Athena가 workgroup(기본 "primary")에 콘솔에서 미리 설정해둔 기본 쿼리결과 위치를 그대로 씀.
        // 즉 로컬에서 별도 env var 없이도, AWS 콘솔에서 (1) Glue 테이블 (2) workgroup 기본 출력 위치만 한 번 설정해두면
        // 로컬 백엔드(실제 AWS 자격증명 사용 중)가 바로 조회 가능 — Athena는 프로필과 무관하게 항상 실제 AWS를 호출함.
        StartQueryExecutionRequest.Builder requestBuilder = StartQueryExecutionRequest.builder()
                .queryString(sql)
                .queryExecutionContext(QueryExecutionContext.builder().database(database).build());
        if (outputLocation != null && !outputLocation.isBlank()) {
            requestBuilder.resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build());
        }
        try {
            return athenaClient.startQueryExecution(requestBuilder.build()).queryExecutionId();
        } catch (RuntimeException e) {
            // IAM 권한 부족, output location 미설정 등 — 코드 버그가 아니라 AWS 설정 문제일 확률이 높은 실패라 ERROR
            log.error("action=ATHENA_QUERY_START result=FAIL reason=START_QUERY_FAILED exception={} message={}",
                    e.getClass().getSimpleName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Athena 쿼리 시작에 실패했습니다: " + e.getMessage());
        }
    }

    private void waitForCompletion(String queryExecutionId) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        QueryExecutionState state;
        try {
            while (true) {
                state = athenaClient.getQueryExecution(
                                GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build())
                        .queryExecution().status().state();

                if (state == QueryExecutionState.SUCCEEDED) {
                    return;
                }
                if (state == QueryExecutionState.FAILED || state == QueryExecutionState.CANCELLED) {
                    // 쿼리가 시작은 됐지만 실행 중 실패 — 보통 SQL/테이블 문제(예: Glue 테이블 미생성)라 ERROR
                    log.error("action=ATHENA_QUERY_EXECUTE result=FAIL reason=QUERY_STATE_{} query_execution_id={}",
                            state, queryExecutionId);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Athena 쿼리 실행 실패: " + state);
                }
                if (System.currentTimeMillis() > deadline) {
                    // 타임아웃은 재시도하면 성공할 수도 있는 일시적 상황이라 WARN
                    log.warn("action=ATHENA_QUERY_EXECUTE result=TIMEOUT query_execution_id={} timeout_ms={}",
                            queryExecutionId, POLL_TIMEOUT_MS);
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "로그 조회가 시간 초과되었습니다.");
                }
                Thread.sleep(POLL_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("action=ATHENA_QUERY_EXECUTE result=INTERRUPTED query_execution_id={}", queryExecutionId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "로그 조회가 중단되었습니다.");
        }
    }

    // startQuery + waitForCompletion + 결과조회를 한 번에 — 반환값은 헤더 행(0번째)을 제외한 실데이터 행만
    private List<Row> runQuery(String sql) {
        String queryExecutionId = startQuery(sql);
        waitForCompletion(queryExecutionId);

        List<Row> rows = athenaClient.getQueryResults(
                        GetQueryResultsRequest.builder().queryExecutionId(queryExecutionId).build()).resultSet().rows();
        return rows.isEmpty() ? rows : rows.subList(1, rows.size());
    }

    private String value(List<Datum> data, int index) {
        if (index >= data.size()) return null;
        return data.get(index).varCharValue();
    }

    private LocalDate parseAndValidateDate(String dateStr) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date는 yyyy-MM-dd 형식이어야 합니다.");
        }

        LocalDate today = LocalDate.now();
        if (date.isAfter(today) || date.isBefore(today.minusDays(MAX_DAYS_BACK))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date는 오늘로부터 최근 " + MAX_DAYS_BACK + "일 이내여야 합니다.");
        }
        return date;
    }

    private String validateHour(String hour) {
        if (hour == null || hour.isBlank()) return null;
        if (!hour.matches("\\d{1,2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hour는 0~23 숫자여야 합니다.");
        }
        int h = Integer.parseInt(hour);
        if (h < 0 || h > 23) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hour는 0~23 숫자여야 합니다.");
        }
        return String.format("%02d", h);
    }

    private String validateLevel(String level) {
        if (level == null || level.isBlank()) return null;
        String upper = level.toUpperCase();
        if (!VALID_LEVELS.contains(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "level은 " + VALID_LEVELS + " 중 하나여야 합니다.");
        }
        return upper;
    }
}
