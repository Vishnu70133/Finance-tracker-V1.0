import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api';

const AIChatContext = createContext(null);

export const useAIChat = () => useContext(AIChatContext);

export const AIChatProvider = ({ children }) => {
  const [messages, setMessages] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [currentSession, setCurrentSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [profileName, setProfileName] = useState("");
  const [isOpen, setIsOpen] = useState(false); // Quick Chat overlay panel state
  const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('token'));

  // Sync token authentication state
  useEffect(() => {
    const handleStorageChange = () => {
      setIsAuthenticated(!!localStorage.getItem('token'));
    };
    window.addEventListener('storage', handleStorageChange);
    const interval = setInterval(() => {
      setIsAuthenticated(!!localStorage.getItem('token'));
    }, 1000);
    return () => {
      window.removeEventListener('storage', handleStorageChange);
      clearInterval(interval);
    };
  }, []);

  const initProfileAndSessions = async () => {
    if (!localStorage.getItem('token')) return;
    try {
      const res = await api.get("/users/me");
      if (res.data && res.data.name) {
        setProfileName(res.data.name);
        await loadSessions(res.data.name);
      } else {
        await loadSessions("");
      }
    } catch (err) {
      console.error("Failed to fetch profile", err);
      await loadSessions("");
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      initProfileAndSessions();
    } else {
      setMessages([]);
      setSessions([]);
      setCurrentSession(null);
      setProfileName("");
      setIsOpen(false);
      localStorage.removeItem("activeChatSessionId");
    }
  }, [isAuthenticated]);

  const loadSessions = async (nameVal) => {
    if (!localStorage.getItem('token')) return;
    try {
      const res = await api.get("/api/ai/sessions");
      if (Array.isArray(res.data)) {
        setSessions(res.data);
        console.log("Available sessions:", res.data);
        const displayName = typeof nameVal === "string" ? nameVal : profileName;

        const savedId = localStorage.getItem("activeChatSessionId");
        console.log("Saved active session:", savedId);

        const savedSession = res.data.find(s => String(s.id) === savedId);
        console.log("Restoring session:", savedSession);

        if (savedSession) {
          await loadMessages(savedSession.id, displayName);
        } else if (res.data.length > 0) {
          const fallback = res.data[0];
          console.log("No saved session found. Fallback to:", fallback);
          localStorage.setItem("activeChatSessionId", String(fallback.id));
          await loadMessages(fallback.id, displayName);
        } else {
          await createChat(displayName);
        }
      } else {
        setSessions([]);
        await createChat(nameVal);
      }
    } catch (err) {
      console.error("Failed to load sessions", err);
      setSessions([]);
    }
  };

  const createChat = async (nameVal) => {
    if (!localStorage.getItem('token')) return;
    try {
      const res = await api.post("/api/ai/session");
      const newSession = res.data;
      setSessions(prev => [newSession, ...prev]);
      setCurrentSession(newSession.id);
      localStorage.setItem("activeChatSessionId", String(newSession.id));
      const displayName = typeof nameVal === "string" ? nameVal : profileName;
      setMessages([
        {
          role: "ai",
          text: displayName ? `Hi ${displayName} 👋\nHow can I help you today?` : "Hi 👋\nHow can I help you today?"
        }
      ]);
      return newSession.id;
    } catch (err) {
      console.error("Failed to create chat session", err);
    }
  };

  const loadMessages = async (sessionId, nameVal) => {
    if (!sessionId || !localStorage.getItem('token')) return;
    try {
      const res = await api.get(`/api/ai/messages/${sessionId}`);
      if (!Array.isArray(res.data)) {
        setMessages([]);
        return;
      }
      const chats = res.data.map(m => ({
        role: m.role,
        text: m.message
      }));
      if (chats.length === 0) {
        const displayName = typeof nameVal === "string" ? nameVal : profileName;
        setMessages([
          {
            role: "ai",
            text: displayName ? `Hi ${displayName} 👋\nHow can I help you today?` : "Hi 👋\nHow can I help you today?"
          }
        ]);
      } else {
        setMessages(chats);
      }
      setCurrentSession(sessionId);
      console.log("Selected session:", sessionId);
      localStorage.setItem("activeChatSessionId", String(sessionId));
    } catch (err) {
      console.error("Failed to load messages", err);
      setMessages([]);
    }
  };

  const sendMessage = async (messageText) => {
    const message = (messageText || "").trim();
    if (!message || loading || !localStorage.getItem('token')) return;

    let sessionId = currentSession;
    if (!sessionId) {
      sessionId = await createChat(profileName);
    }

    const userMessage = { role: "user", text: message };
    setMessages(prev => [...prev, userMessage]);

    const greetings = ["hi", "hello", "hey"];
    if (greetings.includes(message.toLowerCase())) {
      const aiGreeting = {
        role: "ai",
        text: "Hello 👋 How can I help you with your finances today?"
      };
      setMessages(prev => [...prev, aiGreeting]);
      return;
    }

    setLoading(true);

    try {
      const res = await api.post("/api/ai/query", {
        question: message,
        sessionId: sessionId
      });

      const aiMessage = {
        role: "ai",
        text: typeof res.data === "string"
          ? res.data
          : JSON.stringify(res.data, null, 2)
      };

      setMessages(prev => [...prev, aiMessage]);

      // Reload list to refresh titles/float active chat to the top
      const resSessions = await api.get("/api/ai/sessions");
      if (Array.isArray(resSessions.data)) {
        setSessions(resSessions.data);
      }

      // Refresh profile in case name was changed
      try {
        const profileRes = await api.get("/users/me");
        if (profileRes.data && profileRes.data.name) {
          setProfileName(profileRes.data.name);
        }
      } catch (profileErr) {
        console.error("Failed to refresh profile", profileErr);
      }

    } catch (err) {
      console.error(err);
      setMessages(prev => [
        ...prev,
        { role: "ai", text: "AI service failed." }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const deleteChat = async (sessionId) => {
    if (!localStorage.getItem('token')) return;
    try {
      await api.delete(`/api/ai/session/${sessionId}`);
      const updatedSessions = sessions.filter(s => s.id !== sessionId);
      setSessions(updatedSessions);
      if (sessionId === currentSession) {
        localStorage.removeItem("activeChatSessionId");
        if (updatedSessions.length > 0) {
          const fallback = updatedSessions[0];
          localStorage.setItem("activeChatSessionId", String(fallback.id));
          await loadMessages(fallback.id, profileName);
        } else {
          setMessages([]);
          setCurrentSession(null);
        }
      }
    } catch (err) {
      console.error("Failed to delete chat", err);
    }
  };

  return (
    <AIChatContext.Provider value={{
      messages,
      sessions,
      currentSession,
      loading,
      profileName,
      isOpen,
      setIsOpen,
      isAuthenticated,
      setIsAuthenticated,
      loadSessions,
      createChat,
      loadMessages,
      sendMessage,
      deleteChat,
      initProfileAndSessions
    }}>
      {children}
    </AIChatContext.Provider>
  );
};
