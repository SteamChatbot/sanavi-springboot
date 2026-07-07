package com.sanavi.backend.common.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.sanavi.backend.admin.system.dto.LogEntryDto;

// 책임: 최근 로그를 메모리에 고정 개수만큼만 들고 있는 정적 링버퍼
//       InMemoryLogAppender(Logback)가 write, AdminSystemController(Spring)가 read
//       Logback은 Spring 컨텍스트보다 먼저 초기화되므로 DI 대신 정적 접근으로 연결
public final class LogRingBuffer {

    private static final int CAPACITY = 500;
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));

    private static final Deque<LogEntryDto> buffer = new ArrayDeque<>(CAPACITY);

    private LogRingBuffer() {
    }

    public static synchronized void add(long epochMilli, String level, String logger, String message,
                                         String traceId, String clientIp, String userId, String handler, String duration) {
        if (buffer.size() >= CAPACITY) {
            buffer.removeFirst();
        }
        buffer.addLast(new LogEntryDto(
                TS_FMT.format(Instant.ofEpochMilli(epochMilli)), level, logger, message, traceId, clientIp, userId, handler, duration));
    }

    // Output: 최신순(내림차순) 최대 limit건 — level은 완전일치, userId/handler는 대소문자 무시 부분일치
    public static synchronized List<LogEntryDto> getRecent(int limit, String levelFilter, String userIdFilter, String handlerFilter) {
        List<LogEntryDto> result = new ArrayList<>();
        var it = buffer.descendingIterator();
        while (it.hasNext() && result.size() < limit) {
            LogEntryDto entry = it.next();
            if (matchesLevel(entry, levelFilter) && matchesContains(entry.userId(), userIdFilter)
                    && matchesContains(entry.handler(), handlerFilter)) {
                result.add(entry);
            }
        }
        return result;
    }

    private static boolean matchesLevel(LogEntryDto entry, String levelFilter) {
        return levelFilter == null || levelFilter.isBlank() || entry.level().equalsIgnoreCase(levelFilter);
    }

    private static boolean matchesContains(String value, String filter) {
        return filter == null || filter.isBlank() || (value != null && value.toLowerCase().contains(filter.toLowerCase()));
    }
}
