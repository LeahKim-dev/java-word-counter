# 프로젝트 진행 기록

이 파일을 채워 10월 1일 발표용 PPT를 준비합니다. 구현하지 않은 기능은 '미구현'으로 표시하고, 심화 항목은 진행한 경우에만 작성하세요. 발표는 분석·조회·저장 시연을 포함해 5~10분입니다. 10월 1일 전후로 코드와 이 파일을 본인의 GitHub 저장소에 업로드하고, 저장소 링크를 강사에게 전달해 리뷰를 받습니다. 자세한 안내는 [진행 기록과 발표](docs/project-guide.md)에 있습니다.

## 1. 실행 방법

- JDK: 21
- IntelliJ에서 실행할 클래스: kr.sesac.wordcounter.Main
- 작업 디렉터리(`pom.xml`이 있는 폴더):C:\Users\mjink\Desktop\30_개발\java-word-counter
- 설정 위치와 현재 값:

||설정 위치|현재 값|
|---|---|---|
|CSV 열|FileProcessor.processCsv|text|
|TSV 열|FileProcessor.processTsv|document|
|HTML 본문 선택자|FileProcessor.processHtml|#content|
- CSV열 바꾸는 위치: FileProcessor.processCsv에서 수동으로..

## 2. 구현한 기능

| 기능 | 상태(완료·진행 중·미구현) | 확인한 입력과 결과 |
|---|---|---|
| TXT 카운팅 |  완료|  samples/equivalent/basic.txt → 9개·6종, 정답과 일치
| CSV·TSV·HTML 처리 | 완료 | basic.csv/basic.tsv/basic.html 모두 9개·6종으로 동일, 형식 변환본(news-1000.txt/.tsv/.html)도 CSV와 동일(6,991개·5,052종) |
| 여러 파일 순차 처리 | 완료  | samples/equivalent 폴더 36개·6종, data/klue-ynat/many(16개 파일) 321,084개·78,309종으로 news-full.csv(1개 파일)와 동일 |
| 상위 단어·특정 단어 조회 | 완료 |N=2 → java 3회, 자료구조 2회; N=0/-1/abc → 재입력 안내; JAVA! 조회 → java 3회; java 자바 조회 → 단어 하나 입력 안내  |
| 전체 결과 저장 | 완료 | out/counts.tsv로 저장, Compare Files로 basic-counts.tsv와 identical 확인 |
| 잘못된 입력·실패 파일·빈 파일 처리 | 완료 | samples/invalid 4개 파일 각각 실패 확인, 부분 성공(시도2/성공1/실패1) 확인, memo.bin 건너뜀 확인, 저장 실패(폴더 충돌) 후 조회 계속됨 확인 |


## 3. 정확성 확인과 처리 시간

- 작은 기본 샘플의 전체 결과를 정답과 비교한 방법: out/counts.tsv와 expected/basic-counts.tsv를 IntelliJ Compare Files로 비교, identical 확인
- CSV 따옴표·줄바꿈을 확인한 결과: quoted-lines.csv(5개·3종) 결과가 expected/quoted-lines-counts.tsv와 identical
- 일부 파일이 실패했을 때 확인한 결과: basic.txt + broken-quote.csv를 같은 폴더에 두고 분석 → 시도 2개/성공 1개/실패 1개, 저장 결과가 basic-counts.tsv와 동일 (실패 파일의 앞부분 단어가 집계에 섞이지 않음)
- 결과 저장 파일 위치: out/counts.tsv (고정 파일명, 매 분석마다 덮어씀)

| 입력 | 데이터 건수 / 파일 수 | 전체 단어 수 | 종류 수 | 처리 시간 | 완료·오류 |
|---|---|---|---|---|---|
| `data/klue-ynat/news-1000.csv` | 1,000 / 1 |  6991| 5052 |52.2ms  | 완료 |
| `data/klue-ynat/news-10000.csv` | 10,000 / 1 | 70374| 28871 |101.8ms | 완료 |
| `data/klue-ynat/news-full.csv` | 45,678 / 1 |  321084|78309  |184.3ms  | 완료 |
| `data/klue-ynat/many` | 45,678 / 16, 순차 처리 | 321084 |78309  | 186.8ms | 완료 |

- 전체 파일 하나와 16개 파일의 **모든 단어별 횟수**를 비교한 방법과 결과: news-full.csv(1개 파일)와 many 폴더(16개 파일)를 각각 분석해 out/counts.tsv를 저장한 뒤 Compare Files로 비교, 전체 단어 321,084개·종류 78,309개로 단어별 횟수까지 모두 identical

