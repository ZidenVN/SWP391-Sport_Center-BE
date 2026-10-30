package com.fptu.swp391.sportscentermanager.config;

import com.fptu.swp391.sportscentermanager.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple broker xử lý tin nhắn cá nhân qua /queue
        config.enableSimpleBroker("/queue");
        // Prefix dành cho tin nhắn gửi riêng cho từng user: /user/{email}/queue/...
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP qua WebSocket thuần theo hợp đồng (ws:// / wss://)
        String[] origins = allowedOrigins != null && !allowedOrigins.isEmpty()
                ? allowedOrigins.toArray(new String[0])
                : new String[]{"*"};

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);

        // Hỗ trợ thêm fallback SockJS trên endpoint /ws-sockjs nếu client yêu cầu
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Gắn interceptor để chặn và xác thực JWT token ở frame CONNECT
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
