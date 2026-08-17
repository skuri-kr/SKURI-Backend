package com.skuri.skuri_backend.domain.chat.websocket;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseStompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String APP_CHAT_DESTINATION_PREFIX = "/app/chat/";
    private static final String TOPIC_CHAT_DESTINATION_PREFIX = "/topic/chat/";
    private static final String CHAT_MUTATION_EVENTS_SUFFIX = "/events";

    private final FirebaseTokenVerifier firebaseTokenVerifier;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatWebSocketSessionRegistry sessionRegistry;

    @Value("${security.allowed-email-domain:sungkyul.ac.kr}")
    private String allowedEmailDomain;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            log.info(
                    "Chat STOMP CONNECT frame received: sessionId={}, nativeHeaderNames={}, hasAuthorizationHeader={}",
                    accessor.getSessionId(),
                    accessor.toNativeHeaderMap().keySet(),
                    StringUtils.hasText(resolveIdToken(accessor))
            );
            authenticate(accessor);
            return message;
        }

        if (StompCommand.DISCONNECT.equals(command)) {
            sessionRegistry.unregisterSession(accessor.getSessionId());
            return message;
        }

        if ((StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) && accessor.getUser() == null) {
            throw new MessagingException("인증되지 않은 STOMP 세션입니다.", new BusinessException(ErrorCode.STOMP_AUTH_FAILED));
        }
        if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            authorizeDestination(accessor, command);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String idToken = resolveIdToken(accessor);
        if (!StringUtils.hasText(idToken)) {
            throw new MessagingException("WebSocket 인증 토큰이 없습니다.", new BusinessException(ErrorCode.STOMP_AUTH_FAILED));
        }

        try {
            FirebaseTokenClaims claims = firebaseTokenVerifier.verify(idToken);
            validateEmailDomain(claims.email());
            StompAuthenticatedMember authenticatedMember = new StompAuthenticatedMember(
                    claims.uid(),
                    claims.email(),
                    claims.signInProvider(),
                    claims.providerId(),
                    claims.providerDisplayName(),
                    claims.photoUrl()
            );
            accessor.setUser(authenticatedMember);
            sessionRegistry.registerAuthenticatedSession(authenticatedMember.uid(), accessor.getSessionId());
            log.info(
                    "Chat STOMP CONNECT authenticated: sessionId={}, memberId={}, email={}",
                    accessor.getSessionId(),
                    authenticatedMember.uid(),
                    claims.email()
            );
        } catch (BusinessException e) {
            log.warn(
                    "Chat STOMP CONNECT authentication failed: sessionId={}, message={}",
                    accessor.getSessionId(),
                    e.getMessage()
            );
            throw new MessagingException("WebSocket 인증에 실패했습니다.", new BusinessException(ErrorCode.STOMP_AUTH_FAILED));
        }
    }

    private String resolveIdToken(StompHeaderAccessor accessor) {
        String authorization = getFirstNativeHeader(accessor, AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization)) {
            authorization = getFirstNativeHeader(accessor, AUTHORIZATION_HEADER.toLowerCase(Locale.ROOT));
        }
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private String getFirstNativeHeader(StompHeaderAccessor accessor, String key) {
        List<String> values = accessor.getNativeHeader(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private void validateEmailDomain(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.STOMP_AUTH_FAILED);
        }
        String normalizedAllowedDomain = "@" + allowedEmailDomain.toLowerCase(Locale.ROOT);
        if (!email.toLowerCase(Locale.ROOT).endsWith(normalizedAllowedDomain)) {
            throw new BusinessException(ErrorCode.STOMP_AUTH_FAILED);
        }
    }

    private void authorizeDestination(StompHeaderAccessor accessor, StompCommand command) {
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            return;
        }

        String chatRoomId = extractChatRoomId(destination, command);
        if (!StringUtils.hasText(chatRoomId)) {
            return;
        }

        String memberId = accessor.getUser() != null ? accessor.getUser().getName() : null;
        if (!StringUtils.hasText(memberId)) {
            throw new MessagingException("인증되지 않은 STOMP 세션입니다.", new BusinessException(ErrorCode.STOMP_AUTH_FAILED));
        }

        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new MessagingException("채팅방을 찾을 수 없습니다.", new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        }

        boolean member = chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId(chatRoomId, memberId);
        if (!member) {
            throw new MessagingException("채팅방 멤버가 아닙니다.", new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));
        }
    }

    private String extractChatRoomId(String destination, StompCommand command) {
        if (StompCommand.SEND.equals(command) && destination.startsWith(APP_CHAT_DESTINATION_PREFIX)) {
            return requireChatRoomId(destination.substring(APP_CHAT_DESTINATION_PREFIX.length()));
        }
        if (StompCommand.SUBSCRIBE.equals(command) && destination.startsWith(TOPIC_CHAT_DESTINATION_PREFIX)) {
            String chatRoomId = destination.substring(TOPIC_CHAT_DESTINATION_PREFIX.length());
            if (chatRoomId.endsWith(CHAT_MUTATION_EVENTS_SUFFIX)) {
                chatRoomId = chatRoomId.substring(0, chatRoomId.length() - CHAT_MUTATION_EVENTS_SUFFIX.length());
            }
            return requireChatRoomId(chatRoomId);
        }
        return null;
    }

    private String requireChatRoomId(String chatRoomId) {
        if (!StringUtils.hasText(chatRoomId) || chatRoomId.contains("/")) {
            throw new MessagingException("채팅방 ID가 필요합니다.", new BusinessException(ErrorCode.INVALID_REQUEST));
        }
        return chatRoomId;
    }
}
