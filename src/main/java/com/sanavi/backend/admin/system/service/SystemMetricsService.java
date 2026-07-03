package com.sanavi.backend.admin.system.service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sanavi.backend.admin.system.dto.SystemMetricsDto;

// 책임: JVM/OS MXBean + /proc/net/dev(Linux)로 서버 하드웨어 리소스 스냅샷을 만든다
// 네트워크 대역폭은 순수 Java로 OS 독립적으로 구할 수 없어 Linux 전용(EC2 배포 기준)으로만 지원 — 로컬 Windows 개발환경에서는 networkSupported=false
//
// ↓↓↓ 이 서비스에서 실제로 OS별 분기가 필요한 부분은 네트워크(아래) 하나뿐 — CPU/메모리/디스크는 코드 변경 불필요(위 snapshot() 주석 참고).
// EC2(Linux)로 배포하면 /proc/net/dev 파일이 실제로 존재하므로 아래 Files.exists() 체크가 자동으로 true가 되고
// 별도 설정/코드 수정 없이 네트워크 게이지가 바로 동작함 — os.name 같은 문자열 비교가 아니라 파일 존재 여부로 판단하기 때문.
@Service
public class SystemMetricsService {

    private static final Path PROC_NET_DEV = Paths.get("/proc/net/dev");

    private final com.sun.management.OperatingSystemMXBean osBean =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final boolean networkSupported = Files.exists(PROC_NET_DEV);

    // 이전 호출 시점의 누적 바이트 수 — 다음 호출과의 델타로 순간 전송률(MB/s) 계산
    private volatile long prevRxBytes = -1;
    private volatile long prevTxBytes = -1;
    private volatile long prevTimestampNanos = -1;

    public SystemMetricsDto snapshot() {
        // CPU/메모리는 JVM이 OS 차이를 흡수해줘서 EC2(Linux) 배포 시에도 코드 변경 불필요.
        // 단, getSystemLoadAverage()는 Windows에서 -1을 반환(미지원)하고 Linux에서만 실제 값(/proc/loadavg 기반)이 나옴 —
        // 로컬(Windows)에서 -1이 찍히는 건 버그가 아니라 OS 차이이며, 프론트가 이미 `>= 0` 체크로 처리 중(AdminSystemPage.jsx).
        double cpuPercent = Math.max(0, osBean.getCpuLoad() * 100);
        long memoryTotal = osBean.getTotalMemorySize();
        long memoryFree = osBean.getFreeMemorySize();

        // new File("/")는 OS별로 의미가 다름 — Windows에서는 "현재 드라이브 루트", Linux(EC2)에서는 "루트 파티션".
        // 둘 다 코드 변경 없이 동작은 하지만, EC2에 루트 볼륨과 별도로 EBS 볼륨을 추가 마운트해서 로그/데이터를 거기 저장하게 되면
        // 이 경로가 그 볼륨을 반영하지 못함 — 그때는 여기를 실제 데이터 마운트 경로로 바꿔야 함(지금은 해당 없음).
        File root = new File("/");
        long diskTotal = root.getTotalSpace();
        long diskUsed = diskTotal - root.getUsableSpace();

        Double rxMBps = null;
        Double txMBps = null;
        if (networkSupported) {
            long[] rates = readNetworkRates();
            rxMBps = rates == null ? null : rates[0] / 1_000_000.0;
            txMBps = rates == null ? null : rates[1] / 1_000_000.0;
        }

        return new SystemMetricsDto(
                cpuPercent,
                osBean.getAvailableProcessors(),
                osBean.getSystemLoadAverage(),
                memoryTotal - memoryFree,
                memoryTotal,
                diskUsed,
                diskTotal,
                rxMBps,
                txMBps,
                networkSupported
        );
    }

    // Output: [rxBytesPerSec, txBytesPerSec], 첫 호출(비교 기준 없음)이면 null
    private synchronized long[] readNetworkRates() {
        long[] totals = sumProcNetDev();
        if (totals == null) {
            return null;
        }

        long now = System.nanoTime();
        long[] result = null;
        if (prevTimestampNanos > 0) {
            double elapsedSec = (now - prevTimestampNanos) / 1_000_000_000.0;
            if (elapsedSec > 0 && totals[0] >= prevRxBytes && totals[1] >= prevTxBytes) {
                result = new long[]{
                        (long) ((totals[0] - prevRxBytes) / elapsedSec),
                        (long) ((totals[1] - prevTxBytes) / elapsedSec)
                };
            }
        }

        prevRxBytes = totals[0];
        prevTxBytes = totals[1];
        prevTimestampNanos = now;
        return result;
    }

    // /proc/net/dev 형식: "  eth0: <rxBytes> ... (8 more cols) <txBytes> ..." — lo(루프백)는 제외하고 전체 인터페이스 합산
    private long[] sumProcNetDev() {
        try {
            List<String> lines = Files.readAllLines(PROC_NET_DEV);
            long rx = 0;
            long tx = 0;
            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String iface = line.substring(0, colon).trim();
                if ("lo".equals(iface)) continue;

                String[] fields = line.substring(colon + 1).trim().split("\\s+");
                if (fields.length < 9) continue;
                rx += Long.parseLong(fields[0]);
                tx += Long.parseLong(fields[8]);
            }
            return new long[]{rx, tx};
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }
}