## 4. 구현 중 해결한 문제
**문제 1. 널 문자가 섞인 경로에서 프로그램이 죽음**
- 원인: Path.of(pathInput)이 경로에 널 문자(\u0000) 등 허용되지 않는 문자가 있으면 InvalidPathException(실행 중 예외)을 던지는데, Files.exists 이전 단계라 아무 것도 못 잡고 있었음
- 해결: Path.of(pathInput)을 try-catch로 감싸서 InvalidPathException을 잡고 안내 메시지 출력 후 다시 입력받도록 함
- 확인: 테스트 코드로 널 문자가 섞인 문자열을 만들어 Path.of()에 넘겼을 때 예외가 잡혀 "경로에 사용할 수 없는 문자가 있습니다: ..." 출력 후 프로그램이 죽지 않고 메뉴로 복귀함

**문제 2. 깨진 CSV에서 프로그램이 죽음**
- 원인: Commons CSV 반복자가 읽는 도중 생긴 오류를 UncheckedIOException으로 던지는데, Main은 IOException만 잡고 있었음
- 해결: processFile에서 UncheckedIOException을 잡아 IOException으로 바꿔 던짐
- 확인: broken-quote.csv 입력 시 "실패: ... (형식이 올바르지 않습니다.)" 출력 후 메뉴로 복귀

**문제 3. 예외를 바꿔 던지면서 원래 원인 정보가 사라짐**
- 원인: UncheckedIOException을 IOException으로 바꿔 던질 때 고정 메시지("형식이 올바르지 않습니다.")만 넣고 원래 예외를 버려서, 원인이 서로 다른 CSV 오류(닫히지 않은 따옴표 등)가 모두 같은 메시지로 출력됨
- 해결: e.getCause()로 원래 오류 메시지를 꺼내 새 메시지에 포함시키고, new IOException(message, e)처럼 기존 예외를 원인(cause)으로 함께 넘기도록 수정
- 확인: broken-quote.csv 분석 시 "실패: ... (형식이 올바르지 않습니다: (startline 3) EOF reached before encapsulated token finished)"처럼 구체적인 원인이 메시지에 나타남을 확인

**문제 4. 열이 없는 CSV에서 프로그램이 죽음**
- 원인: record.get("text")가 열이 없으면 IllegalArgumentException(실행 중 예외)을 던짐
- 해결: 읽기 전에 getHeaderMap()으로 필수 열이 있는지 확인하고 없으면 IOException
- 확인: missing-column.csv가 그 파일만 실패로 처리됨

**문제 5. 잘못된 인코딩의 HTML이 실패로 처리되지 않음**
- 원인: Jsoup.parse(File, "UTF-8")은 잘못된 UTF-8 바이트가 있어도 내부적으로 관대하게 처리해서 예외 없이 그냥 읽어버림 (TXT·CSV·TSV는 Files.newBufferedReader가 디코딩 시점에 예외를 던지는 것과 차이)
- 해결: Files.readString(input, StandardCharsets.UTF_8)으로 먼저 문자열로 읽은 뒤 Jsoup.parse(html)에 넘기도록 변경. 디코딩 실패 시 MalformedInputException(IOException의 하위 클래스)이 발생해 기존 실패 처리로 연결됨
- 확인: 깨진 UTF-8 바이트를 포함한 html 파일을 만들어 분석했을 때, 수정 전에는 "분석 완료"로 성공 처리되던 것이 수정 후 "실패: ..." 로 바뀌고 프로그램이 죽지 않음

**문제 6. 집계된 단어가 없을 때 안내가 없음**
- 원인: 빈 파일을 분석하면 counts가 비어 있지만, showTopWords()에서 별도 처리를 하지 않아 아무 설명 없이 메뉴로 돌아감
- 해결: N 입력 후 counts.isEmpty()를 확인하고, 비어 있으면 "집계된 단어가 없습니다."를 출력한 뒤 return
- 확인: 빈 파일 분석 후 상위 단어 조회 시 집계된 단어가 없습니다.가 출력되고 메뉴로 정상 복귀


## 5. 심화(진행한 경우만)

- 한 파일 처리 개선: 바꾼 부분, 전후 시간, 결과 동일 여부
- 여러 파일 병렬 처리: 입력 폴더, 스레드 수 1·2·4, 전후 시간, 결과 동일 여부
- 중단 후 재개·데이터 수집·기타: 사용법과 확인한 결과

