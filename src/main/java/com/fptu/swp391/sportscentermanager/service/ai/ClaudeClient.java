package com.fptu.swp391.sportscentermanager.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.*;
import com.fptu.swp391.sportscentermanager.dto.AiChatRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Gọi Claude qua SDK chính thức. Chỉ bật khi có ANTHROPIC_API_KEY (hoặc ai.anthropic.api-key);
 * mọi lỗi / từ chối đều trả Optional.empty() để AiService dùng bộ gợi ý dự phòng, không làm hỏng chức năng.
 */
@Slf4j
@Component
public class ClaudeClient {

    // Bật fallback phía server: khi model từ chối (refusal), API tự chạy lại trên model dự phòng phù hợp
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private final String apiKey;
    private final String model;
    private volatile AnthropicClient client;

    public ClaudeClient(@Value("${ai.anthropic.api-key:${ANTHROPIC_API_KEY:}}") String apiKey,
                        @Value("${ai.anthropic.model:claude-opus-5-5}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    public String model() {
        return model;
    }

    private AnthropicClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .timeout(Duration.ofSeconds(90))
                        .build();
                }
            }
        }
        return client;
    }

    /** Structured output: Claude trả JSON đúng schema của type (record). */
    public <T> Optional<T> structured(String system, String userPrompt, Class<T> type) {
        if (!isEnabled()) return Optional.empty();
        try {
            StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(system)
                .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .outputConfig(type)
                .addUserMessage(userPrompt)
                .build();
            StructuredMessage<T> res = client().messages().create(params);
            if (res.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
                log.warn("Claude từ chối yêu cầu gợi ý bài tập, dùng bộ gợi ý dự phòng");
                return Optional.empty();
            }
            return res.content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst();
        } catch (AnthropicException | IllegalStateException e) {
            log.warn("Gọi Claude (structured) lỗi: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Hội thoại nhiều lượt, trả lời dạng văn bản. */
    public Optional<String> chat(String system, List<AiChatRequestDTO.Turn> history, String message) {
        if (!isEnabled()) return Optional.empty();
        try {
            MessageCreateParams.Builder b = MessageCreateParams.builder()
                .model(model)
                .maxTokens(4000L)
                .system(system)
                // trò chuyện ngắn: effort thấp cho nhanh và rẻ
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
            for (AiChatRequestDTO.Turn t : history) {
                if ("assistant".equals(t.getRole())) b.addAssistantMessage(t.getContent());
                else b.addUserMessage(t.getContent());
            }
            b.addUserMessage(message);
            Message res = client().messages().create(b.build());
            if (res.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
                log.warn("Claude từ chối câu hỏi của hội viên, dùng trả lời dự phòng");
                return Optional.empty();
            }
            String text = res.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .reduce("", (a, c) -> a + c)
                .trim();
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (AnthropicException | IllegalStateException e) {
            log.warn("Gọi Claude (chat) lỗi: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
