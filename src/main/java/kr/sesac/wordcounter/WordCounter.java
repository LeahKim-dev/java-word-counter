package kr.sesac.wordcounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

            /*
             * 문제점 : 소문자 변환 결과가 실행 환경의 Locale에 영향을 받을 수 있음.
             * 원인 : toLowerCase() 호출 시 Locale을 명시하지 않음.
             * 수정자 : 원대호
             */
            String token = matcher.group().toLowerCase(Locale.ROOT);

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

            /*
             * 문제점 : 단어 횟수 증가를 위해 기존 값을 조회하고 다시 저장함.
             * 원인 : getOrDefault()와 put()을 사용해 직접 값을 갱신함.
             * 수정자 : 원대호
             */
            counts.merge(token, 1L, Long::sum);
        }
    }

    // 파일별 결과를 전체 결과에 더하는 헬퍼
    static void mergeCounts(Map<String, Long> total, Map<String, Long> part) {
        for (Map.Entry<String, Long> entry : part.entrySet()) {

            /*
             * 문제점 : 파일별 단어 수를 합칠 때 기존 값을 직접 조회하고 다시 저장함.
             * 원인 : Map에서 제공하는 값 병합 기능을 사용하지 않고 직접 합산함.
             * 수정자 : 원대호
             */
            total.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
    }

    // 정렬 (상위 N개 조회, 전체 저장 모두 같은 순서를 써야 하므로)
    static List<Map.Entry<String, Long>> getSortedEntries(
            Map<String, Long> counts) {

        // counts라는 Map에서 Entry들을 꺼내서,
        // 그것들로 ArrayList를 하나 만들고 entries라는 변수에 저장
        List<Map.Entry<String, Long>> entries =
                new ArrayList<>(counts.entrySet());

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