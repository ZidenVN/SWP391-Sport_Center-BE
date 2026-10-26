package com.fptu.swp391.sportscentermanager.service.ai;

import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {
    // Goi Gemini toi da 2 lan cho moi model truoc khi chuyen sang model du phong
    private static final int ATTEMPTS_PER_MODEL = 2;
    private static final long RETRY_DELAY_MS = 2000;

    private final RestClient restClient;
    private final List<String> models;

    // Spring Boot 4 khong tu tao bean RestClient.Builder neu thieu module restclient, nen tu build
    public GeminiClient(@Value("${gemini.base-url}") String baseUrl,
                        @Value("${gemini.api-key}") String apiKey,
                        @Value("${gemini.model}") String model,
                        @Value("${gemini.fallback-model:}") String fallbackModel
    ) {
        // gemini co the mat vai giay moi tra loi, dat timeout ro rang
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        // Model qua tai co the treo lau, cat som de con thoi gian chuyen sang model du phong
        factory.setReadTimeout(Duration.ofSeconds(45));
        this.restClient = RestClient.builder().baseUrl(baseUrl).defaultHeader("x-goog-api-key", apiKey).requestFactory(factory).build();
        this.models = fallbackModel.isBlank() ? List.of(model) : List.of(model, fallbackModel);
    }

    public Map<String, Object> generate(String systemPrompt, List<Map<String, Object>> contents,
                                        List<Map<String, Object>> functionDeclarations) {
        Map<String, Object> body = new HashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        body.put("contents", contents);
        if (!functionDeclarations.isEmpty()) {
            body.put("tools", List.of(Map.of("functionDeclarations", functionDeclarations)));
        }

        nextModel:
        for (String model : models) {
            for (int attempt = 1; attempt <= ATTEMPTS_PER_MODEL; attempt++) {
                try {
                    return restClient.post().uri("/models/{model}:generateContent", model)
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {
                        });
                } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException.ServiceUnavailable e) {
                    // 429 het han muc / 503 model qua tai: thu lai, roi chuyen model du phong
                    log.warn("Gemini {} ban (HTTP {}, lan {}): {}", model, e.getStatusCode().value(), attempt, e.getResponseBodyAsString());
                    sleepBeforeRetry();
                } catch (ResourceAccessException e) {
                    // Het thoi gian cho / loi mang: khong thu lai model nay, chuyen sang model du phong
                    log.warn("Gemini {} khong phan hoi kip (lan {}): {}", model, attempt, e.getMessage());
                    continue nextModel;
                } catch (RestClientResponseException e) {
                    log.error("Gọi Gemini {} thất bại (HTTP {}): {}", model, e.getStatusCode().value(), e.getResponseBodyAsString());
                    throw new AppException(ErrorCode.AI_UNAVAILABLE);
                } catch (RestClientException e) {
                    log.error("Gọi Gemini {} thất bại", model, e);
                    throw new AppException(ErrorCode.AI_UNAVAILABLE);
                }
            }
        }
        throw new AppException(ErrorCode.AI_PROVIDER_BUSY);
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
    }

}
