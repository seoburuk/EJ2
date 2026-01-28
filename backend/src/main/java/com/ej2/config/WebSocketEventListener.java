package com.ej2.config;

import com.ej2.model.ChatMessage;
import com.ej2.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private ChatService chatService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            String nickname = (String) sessionAttributes.get("nickname");
            Long roomId = (Long) sessionAttributes.get("roomId");

            if (nickname != null && roomId != null) {
                logger.info("User disconnected: {} from room {}", nickname, roomId);

                // Build leave message
                ChatMessage leaveMessage = new ChatMessage();
                leaveMessage.setType(ChatMessage.MessageType.LEAVE);
                leaveMessage.setRoomId(roomId);
                leaveMessage.setSenderNickname(nickname);
                leaveMessage.setContent(nickname + "さんが退室しました");

                chatService.userLeave(roomId);
                chatService.saveMessage(leaveMessage);

                // Broadcast to room
                messagingTemplate.convertAndSend("/topic/chat/" + roomId, leaveMessage);
            }
        }
    }
}
