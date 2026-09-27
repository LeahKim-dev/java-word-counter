package kr.sesac.wordcounter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CsvPractice {
    public static void main(String[] args) throws IOException {
        var format = CSVFormat.RFC4180.builder() // var
                .setHeader() // 첫 레코드를 열 이름으로 읽음
                .setSkipHeaderRecord(true) // 헤더를 데이터로 처리하지 않게
                // tsv 확장 시 .setDelimiter('\t')와 .setQuote(null) 추가
                .get();

        try (var reader = Files.newBufferedReader( // try-with-resources
                Path.of("samples/equivalent/basic.csv"), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                System.out.println(record.get("text"));
            }
        }
    }
}