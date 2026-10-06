package kr.sesac.wordcounter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileProcessor {
    // 확장자로 지원 여부 판단
    public static boolean isSupported(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();

        return fileName.endsWith(".txt")
                || fileName.endsWith(".csv")
                || fileName.endsWith(".tsv")
                || fileName.endsWith(".html")
                || fileName.endsWith(".htm");
    }

    public static void processFile(Path input, Map<String, Long> counts) throws IOException {
        String fileName = input.getFileName().toString().toLowerCase();

        try {
            if (fileName.endsWith(".txt")) {
                processTxt(input, counts);
            } else if (fileName.endsWith(".tsv")) {
                processTsv(input, counts);
            } else if (fileName.endsWith(".csv")) {
                processCsv(input, counts);
            } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                processHtml(input, counts);
            } else {
                throw new IOException("지원하지 않는 형식입니다: " + input);
            }
        } catch (UncheckedIOException e) {
            String detail;
            if (e.getCause() != null) {
                detail = e.getCause().getMessage();
            } else {
                detail = e.getMessage();
            }
            throw new IOException("형식이 올바르지 않습니다: " + detail, e);
        /*
         * 문제점 : 헤더 이름이 비어 있는 CSV 등에서 던지는 IllegalArgumentException을 처리하지 않음.
         * 원인 : IOException, UncheckedIOException만 변환하고 파서가 던지는 IllegalArgumentException은 놓침.
         *        Main은 IOException만 잡으므로 예외가 전파되어 프로그램이 종료됨.
         * 수정자 : 정유진
         */
        } catch (IllegalArgumentException e) {
            throw new IOException("형식이 올바르지 않습니다: " + e.getMessage(), e);
        }
    }

    private static void processTxt(Path input, Map<String, Long> counts) throws IOException {
        try (BufferedReader reader =
                     Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                WordCounter.countWords(line, counts);
            }
        }
    }

    private static void processCsv(Path input, Map<String, Long> counts) throws IOException {
        processDelimitedFile(input, counts, ',', true, List.of("Q", "A"));
    }

    private static void processTsv(Path input, Map<String, Long> counts) throws IOException {
        processDelimitedFile(input, counts, '\t', false, List.of("document"));
    }

    /*
     * 문제점 : processCsv와 processTsv가 헤더 검증, 셀 수 검사, 순회 구조까지 거의 같은 코드를 중복함.
     * 원인 : 구분자, 따옴표 사용 여부, 분석 열만 다른데도 형식별로 메서드를 따로 구현함.
     * 수정자 : 정유진
     */
    private static void processDelimitedFile(Path input, Map<String, Long> counts,
                                             char delimiter, boolean useQuote,
                                             List<String> columns) throws IOException {
        var builder = CSVFormat.RFC4180.builder()
                .setHeader() // 첫 번째 줄을 헤더로
                .setSkipHeaderRecord(true) // 헤더 데이터로 처리x
                .setDelimiter(delimiter);
        if (!useQuote) {
            builder.setQuote(null);
        }
        var format = builder.get();

        try (var reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            Map<String, Integer> rawHeader = parser.getHeaderMap();
            if (rawHeader == null) {
                throw new IOException("헤더를 읽을 수 없습니다.");
            }

            /*
             * 문제점 : 헤더 이름에 앞뒤 공백이 있으면(예: "Q, A") 열이 있어도 없다고 판단함.
             * 원인 : 헤더 이름을 그대로 비교함. 요구사항은 앞뒤 공백을 제거해 비교하도록 정함.
             * 수정자 : 정유진
             */
            Map<String, Integer> header = new HashMap<>();
            for (Map.Entry<String, Integer> entry : rawHeader.entrySet()) {
                header.put(entry.getKey().trim(), entry.getValue());
            }

            for (String column : columns) {
                if (!header.containsKey(column)) {
                    throw new IOException(column + " 열이 없습니다.");
                }
            }

            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    throw new IOException("셀 수가 헤더와 다릅니다. (" + record.getRecordNumber() + "번째 레코드)");
                }
                for (String column : columns) {
                    WordCounter.countWords(record.get(header.get(column)), counts);
                }
            }
        }
    }

    private static void processHtml(Path input, Map<String, Long> counts) throws IOException {
        String html = Files.readString(input, StandardCharsets.UTF_8);
        Document document = Jsoup.parse(html);

        Elements matches = document.select("#content");
        if (matches.size() != 1) {
            throw new IOException("본문 요소는 정확히 하나여야 합니다.");
        }
        Element content = matches.first();
        content.select("script, style, nav, header, footer").remove();
        WordCounter.countWords(content.text(), counts);
    }

}