성능을 비교했다면 측정 기기·JDK, 예열·반복 횟수, 전체 작업의 중앙값을 적습니다. 표 양식은 [심화 요구사항](docs/advanced.md)에 있습니다.

## 6. AI 대화 또는 참고 자료

- 웹 대화에서 물어본 개념·힌트·오류 설명:
  -  Commons CSV는 for (CSVRecord record : parser) 반복 중 만난 형식 오류(닫히지
      않은 따옴표 등)를 IOException이 아니라 UncheckedIOException으로 던진다는 점
      — catch (IOException e)만 있으면 못 잡고 프로그램이 멈춘다는 걸 알게 됨
    - record.get("text")처럼 헤더에 없는 열을 요청하면 IllegalArgumentException이
      발생하는데, 이것도 IOException이 아니라서 별도로 잡아야 한다는 점
    - 성공한 파일과 실패한 파일을 구분해서 합산하려면, 파일마다 임시 Map을 따로 만들고
      성공한 것만 전체 결과에 합쳐야 한다는 설계 힌트
    - 코드가 길어져서 파일을 나누는 기준 (Main / FileProcessor / WordCounter / AnalysisSummary)
    - CSV 따옴표 규칙: CSVFormat.RFC4180 -> 따옴표 안의 쉼표, "" 복원, 따옴표 셀 안의 줄바꿈을 파서가 처리

- 도움을 바탕으로 직접 구현한 내용:
    - 파일별 임시 집계 → 성공한 파일만 합산하는 구조 (실패 파일의 앞부분 단어가
      집계에 안 섞이도록)
    - CSV/TSV의 UncheckedIOException, IllegalArgumentException을 잡아 그 파일만
      실패 처리하는 예외 처리
    - 예외 처리: 지원하지 않는 확장자면 확장자를 안내하고, 필수 열이 없거나 셀 수가 맞지 않으면 그 파일만 실패 처리
    - 리팩토링: 4개 파일로 분리했고, 기존 코드 스타일을 유지했으며 파일 수는 최소로 뒀음

- 직접 확인한 입력과 결과:
    - samples/equivalent(txt/csv/tsv/html 4형식 + 폴더 전체) — 모두 9개·6종 또는
      36개·6종으로 정답과 일치
    - samples/edge(빈 파일, 헤더만 있는 CSV, 따옴표·줄바꿈 포함 CSV, 여러 열 CSV,
      특수문자로 시작하는 TXT) — 각각 정답과 identical
    - samples/invalid(깨진 CSV, 열 누락 CSV, 셀 수 불일치 TSV, 본문 요소 0/2개인 HTML)
      — 4개 모두 개별 실패 확인, 폴더 전체 분석 시 시도4/성공0/실패4 확인
    - 오류 상황 실험 4종: 부분 성공(시도2/성공1/실패1, 저장 결과 basic과 identical),
      지원 안 하는 확장자 섞임(건너뜀만 증가), 전체 실패(조회·저장 불가, 요약은 가능),
      저장 실패(out/counts.tsv를 폴더로 만들어 저장 실패 유도, 이후 조회는 정상 동작)
    - 큰 데이터 4종(news-1000/10000/full/many) — 전체 단어 수·종류 수·처리 시간
      모두 정답과 일치, 특히 news-full(1개 파일)과 many(16개 파일 분할)의 단어별
      횟수까지 identical 확인
    - chatbot.csv(Q, A 두 열) — 86,119개·20,675종으로 정답과 일치
    - 형식 변환본(news-1000.txt/.tsv/.html) — 모두 news-1000.csv와 동일한
      6,991개·5,052종

- 참고 링크: 


## 7. 발표할 내용

- 구현한 기능과 전체 처리 흐름: 메뉴 → 새 분석(경로 입력 → 파일별 처리 → 성공만 합산) → 조회/저장/요약
- 시연할 파일·폴더와 정답: basic.txt(9개·6종), samples/invalid(전체 실패), 저장 실패 실험
- 분석 → 조회 → 저장 시연: news-1000.csv 정도로 규모감 있게, out/counts.tsv 저장 확인
- 해결한 문제 또는 성능 실험에서 알게 된 점: UncheckedIOException 처리, news-full과 many 파일 분할이 동일 결과인 것 확인
- 남은 문제와 더 개선하고 싶은 부분: 더 나은 구조, 처리방법, 병렬 처리
