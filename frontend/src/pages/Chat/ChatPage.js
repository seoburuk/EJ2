import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import './ChatPage.css';

const GLOBAL_ROOM_ID = 1;

function ChatPage() {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [nickname, setNickname] = useState('');
  const [connected, setConnected] = useState(false);
  const [connecting, setConnecting] = useState(true);
  const stompClientRef = useRef(null);
  const messagesEndRef = useRef(null);
  const subscriptionRef = useRef(null);
  const nicknameRef = useRef('');

  useEffect(() => {
    initChat();

    const handleBeforeUnload = () => {
      disconnectSync();
    };
    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      disconnectSync();
    };
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const initChat = async () => {
    try {
      // 1. Get nickname (anonymous)
      const nicknameRes = await axios.post(`/api/chat/rooms/${GLOBAL_ROOM_ID}/nickname`, {
        useAnonymous: true
      });
      const assignedNickname = nicknameRes.data.nickname;
      setNickname(assignedNickname);
      nicknameRef.current = assignedNickname;

      // 2. 이전 메시지 로드하지 않음 (새로 들어온 사용자는 이전 대화 볼 수 없음)
      // const messagesRes = await axios.get(`/api/chat/rooms/${GLOBAL_ROOM_ID}/messages`);
      // setMessages(messagesRes.data);
      setMessages([]);

      // 3. Connect WebSocket
      const client = new Client({
        webSocketFactory: () => new SockJS('/ws/chat'),
        debug: () => {},
        onConnect: () => {
          setConnected(true);
          setConnecting(false);
          stompClientRef.current = client;

          subscriptionRef.current = client.subscribe(
            `/topic/chat/${GLOBAL_ROOM_ID}`,
            (messageOutput) => {
              const msg = JSON.parse(messageOutput.body);
              setMessages(prev => [...prev, msg]);
            }
          );

          client.publish({
            destination: `/app/chat/${GLOBAL_ROOM_ID}/join`,
            body: JSON.stringify({
              senderNickname: assignedNickname,
              type: 'JOIN'
            })
          });
        },
        onStompError: (error) => {
          console.error('WebSocket error:', error);
          setConnected(false);
          setConnecting(false);
        }
      });
      client.activate();
    } catch (error) {
      console.error('Failed to init chat:', error);
      setConnecting(false);
    }
  };

  const disconnectSync = () => {
    if (stompClientRef.current) {
      // leave 메시지는 보내지 않음 - WebSocketEventListener가 disconnect 시 자동 처리
      // 중복 호출 방지: userLeave()가 두 번 호출되면 currentUsers가 음수가 되어 리셋 로직이 깨짐
      try {
        if (subscriptionRef.current) {
          subscriptionRef.current.unsubscribe();
        }
        stompClientRef.current.deactivate();
      } catch (e) {}
      stompClientRef.current = null;
      subscriptionRef.current = null;
    }
  };

  const sendMessage = (e) => {
    e.preventDefault();
    if (!inputMessage.trim() || !stompClientRef.current || !connected) return;

    stompClientRef.current.publish({
      destination: `/app/chat/${GLOBAL_ROOM_ID}/send`,
      body: JSON.stringify({
        senderNickname: nickname,
        content: inputMessage.trim(),
        type: 'CHAT'
      })
    });
    setInputMessage('');
  };

  const formatTime = (dateString) => {
    if (!dateString) return '';
    const dateStr = String(dateString);
    const year = dateStr.substring(0, 4);
    const month = dateStr.substring(4, 6);
    const day = dateStr.substring(6, 8);
    const hour = dateStr.substring(8, 10);
    const min = dateStr.substring(10, 12);
    const sec = dateStr.substring(12, 14);
    const date = new Date(year, month, day, hour, min, sec);
    
    return date.toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="chat-popup">
      {/* Header */}
      <div className="chat-header">
        <span className="chat-title">チャット</span>
        <span className="chat-status">
          {connecting ? '接続中...' : connected ? `${nickname}` : '切断'}
        </span>
      </div>

      {/* Messages */}
      <div className="chat-messages">
        {messages.length === 0 ? (
          <div className="chat-empty">
            <div className="chat-empty-icon">💬</div>
            <div className="chat-empty-text">
              チャットを始めましょう！<br />
              メッセージを送信してください
            </div>
          </div>
        ) : (
          messages.map((msg, index) => {
            if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
              return (
                <div key={msg.id || index} className="chat-system">
                  {msg.type === 'JOIN' ? '👋 ' : '👋 '}{msg.content}
                </div>
              );
            }
            const isMe = msg.senderNickname === nickname;
            return (
              <div key={msg.id || index} className={`chat-msg ${isMe ? 'mine' : 'other'}`}>
                {!isMe && <span className="msg-sender">{msg.senderNickname}</span>}
                <div className="msg-bubble">
                  <span className="msg-text">{msg.content}</span>
                  <span className="msg-time">{formatTime(msg.createdAt)}</span>
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form className="chat-input-area" onSubmit={sendMessage}>
        <input
          type="text"
          placeholder="メッセージを入力...!"
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          className="chat-input"
          disabled={!connected}
          maxLength={500}
        />
        <button
          type="submit"
          className="chat-send"
          disabled={!connected || !inputMessage.trim()}
        >
          送信
        </button>
      </form>
    </div>
  );
}

export default ChatPage;
