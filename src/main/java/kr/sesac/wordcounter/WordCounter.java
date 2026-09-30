package kr.sesac.wordcounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordCounter {
    private static final Pattern WORD_PATTERN =
            Pattern.compile("[A-Za-z0-9가-힣ㄱ-ㅎㅏ-ㅣ]+");

    // 문자열에서 단어 목록만 뽑기 findWord용
    public static List<String> extractTokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String token = matcher.group().toLowerCase();
            // 숫자만 있는 단어 제외
            if (!token.matches("[0-9]+")) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    // 한 줄(또는 한 문단)에서 단어를 뽑아 counts에 누적
    static void countWords(String text, Map<String, Long> counts) {
        for (String token : extractTokens(text)) {
                long current = counts.getOrDefault(token, 0L);
                counts.put(token, current + 1);
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
        // Map -> entry들의 List

        // counts라는 Map에서 Entry들을 꺼내서, 그것들로 ArrayList를 하나 만들고, 그 List를 entries라는 변수에 저장
        List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());

        entries.sort((a, b) -> {
            // 횟수 내림차순
            int cmp = Long.compare(b.getValue(), a.getValue());

            // 횟수가 다르면 -> 횟수 기준으로 결정
            if (cmp != 0) {
                return cmp;
            }

            // 횟수가 같으면 -> 이름 사전순
            return a.getKey().compareTo(b.getKey());
        });
        return entries;
    }
}
