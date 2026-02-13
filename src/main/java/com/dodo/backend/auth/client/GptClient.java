package com.dodo.backend.auth.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OpenAI(ChatGPT) API와 통신하여 건강 분석 리포트를 생성하는 클라이언트입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GptClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    /**
     * 지원 가능한 분석 타입인지 문자열 기준으로 확인합니다.
     */
    public boolean isSupportedAnalysisType(String analysisType) {
        return "DAILY".equals(analysisType)
                || "WEEKLY".equals(analysisType)
                || "MONTHLY".equals(analysisType);
    }

    /**
     * GPT에 건강 분석을 요청하고 title/summary/content/fullContent를 반환합니다.
     * 폴백은 사용하지 않으며 실패 시 예외를 발생시킵니다.
     *
     * @param analysisType 분석 타입
     * @param petId        반려동물 ID
     * @param healthData   분석 원본 데이터
     * @return 분석 결과 맵
     */
    public Map<String, Object> generateHealthAnalysis(String analysisType, Long petId, Map<String, Object> healthData) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OPENAI API 키가 설정되지 않았습니다.");
        }

        String normalizedType = analysisType.trim().toUpperCase(Locale.ROOT);
        String prompt = buildPrompt(normalizedType, petId, healthData);
        Map<String, Object> requestBody = Map.of(
                "model", openAiModel,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of(
                                "role",
                                "system",
                                "content",
                                "너는 반려동물 건강 분석가다. 반드시 JSON 객체 하나만 출력한다. 마크다운 코드블록/설명문을 출력하지 않는다."
                        ),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        if (log.isDebugEnabled()) {
            log.debug("GPT 요청 데이터 - petId={}, analysisType={}, model={}, healthData={}",
                    petId, normalizedType, openAiModel, toJson(healthData));
            log.debug("GPT 요청 바디 - {}", toJson(requestBody));
        }

        Map<String, Object> response = webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + openAiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String gptContent = extractMessageContent(response);
        if (gptContent == null || gptContent.isBlank()) {
            throw new IllegalStateException("GPT 응답이 비어 있습니다.");
        }

        Map<String, Object> parsed = parseGptJson(gptContent);
        String title = cleanSingleLine(requiredString(parsed, "title"));
        String summary = cleanSingleLine(requiredString(parsed, "summary"));
        String content = cleanBody(requiredString(parsed, "content"));
        List<String> recommendations = requiredStringList(parsed, "recommendations");

        Object chartData = parsed.get("chartData");
        if (!(chartData instanceof Map<?, ?> chartMap)) {
            throw new IllegalStateException("GPT 응답에 chartData 객체가 없습니다.");
        }

        Map<String, Object> fullContentMap = new LinkedHashMap<>();
        fullContentMap.put("recommendations", recommendations);
        fullContentMap.put("chartData", chartMap);
        fullContentMap.put("analysisType", normalizedType);
        fullContentMap.put("generatedAt", LocalDateTime.now().toString());

        return buildResultMap(title, summary, content, toJson(fullContentMap));
    }

    /**
     * OpenAI 응답에서 첫 번째 choice의 message.content를 추출합니다.
     *
     * @param response OpenAI API 응답 본문
     * @return 추출된 응답 문자열
     */
    private String extractMessageContent(Map<String, Object> response) {
        if (response == null) {
            return null;
        }

        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return null;
        }

        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return null;
        }

        Object contentObj = messageMap.get("content");
        return contentObj instanceof String content ? content : null;
    }

    /**
     * GPT 원문(JSON 문자열)을 Map으로 파싱합니다.
     *
     * @param gptContent GPT 응답 원문
     * @return 파싱된 맵
     */
    private Map<String, Object> parseGptJson(String gptContent) {
        String cleaned = stripCodeFence(gptContent);
        try {
            return objectMapper.readValue(cleaned, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("GPT JSON 파싱에 실패했습니다.", e);
        }
    }

    /**
     * GPT 요청 프롬프트를 생성합니다.
     *
     * @param analysisType 분석 타입
     * @param petId        반려동물 ID
     * @param healthData   수집 데이터
     * @return GPT 요청 프롬프트
     */
    private String buildPrompt(String analysisType, Long petId, Map<String, Object> healthData) {
        return """
                petId=%d
                analysisType=%s
                healthData=%s

                다음 JSON 스키마 그대로 응답해.
                {
                  "title": "반드시 '{petName}의 yyyy년 M월 d일 건강 분석 리포트' 형식",
                  "summary": "핵심 요약 1~2문장(템플릿 문구 금지)",
                  "content": "분석 본문만 4~6문장. '제목:', '요약:', '기본 본문:' 접두어 금지",
                  "recommendations": ["실행 가능한 권장 행동 2~4개"],
                  "chartData": {
                    "weightSeries": {"labels": [], "data": []},
                    "activitySeries": {"labels": [], "distance": []},
                    "heartRateSeries": {"labels": [], "data": []},
                    "stats": {"weightCount": 0, "activityCount": 0, "heartRateCount": 0, "heartRateAvg": 0}
                  }
                }

                중요:
                - JSON 객체 하나만 출력
                - 마크다운 코드블록 금지
                - title은 healthData.petName을 반드시 포함
                - title 날짜는 healthData.reportDate를 기준으로 작성
                - title/summary/content에 템플릿 문구('일별 건강 분석 리포트', '일별 데이터 기반 건강 분석 요약입니다.') 사용 금지
                - recommendations는 반드시 2~4개 문자열 배열로 작성 (예: "간식 10%% 줄이기", "산책 시간 15분 늘리기")
                - chartData.weightSeries.labels / activitySeries.labels / heartRateSeries.labels 는 모두 필수
                - 각 시리즈에서 데이터가 1개 이상이면 labels도 반드시 1개 이상이어야 함
                - 각 시리즈의 labels 길이는 해당 data(또는 distance) 길이와 반드시 동일해야 함
                - 데이터가 1개여도 labels에도 반드시 1개 값을 넣을 것
                """.formatted(petId, analysisType, toJson(healthData));
    }

    /**
     * 마크다운 코드블록으로 감싸진 응답을 정리합니다.
     *
     * @param value 원본 문자열
     * @return 정리된 문자열
     */
    private String stripCodeFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    /**
     * 필수 문자열 필드를 추출합니다.
     *
     * @param map 맵 데이터
     * @param key 필드 키
     * @return 문자열 값
     */
    private String requiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String str) || str.isBlank()) {
            throw new IllegalStateException("GPT 응답 필수 필드 누락: " + key);
        }
        return str;
    }

    /**
     * 필수 문자열 배열 필드를 추출합니다.
     *
     * @param map 맵 데이터
     * @param key 필드 키
     * @return 문자열 리스트 값
     */
    private List<String> requiredStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalStateException("GPT 응답 필수 배열 필드 누락: " + key);
        }

        for (Object element : list) {
            if (!(element instanceof String str) || str.isBlank()) {
                throw new IllegalStateException("GPT 응답 배열 필드 값이 올바르지 않습니다: " + key);
            }
        }

        List<String> casted = (List<String>) list;
        return casted;
    }

    /**
     * 제목/요약 문자열을 한 줄로 정리합니다.
     *
     * @param value 원본 문자열
     * @return 정리된 문자열
     */
    private String cleanSingleLine(String value) {
        return value
                .replaceAll("^(제목|요약|기본 본문)\\s*:\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 본문 문자열에서 불필요한 접두어를 제거합니다.
     *
     * @param value 원본 문자열
     * @return 정리된 본문 문자열
     */
    private String cleanBody(String value) {
        return value
                .replaceAll("(?m)^제목\\s*:\\s*.*$", "")
                .replaceAll("(?m)^요약\\s*:\\s*.*$", "")
                .replaceAll("(?m)^기본 본문\\s*:\\s*", "")
                .trim();
    }

    /**
     * 객체를 JSON 문자열로 직렬화합니다.
     *
     * @param value 직렬화 대상
     * @return JSON 문자열
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 직렬화에 실패했습니다.", e);
        }
    }

    /**
     * 분석 결과를 저장용 맵으로 생성합니다.
     *
     * @param title       제목
     * @param summary     요약
     * @param content     본문
     * @param fullContent 상세 JSON 문자열
     * @return 결과 맵
     */
    private Map<String, Object> buildResultMap(String title, String summary, String content, String fullContent) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", title);
        result.put("summary", summary);
        result.put("content", content);
        result.put("fullContent", fullContent);
        return result;
    }
}
