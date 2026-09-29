package kr.sesac.wordcounter;

public class AnalysisSummary {
    static String lastInputPath = null;
    static int lastAttemptCount = 0;
    static int lastSuccessCount = 0;
    static int lastFailCount = 0;
    static int lastSkippedCount = 0;
    static long lastTotalWords = 0;
    static long lastDistinctWords = 0;
    static long lastElapsedNanos = 0;
    static boolean hasUsableResult = false;

    static void printSummary() {
        if (lastInputPath == null) {
            System.out.println("먼저 분석을 시작하세요.");
            return;
        }
        System.out.println("입력: " + lastInputPath);
        System.out.println("파일: 시도 " + lastAttemptCount + "개 / 성공 " + lastSuccessCount
                + "개 / 실패 " + lastFailCount + "개 / 지원하지 않아 건너뜀 " + lastSkippedCount + "개");
        System.out.println("전체 단어: " + lastTotalWords + "개 / 서로 다른 단어: " + lastDistinctWords + "개");
        double ms = lastElapsedNanos / 1_000_000.0;
        System.out.printf("처리 시간: %.1fms%n", ms);
    }

}
