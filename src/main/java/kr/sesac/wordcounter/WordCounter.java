package kr.sesac.wordcounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordCounter {
    private static final Pattern WORD_PATTERN =
            Pattern.compile("[A-Za-z0-9가-힣ㄱ-ㅎㅏ-ㅣ]+");

    // 한 줄(또는 한 문단)에서 단어를 뽑아 counts에 누적
    static void countWords(String text, Map<String, Long> counts) {
        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String token = matcher.group();
            token = token.toLowerCase();

            if (!token.matches("[0-9]+")) {
                long current = counts.getOrDefault(token, 0L);
                counts.put(token, current + 1);
            }
        }
    }

    // 파일별 결과를 전체 결과에 더하는 헬퍼
    static void mergeCounts(Map<String, Long> total, Map<String, Long> part) {
        for (Map.Entry<String, Long> entry : part.entrySet()) {
            String word = entry.getKey();
            long count = entry.getValue();
            long current = total.getOrDefault(word, 0L);
            total.put(word, current + count);
        }
    }

    // 정렬 (상위 N개 조회, 전체 저장 모두 같은 순서를 써야 하므로)
    static List<Map.Entry<String, Long>> getSortedEntries(Map<String, Long> counts) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> {
            int cmp = Long.compare(b.getValue(), a.getValue()); // 횟수 내림차순
            if (cmp != 0) return cmp;
            return a.getKey().compareTo(b.getKey()); // 같으면 사전순
        });
        return entries;
    }

    // 문자열에서 단어 목록만 뽑기 findWord용
    public static List<String> extractTokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String token = matcher.group().toLowerCase();
            if (!token.matches("[0-9]+")) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
