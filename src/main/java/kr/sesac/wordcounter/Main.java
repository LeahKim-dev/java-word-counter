package kr.sesac.wordcounter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        showMenu(scanner);
    }

    // 0. 메뉴
    private static void showMenu(Scanner scanner) {
        Map<String, Long> counts = new HashMap<>();

        while (true) {
            System.out.println();
            System.out.println("문서 단어 분석기");
            System.out.println("1. 새 분석 시작");
            System.out.println("2. 상위 N개 단어 조회");
            System.out.println("3. 특정 단어 조회");
            System.out.println("4. 전체 결과 저장");
            System.out.println("5. 최근 분석 요약 보기");
            System.out.println("0. 종료");
            System.out.print("선택 > ");

            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    counts = startNewAnalysis(scanner);
                    break;

                case "2":
                    showTopWords(scanner, counts);
                    break;

                case "3":
                    findWord(scanner, counts);
                    break;

                case "4":
                    saveCounts(counts);
                    break;

                case "5":
                    AnalysisSummary.printSummary();
                    break;

                case "0":
                    System.out.println("프로그램을 종료합니다.");
                    return;

                default:
                    System.out.println("잘못된 입력입니다.");
            }
        }
    }

    // 1. 새 분석 시작
    private static Map<String, Long> startNewAnalysis(Scanner scanner) {

        while (true) {
            System.out.print("파일 또는 폴더 경로 > ");
            String pathInput = scanner.nextLine().trim();

            if (pathInput.isEmpty()) {
                System.out.println("경로를 입력하세요.");
                continue;
            }

            Path inputPath;

            try {
                inputPath = Path.of(pathInput);
            } catch (InvalidPathException e) {
                    System.out.println("유효하지 않은 경로입니다. 다시 입력해주세요.");
                    continue;
            }

            if (!Files.exists(inputPath)) {
                System.out.println("경로를 찾을 수 없습니다: " + pathInput);
                continue;}

            List<Path> files = new ArrayList<>();
            int skippedCount = 0;

            if (Files.isRegularFile(inputPath)) {
                if (FileProcessor.isSupported(inputPath)) {
                    files.add(inputPath);
                } else {
                    System.out.println("지원하지 않는 파일입니다. 지원 확장자: .txt, .csv, .tsv, .html, .htm");
                    continue;
                }
            } else if (Files.isDirectory(inputPath)) {
                try (var paths = Files.list(inputPath)) {
                    for (Path path : paths.toList()) {
                        if (!Files.isRegularFile(path)) {
                            continue;
                        }
                        if (FileProcessor.isSupported(path)) {
                            files.add(path);
                        } else {
                            skippedCount++;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("경로를 읽을 수 없습니다: " + pathInput);
                    continue;
                }

                if (files.isEmpty()) {
                    System.out.println("지원 파일이 없습니다.");
                    continue;
                }
            }

            // 시간 측정 시작
            long startTime = System.nanoTime();

            Map<String, Long> totalCounts = new HashMap<>();
            int successCount = 0;
            int failCount = 0;

            for (Path file : files) {
                Map<String, Long> fileCounts = new HashMap<>();
                try {
                    FileProcessor.processFile(file, fileCounts); // 실패 시 안더함
                    WordCounter.mergeCounts(totalCounts, fileCounts);
                    successCount++;
                } catch (IOException e) {
                    System.out.println("실패: " + file + " (" + e.getMessage() + ")");
                    failCount++;
                }
            }

            long elapsedNanos = System.nanoTime() - startTime;
            // 시간 측정 끝

            long totalWords = 0;
            for (long count : totalCounts.values()) {
                totalWords += count;
            }

            // 요약 정보 저장
            AnalysisSummary.lastInputPath = pathInput;
            AnalysisSummary.lastAttemptCount = files.size();
            AnalysisSummary.lastSuccessCount = successCount;
            AnalysisSummary.lastFailCount = failCount;
            AnalysisSummary.lastSkippedCount = skippedCount;
            AnalysisSummary.lastTotalWords = totalWords;
            AnalysisSummary.lastElapsedNanos = elapsedNanos;
            AnalysisSummary.hasUsableResult = successCount > 0; // 성공한 파일이 하나라도 있어야 조회저장 가능
            AnalysisSummary.lastDistinctWords = totalCounts.size();

            System.out.println();
            System.out.println("분석 완료");
            AnalysisSummary.printSummary();

            return totalCounts;
        }
    }

    // 2. 상위 N개 단어 조회
    private static void showTopWords(Scanner scanner, Map<String, Long> counts) {
        if (!AnalysisSummary.hasUsableResult) {
            System.out.println("조회할 결과가 없습니다. 먼저 분석을 시작하세요.");
            return;
        }

        int n = 10; // 기본값

        while (true) {
            System.out.print("몇 개를 볼까요? (기본 10) > ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                n = 10;
                break;
            }

            try {
                int parsed = Integer.parseInt(line);
                if (parsed < 1) {
                    System.out.println("1 이상의 정수를 입력하세요.");
                    continue;
                }
                n = parsed;
                break;
            } catch (NumberFormatException e) {
                System.out.println("1 이상의 정수를 입력하세요.");
            }
        }

        List<Map.Entry<String, Long>> sorted = WordCounter.getSortedEntries(counts);

        int limit = Math.min(n, sorted.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            System.out.println((i + 1) + ". " + entry.getKey() + " : " + entry.getValue() + "회");
        }
    }

    // 3. 특정 단어 조회
    private static void findWord(Scanner scanner, Map<String, Long> counts) {
        if (!AnalysisSummary.hasUsableResult) {
            System.out.println("조회할 결과가 없습니다. 먼저 분석을 시작하세요.");
            return;
        }

        while (true) {
            System.out.print("찾을 단어 > ");
            String line = scanner.nextLine();

            List<String> tokens = WordCounter.extractTokens(line);

            if (tokens.size() != 1) {
                System.out.println("단어 하나를 입력하세요.");
                continue;
            }

            String word = tokens.get(0);
            long count = counts.getOrDefault(word, 0L);
            System.out.println(word + " : " + count + "회");
            return;
        }
    }

    // 4. 전체 결과 저장
    private static void saveCounts(Map<String, Long> counts) {
        if (!AnalysisSummary.hasUsableResult) {
            System.out.println("저장할 결과가 없습니다. 먼저 분석을 시작하세요.");
            return;
        }

        List<Map.Entry<String, Long>> sorted = WordCounter.getSortedEntries(counts);

        Path outDir = Path.of("out");
        Path outFile = outDir.resolve("counts.tsv");

        try {
            Files.createDirectories(outDir);

            try (var writer = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
                writer.write("word\tcount");
                writer.newLine();
                for (Map.Entry<String, Long> entry : sorted) {
                    writer.write(entry.getKey() + "\t" + entry.getValue());
                    writer.newLine();
                }
            }

            System.out.println("전체 결과 " + sorted.size() + "개 단어를 " + outFile + "에 저장했습니다.");

        } catch (IOException e) {
            System.out.println("저장에 실패했습니다: " + e.getMessage());
            // 저장 실패해도 counts는 그대로이므로, 계속 조회 가능
        }
    }


}