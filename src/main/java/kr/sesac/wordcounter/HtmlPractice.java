package kr.sesac.wordcounter;

import java.io.IOException;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlPractice {
    public static void main(String[] args) throws IOException {
        Document document = Jsoup.parse(
                Path.of("samples/equivalent/basic.html").toFile(), "UTF-8");
        Elements matches = document.select("#content");
        if (matches.size() != 1) {
            throw new IOException("본문 요소는 정확히 하나여야 합니다.");
        }
        Element content = matches.first();
        content.select("script, style, nav, header, footer").remove();
        System.out.println(content.text());
    }
}