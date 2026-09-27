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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
        Path inputDir = Path.of("samples/equivalent");

        Map<String, Integer> counts = new HashMap<>();

        try (var paths = Files.list(inputDir)) {
            for (Path path : paths.toList()) {
                if (!Files.isRegularFile(path)) { // 파일 아닌것 처리
                    continue;
                }

                System.out.println("입력 파일: " + path);
                System.out.println();

                Map<String, Integer> fileCounts = new HashMap<>(); // 임시 Map
                try {
                    processFile(path, fileCounts);

                    // 합치기
                    for (Map.Entry<String, Integer> entry : fileCounts.entrySet()) {
                        counts.merge(
                                entry.getKey(),
                                entry.getValue(),
                                Integer::sum // (oldValue, newValue) -> Integer.sum(oldValue, newValue)
                        );
                    }
                }
                catch (IOException e) {
                    System.out.println("처리 실패: " + path);
                }
            }
        }

        System.out.println("word\tcount");
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
    }

    private static void processFile(Path input, Map<String, Integer> counts) throws IOException {
        // 0. 파일 형식 구분
        String fileName = input.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".txt")) {
            processTxt(input, counts);
        } else if (fileName.endsWith(".tsv")) {
            processTsv(input, counts);
        } else if (fileName.endsWith(".csv")) {
            processCsv(input, counts);
        } else if (fileName.endsWith(".html")) {
            processHtml(input, counts);
        } else {
            throw new IOException("지원하지 않는 형식입니다." + input); // 실패한 파일 확인용
        }
    }

    // 1. txt
    private static void processTxt(Path input, Map<String, Integer> counts) throws IOException {
        try (BufferedReader reader =
                     Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                countWords(line, counts);
            }
        }
    }

    // 2. csv
    private static void processCsv(Path input, Map<String, Integer> counts) throws IOException {
        // var: 오른쪽 값을 보고 컴파일러가 변수 타입을 알아서 정하게 하는 것
        var format = CSVFormat.RFC4180.builder() // 표준 CSV형식으로 읽기
                .setHeader() // 첫 번째 줄을 컬럼 이름으로
                .setSkipHeaderRecord(true) // 첫 번째 줄 데이터로 처리하지 않게
                // tsv 확장 시 .setDelimiter('\t')와 .setQuote(null) 추가
                .get();

        try (var reader = Files.newBufferedReader( // try-with-resources
                input, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {
                String text = record.get("text");
                countWords(text, counts);
            }
        }
    }

    // 3. tsv
    private static void processTsv(Path input, Map<String, Integer> counts) throws IOException {
        var format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDelimiter('\t')
                .setQuote(null)
                .get();

        try (var reader = Files.newBufferedReader( // try-with-resources
                input, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {
                String document = record.get("document");
                countWords(document, counts);
            }
        }
    }

    // 4. html
    private static void processHtml(Path input, Map<String, Integer> counts) throws IOException {
        Document document = Jsoup.parse(
                Path.of("samples/equivalent/basic.html").toFile(), "UTF-8");
        Elements matches = document.select("#content");
        if (matches.size() != 1) {
            throw new IOException("본문 요소는 정확히 하나여야 합니다.");
        }
        Element content = matches.first();
        content.select("script, style, nav, header, footer").remove();
        countWords(content.text(), counts);
    }

    private static void countWords(String text, Map<String, Integer> counts) {
        Pattern pattern = Pattern.compile("[A-Za-z0-9가-힣ㄱ-ㅎㅏ-ㅣ]+");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String token = matcher.group();
            token = token.toLowerCase();

            if (!token.matches("[0-9]+")) {
                counts.put(
                        token,
                        counts.getOrDefault(token, 0) + 1
                );
            }
        }
    }
}