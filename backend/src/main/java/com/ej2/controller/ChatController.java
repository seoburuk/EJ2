package com.ej2.controller;

import com.ej2.model.ChatMessage;
import com.ej2.model.ChatRoom;
import com.ej2.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // ======== REST API ========

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getAllRooms() {
        return ResponseEntity.ok(chatService.getAllRooms());
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<ChatRoom> getRoomById(@PathVariable Long id) {
        return chatService.getRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody ChatRoom room) {
        ChatRoom created = chatService.createRoom(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        chatService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getRecentMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatService.getRecentMessages(roomId));
    }

    // REST endpoint: assign nickname before WebSocket connect
    // useAnonymous=true or not logged in → 匿名N
    // useAnonymous=false and logged in → use provided username
    @PostMapping("/rooms/{roomId}/nickname")
    public ResponseEntity<Map<String, String>> assignNickname(
            @PathVariable Long roomId,
            @RequestBody(required = false) Map<String, Object> body) {

        boolean useAnonymous = true;
        String userName = null;

        if (body != null) {
            Object anon = body.get("useAnonymous");
            if (anon instanceof Boolean) {
                useAnonymous = (Boolean) anon;
            }
            Object name = body.get("userName");
            if (name instanceof String) {
                userName = (String) name;
            }
        }

        String nickname;
        if (!useAnonymous && userName != null && !userName.trim().isEmpty()) {
            // Logged in user choosing to use their name
            nickname = userName.trim();
            chatService.incrementCurrentUsers(roomId);
        } else {
            // Anonymous
            nickname = chatService.assignNickname(roomId);
        }

        Map<String, String> result = new HashMap<String, String>();
        result.put("nickname", nickname);
        return ResponseEntity.ok(result);
    }

    // ======== WebSocket Message Handlers ========

    @MessageMapping("/chat/{roomId}/send")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessage sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessage message) {
        message.setRoomId(roomId);
        message.setType(ChatMessage.MessageType.CHAT);
        return chatService.saveMessage(message);
    }

    @MessageMapping("/chat/{roomId}/join")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessage joinRoom(
            @DestinationVariable Long roomId,
            @Payload ChatMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setRoomId(roomId);
        message.setType(ChatMessage.MessageType.JOIN);
        message.setContent(message.getSenderNickname() + "さんが入室しました");

        // Store in WebSocket session for disconnect handling
        headerAccessor.getSessionAttributes().put("nickname", message.getSenderNickname());
        headerAccessor.getSessionAttributes().put("roomId", roomId);

        // WebSocket接続成功時にcurrentUsersを増加（REST/WebSocketライフサイクル一致）
        chatService.incrementCurrentUsers(roomId);

        return chatService.saveMessage(message);
    }

    @MessageMapping("/chat/{roomId}/leave")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessage leaveRoom(
            @DestinationVariable Long roomId,
            @Payload ChatMessage message) {
        message.setRoomId(roomId);
        message.setType(ChatMessage.MessageType.LEAVE);
        message.setContent(message.getSenderNickname() + "さんが退室しました");

        chatService.userLeave(roomId);
        return chatService.saveMessage(message);
    }
}
