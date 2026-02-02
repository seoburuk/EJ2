package com.ej2.service;

import com.ej2.model.ChatMessage;
import com.ej2.model.ChatRoom;
import com.ej2.repository.ChatMessageRepository;
import com.ej2.repository.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // アプリ起動時にグローバルルームを自動作成
    @PostConstruct
    public void initGlobalRoom() {
        if (!chatRoomRepository.findById(1L).isPresent()) {
            ChatRoom globalRoom = new ChatRoom();
            globalRoom.setName("グローバルチャット");
            globalRoom.setDescription("全員匿名のチャットルーム");
            globalRoom.setNicknameCounter(0);
            globalRoom.setCurrentUsers(0);
            chatRoomRepository.save(globalRoom);
        }
    }

    // Room operations
    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }

    public Optional<ChatRoom> getRoomById(Long id) {
        return chatRoomRepository.findById(id);
    }

    public ChatRoom createRoom(ChatRoom room) {
        return chatRoomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        chatRoomRepository.deleteById(id);
    }

    // Message operations
    public ChatMessage saveMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getRecentMessages(Long roomId) {
        List<ChatMessage> messages = chatMessageRepository.findTop50ByRoomIdOrderByCreatedAtDesc(roomId);
        List<ChatMessage> reversed = new ArrayList<ChatMessage>(messages);
        Collections.reverse(reversed);
        return reversed;
    }

    // Assign sequential anonymous nickname: 匿名1, 匿名2, 匿名3...
    public String assignNickname(Long roomId) {
        Optional<ChatRoom> optRoom = chatRoomRepository.findById(roomId);
        if (optRoom.isPresent()) {
            ChatRoom room = optRoom.get();
            int nextNumber = room.getNicknameCounter() + 1;
            room.setNicknameCounter(nextNumber);
            room.setCurrentUsers(room.getCurrentUsers() + 1);
            chatRoomRepository.save(room);
            return "匿名" + nextNumber;
        }
        return "匿名0";
    }

    // Increment user count only (for non-anonymous users)
    public void incrementCurrentUsers(Long roomId) {
        Optional<ChatRoom> optRoom = chatRoomRepository.findById(roomId);
        if (optRoom.isPresent()) {
            ChatRoom room = optRoom.get();
            room.setCurrentUsers(room.getCurrentUsers() + 1);
            chatRoomRepository.save(room);
        }
    }

    // User leaves room - reset nickname counter when room becomes empty
    public void userLeave(Long roomId) {
        Optional<ChatRoom> optRoom = chatRoomRepository.findById(roomId);
        if (optRoom.isPresent()) {
            ChatRoom room = optRoom.get();
            int count = room.getCurrentUsers() - 1;
            room.setCurrentUsers(Math.max(0, count));
            int nickCnt = room.getNicknameCounter() - 1;
            room.setNicknameCounter(nickCnt);

            // 채팅방에 아무도 없으면 익명 번호 리셋
            if (count <= 0) {
                room.setNicknameCounter(0);
            }

            chatRoomRepository.save(room);
        }
    }
}
