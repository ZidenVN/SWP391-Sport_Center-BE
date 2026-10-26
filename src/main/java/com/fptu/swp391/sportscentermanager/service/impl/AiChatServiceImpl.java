package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.AiChatResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.AiConversationResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.AiMessageResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.AiConversation;
import com.fptu.swp391.sportscentermanager.entity.AiMessage;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.AiConversationRepository;
import com.fptu.swp391.sportscentermanager.repository.AiMessageRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.AiChatService;
import com.fptu.swp391.sportscentermanager.service.ai.AiTool;
import com.fptu.swp391.sportscentermanager.service.ai.GeminiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {
    private static final int MAX_TOOL_ROUNDS = 5;
    private static final String FALLBACK_REPLY = "Xin lỗi, mình chưa trả lời được câu hỏi này. Bạn thử hỏi lại theo cách khác nhé!";

    private final GeminiClient geminiClient;
    private final List<AiTool> allTools;
    private final AiConversationRepository aiConversationRepository;
    private final AiMessageRepository aiMessageRepository;
    private final UserRepository userRepository;
    private final int memberDailyLimit;
    private final int historySize;

    public AiChatServiceImpl(GeminiClient geminiClient, List<AiTool> allTools,
                             AiConversationRepository aiConversationRepository,
                             AiMessageRepository aiMessageRepository, UserRepository userRepository,
                             @Value("${ai.daily-limit.member}") int memberDailyLimit,
                             @Value("${ai.history-size}") int historySize) {
        this.geminiClient = geminiClient;
        this.allTools = allTools;
        this.aiConversationRepository = aiConversationRepository;
        this.aiMessageRepository = aiMessageRepository;
        this.userRepository = userRepository;
        this.memberDailyLimit = memberDailyLimit;
        this.historySize = historySize;
    }

    @Override
    @Transactional
    public AiChatResponseDTO chat(CustomUserDetails principal, Long conversationId, String message) {
        Long userId = principal.getUser().getUserId();
        String role = principal.getUser().getRole().getRoleName();

        // 1. Gioi han luot hoi trong ngay
        long usedToday = aiMessageRepository.countByConversation_User_UserIdAndRoleAndCreatedAtAfter(
            userId, "user", LocalDate.now().atStartOfDay());
        if (usedToday >= memberDailyLimit) {
            throw new AppException(ErrorCode.AI_DAILY_LIMIT);
        }

        // 2. Lay cuoc tro chuyen cu (phai la cua chinh minh) hoac tao moi
        AiConversation conv = (conversationId == null)
            ? aiConversationRepository.save(AiConversation.builder()
                .user(userRepository.getReferenceById(userId))
                .title(message.length() > 50 ? message.substring(0, 50) + "..." : message)
                .build())
            : aiConversationRepository.findByConversationIdAndUser_UserId(conversationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_CONVERSATION_NOT_FOUND));

        // 3. Lich su: chi gui historySize tin gan nhat de tiet kiem token
        List<AiMessage> history = aiMessageRepository.findByConversation_ConversationIdOrderByCreatedAtAsc(conv.getConversationId());
        List<Map<String, Object>> contents = new ArrayList<>();
        history.stream().skip(Math.max(0, history.size() - historySize))
            .forEach(m -> contents.add(textContent(m.getRole(), m.getContent())));
        contents.add(textContent("user", message));

        // 4. Chi dua cho AI cac cong cu thuoc role hien tai
        Map<String, AiTool> tools = allTools.stream()
            .filter(t -> t.roles().contains(role))
            .collect(Collectors.toMap(AiTool::name, t -> t));
        List<Map<String, Object>> declarations = tools.values().stream().map(this::toDeclaration).toList();

        // 5. Vong lap tool calling
        String reply = runToolLoop(systemPromptFor(), contents, declarations, tools, principal);

        // 6. Chi luu khi Gemini tra loi thanh cong, loi thi khong mat luot
        aiMessageRepository.save(AiMessage.builder().conversation(conv).role("user").content(message).build());
        aiMessageRepository.save(AiMessage.builder().conversation(conv).role("model").content(reply).build());
        conv.setUpdatedAt(LocalDateTime.now());

        return AiChatResponseDTO.builder().conversationId(conv.getConversationId()).reply(reply).build();
    }

    @SuppressWarnings("unchecked")
    private String runToolLoop(String systemPrompt, List<Map<String, Object>> contents,
                               List<Map<String, Object>> declarations, Map<String, AiTool> tools,
                               CustomUserDetails principal) {
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            Map<String, Object> modelContent = firstCandidateContent(geminiClient.generate(systemPrompt, contents, declarations));
            List<Map<String, Object>> parts = (List<Map<String, Object>>) modelContent.getOrDefault("parts", List.of());

            List<Map<String, Object>> calls = parts.stream()
                .filter(p -> p.containsKey("functionCall"))
                .map(p -> (Map<String, Object>) p.get("functionCall"))
                .toList();

            if (calls.isEmpty()) {
                String text = parts.stream().map(p -> (String) p.get("text")).filter(Objects::nonNull)
                    .collect(Collectors.joining()).trim();
                return text.isEmpty() ? FALLBACK_REPLY : text;
            }

            // Giu nguyen luot AI yeu cau goi cong cu, roi gui ket qua tung cong cu
            contents.add(modelContent);
            List<Map<String, Object>> responses = new ArrayList<>();
            for (Map<String, Object> call : calls) {
                String name = (String) call.get("name");
                Map<String, Object> args = (Map<String, Object>) call.getOrDefault("args", Map.of());
                AiTool tool = tools.get(name);
                Object result;
                if (tool == null) {
                    // AI goi cong cu khong thuoc role nay: khong chay
                    result = Map.of("error", "Không có quyền dùng công cụ này");
                } else {
                    log.info("AI gọi công cụ {} cho user {}", name, principal.getUser().getUserId());
                    result = tool.execute(args, principal);
                }
                // Model moi tra kem id cho moi functionCall, gui lai dung id de Gemini ghep ket qua
                Map<String, Object> functionResponse = new HashMap<>();
                functionResponse.put("name", name);
                functionResponse.put("response", Map.of("result", result));
                if (call.get("id") != null) {
                    functionResponse.put("id", call.get("id"));
                }
                responses.add(Map.of("functionResponse", functionResponse));
            }
            contents.add(Map.of("role", "user", "parts", responses));
        }
        return FALLBACK_REPLY;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiConversationResponseDTO> getMyConversations(Long userId) {
        return aiConversationRepository.findByUser_UserIdOrderByUpdatedAtDesc(userId).stream()
            .map(c -> AiConversationResponseDTO.builder()
                .conversationId(c.getConversationId()).title(c.getTitle()).updatedAt(c.getUpdatedAt()).build())
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiMessageResponseDTO> getMessages(Long userId, Long conversationId) {
        aiConversationRepository.findByConversationIdAndUser_UserId(conversationId, userId)
            .orElseThrow(() -> new AppException(ErrorCode.AI_CONVERSATION_NOT_FOUND));
        return aiMessageRepository.findByConversation_ConversationIdOrderByCreatedAtAsc(conversationId).stream()
            .map(m -> AiMessageResponseDTO.builder()
                .role(m.getRole()).content(m.getContent()).createdAt(m.getCreatedAt()).build())
            .toList();
    }

    private Map<String, Object> textContent(String role, String text) {
        return Map.of("role", role, "parts", List.of(Map.of("text", text)));
    }

    private Map<String, Object> toDeclaration(AiTool tool) {
        Map<String, Object> decl = new HashMap<>();
        decl.put("name", tool.name());
        decl.put("description", tool.description());
        if (!tool.parameters().isEmpty()) {
            decl.put("parameters", tool.parameters());
        }
        return decl;
    }

    // Lay candidates[0].content; bi chan an toan hoac rong thi tra Map rong
    @SuppressWarnings("unchecked")
    private Map<String, Object> firstCandidateContent(Map<String, Object> response) {
        if (response == null) return Map.of();
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return Map.of();
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        return content == null ? Map.of() : content;
    }

    // Giai doan 1 chi co member; giai doan sau them prompt cho COACH, RECEPTIONIST, CENTER_MANAGER
    private String systemPromptFor() {
        return """
            Bạn là trợ lý AI của trung tâm thể thao Sport Center, trả lời bằng tiếng Việt, ngắn gọn, thân thiện.
            Hôm nay là %s.
            Phạm vi: (1) thông tin trung tâm: lớp học, gói tập, giá, lịch và gói của chính hội viên;
            (2) tập luyện, dinh dưỡng, sức khoẻ, lối sống.
            Với câu hỏi ngoài phạm vi, lịch sự từ chối và gợi ý hỏi về các chủ đề trên.
            Luôn dùng công cụ để lấy dữ liệu trung tâm; không tự bịa giá, lịch hay số chỗ.
            Khi gợi ý lớp, nêu rõ môn, ngày giờ và số chỗ còn.
            Khi tư vấn sức khoẻ hoặc chấn thương, nhắc rằng đây là gợi ý chung, nên hỏi HLV hoặc bác sĩ.
            """.formatted(LocalDate.now());
    }
}
