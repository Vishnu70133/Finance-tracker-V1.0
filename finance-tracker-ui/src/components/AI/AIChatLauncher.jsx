import React, { useState, useRef, useEffect } from 'react';
import { useAIChat } from '../../context/AIChatContext';
import { useNavigate, useLocation } from 'react-router-dom';
import './AIChatLauncher.css';

export default function AIChatLauncher() {
  const {
    messages,
    loading,
    profileName,
    isOpen,
    setIsOpen,
    isAuthenticated,
    sendMessage
  } = useAIChat();

  const navigate = useNavigate();
  const location = useLocation();
  const [input, setInput] = useState("");
  const panelMessagesEndRef = useRef(null);

  // Auto-scroll inside floating panel
  useEffect(() => {
    if (isOpen) {
      panelMessagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isOpen]);

  // Hide floating launcher on the full AI page to avoid duplication
  if (!isAuthenticated || location.pathname === '/ai') {
    return null;
  }

  const handleSend = async (e) => {
    if (e) e.preventDefault();
    const text = input.trim();
    if (!text || loading) return;
    setInput("");
    await sendMessage(text);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleOpenFullChat = () => {
    setIsOpen(false);
    navigate('/ai');
  };

  const suggestions = [
    "What is my name?",
    "How much did I spend this month?",
    "Analyze my spending",
    "What's my highest spending category?"
  ];

  return (
    <div className="ai-launcher-container">
      {/* Floating Button */}
      <button className="ai-launcher-btn" onClick={() => setIsOpen(!isOpen)} title="Ask FinTrack AI">
        <span className="ai-launcher-icon">✨</span>
        <span className="ai-launcher-text">AI Chat</span>
      </button>

      {/* Floating Chat Panel */}
      {isOpen && (
        <div className="ai-launcher-panel">
          {/* Header */}
          <div className="ai-panel-header">
            <div className="ai-panel-brand">
              <span className="ai-panel-spark">✨</span>
              <span className="ai-panel-title-text">FinTrack AI</span>
            </div>
            <div className="ai-panel-header-actions">
              <button className="ai-panel-full-btn" onClick={handleOpenFullChat} title="Open full chat">
                ↗ Full Chat
              </button>
              <button className="ai-panel-close-btn" onClick={() => setIsOpen(false)} title="Close panel">
                ×
              </button>
            </div>
          </div>

          {/* Messages Area */}
          <div className="ai-panel-body">
            {messages.map((m, i) => (
              <div key={i} className={`ai-panel-msg-bubble ${m.role === 'user' ? 'user' : 'ai'}`}>
                {m.text}
              </div>
            ))}

            {messages.length === 1 && (
              <div className="ai-panel-suggestions">
                {suggestions.map((s, i) => (
                  <button key={i} className="ai-panel-suggest-btn" onClick={() => sendMessage(s)}>
                    {s}
                  </button>
                ))}
              </div>
            )}

            {loading && (
              <div className="ai-panel-msg-bubble ai loading">
                Thinking...
              </div>
            )}
            <div ref={panelMessagesEndRef} />
          </div>

          {/* Input Area */}
          <form className="ai-panel-footer" onSubmit={handleSend}>
            <textarea
              className="ai-panel-input"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask FinTrack anything..."
              rows={1}
            />
            <button className="ai-panel-send-btn" type="submit" disabled={loading || !input.trim()}>
              ➤
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
