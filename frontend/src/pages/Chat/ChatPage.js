import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import './ChatPage.css';

function ChatPage() {
  const [rooms, setRooms] = useState([]);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [nickname, setNickname] = useState('');
  const [connected, setConnected] = useState(false);
  const [showCreateRoom, setShowCreateRoom] = useState(false);
  const [newRoomName, setNewRoomName] = useState('');
  const [newRoomDesc, setNewRoomDesc] = useState('');
  const [loggedInUser, setLoggedInUser] = useState(null);
  const [showNicknameModal, setShowNicknameModal] = useState(false);
  const [pendingRoom, setPendingRoom] = useState(null);
  const stompClientRef = useRef(null);
  const messagesEndRef = useRef(null);
  const subscriptionRef = useRef(null);
  const nicknameRef = useRef('');
  const selectedRoomRef = useRef(null);

  useEffect(() => {
    fetchRooms();

    // Check login status from localStorage (shared with main window)
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setLoggedInUser(JSON.parse(storedUser));
    }

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

  const fetchRooms = async () => {
    try {
      const response = await axios.get('/api/chat/rooms');
      setRooms(response.data);
    } catch (error) {
      console.error('Failed to fetch chat rooms:', error);
    }
  };

  // Room click handler: if logged in → show modal, else → join as anonymous
  const handleRoomClick = (room) => {
    if (loggedInUser) {
      setPendingRoom(room);
      setShowNicknameModal(true);
    } else {
      joinRoom(room, true); // anonymous
    }
  };

  // Join room with nickname choice
  const joinRoom = async (room, useAnonymous) => {
    setShowNicknameModal(false);
    setPendingRoom(null);
    setSelectedRoom(room);
    selectedRoomRef.current = room;
    setMessages([]);

    try {
      // 1. Request nickname from server
      const nicknameRes = await axios.post(`/api/chat/rooms/${room.id}/nickname`, {
        useAnonymous: useAnonymous,
        userName: useAnonymous ? null : loggedInUser.name
      });
      const assignedNickname = nicknameRes.data.nickname;
      setNickname(assignedNickname);
      nicknameRef.current = assignedNickname;

      // 2. Load recent messages
      const messagesRes = await axios.get(`/api/chat/rooms/${room.id}/messages`);
      setMessages(messagesRes.data);

      // 3. Connect via WebSocket
      const client = new Client({
        webSocketFactory: () => new SockJS('/api/ws/chat'),
        debug: () => {},
        onConnect: () => {
          setConnected(true);
          stompClientRef.current = client;

          subscriptionRef.current = client.subscribe(
            `/topic/chat/${room.id}`,
            (messageOutput) => {
              const msg = JSON.parse(messageOutput.body);
              setMessages(prev => [...prev, msg]);
            }
          );

          client.publish({
            destination: `/app/chat/${room.id}/join`,
            body: JSON.stringify({
              senderNickname: assignedNickname,
              type: 'JOIN'
            })
          });
        },
        onStompError: (error) => {
          console.error('WebSocket connection error:', error);
          setConnected(false);
        }
      });
      client.activate();
    } catch (error) {
      console.error('Failed to join room:', error);
    }
  };

  const disconnectSync = () => {
    if (stompClientRef.current) {
      const currentNickname = nicknameRef.current;
      const currentRoom = selectedRoomRef.current;
      if (currentNickname && currentRoom) {
        try {
          stompClientRef.current.publish({
            destination: `/app/chat/${currentRoom.id}/leave`,
            body: JSON.stringify({ senderNickname: currentNickname, type: 'LEAVE' })
          });
        } catch (e) {
          // Ignore errors during cleanup
        }
      }
      try {
        if (subscriptionRef.current) {
          subscriptionRef.current.unsubscribe();
        }
        stompClientRef.current.deactivate();
      } catch (e) {
        // Ignore errors during cleanup
      }
      stompClientRef.current = null;
      subscriptionRef.current = null;
    }
  };

  const leaveRoom = () => {
    disconnectSync();
    setConnected(false);
    setSelectedRoom(null);
    selectedRoomRef.current = null;
    setNickname('');
    nicknameRef.current = '';
    setMessages([]);
    fetchRooms();
  };

  const sendMessage = (e) => {
    e.preventDefault();
    if (!inputMessage.trim() || !stompClientRef.current || !connected) return;

    stompClientRef.current.publish({
      destination: `/app/chat/${selectedRoom.id}/send`,
      body: JSON.stringify({
        senderNickname: nickname,
        content: inputMessage.trim(),
        type: 'CHAT'
      })
    });
    setInputMessage('');
  };

  const handleCreateRoom = async (e) => {
    e.preventDefault();
    if (!newRoomName.trim()) return;

    try {
      await axios.post('/api/chat/rooms', {
        name: newRoomName.trim(),
        description: newRoomDesc.trim()
      });
      setNewRoomName('');
      setNewRoomDesc('');
      setShowCreateRoom(false);
      fetchRooms();
    } catch (error) {
      console.error('Failed to create room:', error);
    }
  };

  const formatTime = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit' });
  };

  // ======== Nickname Selection Modal ========
  const renderNicknameModal = () => {
    if (!showNicknameModal || !pendingRoom) return null;

    return (
      <div className="nickname-modal-overlay" onClick={() => {
        setShowNicknameModal(false);
        setPendingRoom(null);
      }}>
        <div className="nickname-modal" onClick={(e) => e.stopPropagation()}>
          <h3 className="nickname-modal-title">参加方法を選択</h3>
          <p className="nickname-modal-room">「{pendingRoom.name}」に参加</p>
          <div className="nickname-modal-options">
            <button
              className="nickname-option nickname-option-anon"
              onClick={() => joinRoom(pendingRoom, true)}
            >
              <span className="nickname-option-icon">👤</span>
              <span className="nickname-option-label">匿名で参加</span>
              <span className="nickname-option-desc">匿名N として表示されます</span>
            </button>
            <button
              className="nickname-option nickname-option-name"
              onClick={() => joinRoom(pendingRoom, false)}
            >
              <span className="nickname-option-icon">😊</span>
              <span className="nickname-option-label">{loggedInUser.name} で参加</span>
              <span className="nickname-option-desc">自分の名前で表示されます</span>
            </button>
          </div>
          <button
            className="nickname-modal-cancel"
            onClick={() => {
              setShowNicknameModal(false);
              setPendingRoom(null);
            }}
          >
            キャンセル
          </button>
        </div>
      </div>
    );
  };

  // ======== Room List View ========
  if (!selectedRoom) {
    return (
      <div className="chat-page">
        <div className="chat-container">
          <div className="chat-header">
            <h2>匿名チャット</h2>
            <div className="chat-header-actions">
              {loggedInUser && (
                <span className="chat-user-badge">👤 {loggedInUser.name}</span>
              )}
              <button
                className="btn-create-room"
                onClick={() => setShowCreateRoom(!showCreateRoom)}
              >
                {showCreateRoom ? 'キャンセル' : '+ ルーム作成'}
              </button>
            </div>
          </div>

          {showCreateRoom && (
            <form className="create-room-form" onSubmit={handleCreateRoom}>
              <input
                type="text"
                placeholder="ルーム名"
                value={newRoomName}
                onChange={(e) => setNewRoomName(e.target.value)}
                className="room-input"
                maxLength={100}
              />
              <input
                type="text"
                placeholder="説明（任意）"
                value={newRoomDesc}
                onChange={(e) => setNewRoomDesc(e.target.value)}
                className="room-input"
              />
              <button type="submit" className="btn-submit-room">作成</button>
            </form>
          )}

          <div className="room-list">
            {rooms.length === 0 ? (
              <div className="no-rooms">
                <p>チャットルームがありません</p>
                <p>新しいルームを作成してみましょう！</p>
              </div>
            ) : (
              rooms.map(room => (
                <div
                  key={room.id}
                  className="room-card"
                  onClick={() => handleRoomClick(room)}
                >
                  <div className="room-info">
                    <h3 className="room-name">{room.name}</h3>
                    {room.description && (
                      <p className="room-description">{room.description}</p>
                    )}
                  </div>
                  <div className="room-meta">
                    <span className="room-users">
                      👥 {room.currentUsers || 0}人
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {renderNicknameModal()}
      </div>
    );
  }

  // ======== Chat Room View ========
  return (
    <div className="chat-page">
      <div className="chat-container chat-active">
        <div className="chat-room-header">
          <button className="btn-back" onClick={leaveRoom}>← 戻る</button>
          <div className="room-title-area">
            <h3>{selectedRoom.name}</h3>
            <span className="connection-status">
              {connected ? '🟢 接続中' : '🔴 接続中...'}
            </span>
          </div>
          <span className="my-nickname-small">{nickname}</span>
        </div>

        <div className="messages-area">
          {messages.map((msg, index) => {
            if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
              return (
                <div key={msg.id || index} className="system-message">
                  {msg.content}
                </div>
              );
            }
            const isMe = msg.senderNickname === nickname;
            return (
              <div key={msg.id || index} className={`message ${isMe ? 'message-mine' : 'message-other'}`}>
                {!isMe && (
                  <span className="message-sender">{msg.senderNickname}</span>
                )}
                <div className="message-bubble">
                  <p className="message-content">{msg.content}</p>
                  <span className="message-time">{formatTime(msg.createdAt)}</span>
                </div>
              </div>
            );
          })}
          <div ref={messagesEndRef} />
        </div>

        <form className="message-input-area" onSubmit={sendMessage}>
          <input
            type="text"
            placeholder="メッセージを入力..."
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            className="message-input"
            disabled={!connected}
            maxLength={500}
          />
          <button
            type="submit"
            className="btn-send"
            disabled={!connected || !inputMessage.trim()}
          >
            送信
          </button>
        </form>
      </div>
    </div>
  );
}

export default ChatPage;
