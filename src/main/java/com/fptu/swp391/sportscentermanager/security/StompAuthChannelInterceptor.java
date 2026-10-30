package com.fptu.swp391.sportscentermanager.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null) {
                authHeader = accessor.getFirstNativeHeader("authorization");
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("STOMP connect rejected: Missing or invalid Authorization header");
                throw new BadCredentialsException("Missing or invalid Authorization header");
            }

            String jwt = authHeader.substring(7).trim();

            try {
                String email = jwtUtils.extractEmail(jwt);
                if (email == null || email.isBlank()) {
                    log.warn("STOMP connect rejected: Token contains empty email");
                    throw new BadCredentialsException("Invalid token: email not found");
                }

                CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
                boolean tokenStillValid = jwtUtils.extractTokenVersion(jwt) == userDetails.getUser().currentTokenVersion();
                if (!tokenStillValid) {
                    log.warn("STOMP connect rejected: Token version mismatch for user {}", email);
                    throw new BadCredentialsException("Token has been invalidated");
                }

                if (!userDetails.isAccountNonLocked() || !userDetails.isEnabled()) {
                    log.warn("STOMP connect rejected: Account locked or disabled for user {}", email);
                    throw new BadCredentialsException("Account is inactive or locked");
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                accessor.setUser(authentication);
                log.info("STOMP user connected successfully: {}", email);
            } catch (BadCredentialsException bce) {
                throw bce;
            } catch (Exception e) {
                log.warn("STOMP connection authentication failed: {}", e.getMessage());
                throw new BadCredentialsException("Authentication failed: " + e.getMessage(), e);
            }
        }

        return message;
    }
}
