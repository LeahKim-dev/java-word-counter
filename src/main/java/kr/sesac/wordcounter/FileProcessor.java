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
        var format = CSVFormat.RFC4180.builder()
                .setHeader() // 첫 번째 줄을 헤더로
                .setSkipHeaderRecord(true) // 헤더 데이터로 처리x
                .get();

        try (
            var reader = Files.newBufferedReader(input, StandardCharsets.UTF_8); // input 파일을 UTF-8로 읽기 위한 Reader
            CSVParser parser = format.parse(reader) // reader를 CSVParser로 변환
        ) { // 열이 있는지 확인
            Map<String, Integer> header = parser.getHeaderMap();
            if (header == null || !header.containsKey("text")) {
//            if (header == null || !header.containsKey("Q")|| !header.containsKey("A")){
                throw new IOException("text 열이 없습니다.");
            }

            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    throw new IOException("셀 수가 헤더와 다릅니다. (" + record.getRecordNumber() + "번째 레코드)");
                }
                String text = record.get("text");
                WordCounter.countWords(text, counts);
//                WordCounter.countWords(record.get("Q"), counts);
//                WordCounter.countWords(record.get("A"), counts);
            }
        }
    }

    private static void processTsv(Path input, Map<String, Long> counts) throws IOException {
        var format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDelimiter('\t') // 열을 탭으로 구분
                .setQuote(null)
                .get();

        try (var reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            Map<String, Integer> header = parser.getHeaderMap();
            if (header == null || !header.containsKey("document")) {
                throw new IOException("document 열이 없습니다.");
            }

            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    throw new IOException("셀 수가 헤더와 다릅니다. (" + record.getRecordNumber() + "번째 레코드)");
                }
                String document = record.get("document");
                WordCounter.countWords(document, counts);
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