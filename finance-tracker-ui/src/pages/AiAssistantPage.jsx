import { useState, useRef, useEffect } from "react"
import api from "../api"
import "./AiAssistantPage.css"
import Navbar from "../components/Navbar"

export default function AiAssistantPage() {

  const [messages, setMessages] = useState([])
  const [sessions, setSessions] = useState([])
  const [currentSession, setCurrentSession] = useState(null)

  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)

  const chatEndRef = useRef(null)

  /* ---------------- AUTO SCROLL ---------------- */

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  /* ---------------- LOAD SESSIONS ---------------- */
async function loadSessions(){

  try{

    const res = await api.get("/api/ai/sessions")

    if(Array.isArray(res.data)){
      setSessions(res.data)

      if(res.data.length > 0){
        loadMessages(res.data[0].id)
      }else{
        createChat()
      }

    }else{
      console.error("Sessions API returned invalid data:", res.data)
      setSessions([])
      createChat()
    }

  }catch(err){
    console.error("Failed to load sessions", err)
    setSessions([])
  }

}
  useEffect(() => {

    loadSessions()

  }, [])

  /* ---------------- CREATE NEW CHAT ---------------- */

  async function createChat(){

    try{

      const res = await api.post("/api/ai/session")

      const newSession = res.data

      setSessions(prev => [newSession, ...prev])

      setCurrentSession(newSession.id)

      setMessages([
        {
          role:"ai",
          text:"Hi 👋 I am your Finance Assistant.\nHow can I help you today?"
        }
      ])

    }catch(err){
      console.error(err)
    }
  }

  /* ---------------- LOAD MESSAGES ---------------- */

  async function loadMessages(sessionId) {

  if (!sessionId) {
    console.error("Invalid sessionId:", sessionId)
    return
  }

  try {

    const res = await api.get(`/api/ai/messages/${sessionId}`)

    if (!Array.isArray(res.data)) {
      console.error("Invalid messages response:", res.data)
      setMessages([])
      return
    }

    const chats = res.data.map(m => ({
      role: m.role,
      text: m.message
    }))

    // Reset first to avoid UI mixing
    setMessages([])

    // Small delay ensures clean UI update
    setTimeout(() => {
      setMessages(chats)
    }, 0)

    setCurrentSession(sessionId)

  } catch (err) {

    console.error("Failed to load messages", err)

    setMessages([])

  }
}



  /* ---------------- SEND MESSAGE ---------------- */

  async function sendMessage(textOverride) {

  const message = (textOverride || input).trim()

  if (!message) return

  let sessionId = currentSession

  /* --------------------------------
     Create session automatically
  -------------------------------- */

  if (!sessionId) {

    try {

      const res = await api.post("/api/ai/session")

      const newSession = res.data

      sessionId = newSession.id

      setCurrentSession(sessionId)

      // put new chat at top
      setSessions(prev => [newSession, ...prev])

    } catch (err) {
      console.error(err)
      return
    }
  }

  /* --------------------------------
     Add user message to UI
  -------------------------------- */

  const userMessage = { role: "user", text: message }

  setMessages(prev => [...prev, userMessage])

  setInput("")

  /* --------------------------------
     Greeting handling
  -------------------------------- */

  const greetings = ["hi", "hello", "hey"]

  if (greetings.includes(message.toLowerCase())) {

    const aiGreeting = {
      role: "ai",
      text: "Hello 👋 How can I help you with your finances today?"
    }

    setMessages(prev => [...prev, aiGreeting])

    return
  }

  setLoading(true)

  /* --------------------------------
     Call AI API
  -------------------------------- */

  try {

    const res = await api.post("/api/ai/query", {
      question: message,
      sessionId: sessionId
    })

    const aiMessage = {
      role: "ai",
      text: typeof res.data === "string"
        ? res.data
        : JSON.stringify(res.data, null, 2)
    }

    setMessages(prev => [...prev, aiMessage])

    /* --------------------------------
       Refresh sessions so active chat
       moves to the top
    -------------------------------- */

    await loadSessions()

  } catch (err) {

    console.error(err)

    setMessages(prev => [
      ...prev,
      { role: "ai", text: "AI service failed." }
    ])

  } finally {

    setLoading(false)

  }
}

  /* ---------------- ENTER KEY HANDLING ---------------- */

  function handleKey(e){

    if(e.key === "Enter" && !e.shiftKey){
      e.preventDefault()
      sendMessage()
    }

  }

  /* ---------------- SUGGESTIONS ---------------- */

  const suggestions = [
    "Add 500 food expense today",
    "What are my top spending categories?",
    "How much did I spend last month?",
    "Did my spending increase compared to last month?"
  ]


  async function deleteChat(sessionId){

  try{

    await api.delete(`/api/ai/session/${sessionId}`)

    setSessions(prev => prev.filter(s => s.id !== sessionId))

    // if deleted chat was active
    if(sessionId === currentSession){

      setMessages([])
      setCurrentSession(null)

    }

  }catch(err){
    console.error("Failed to delete chat", err)
  }

}
  /* ---------------- UI ---------------- */

  return (

    <div className="ai-layout">

      {/* -------- SIDEBAR -------- */}

      <div className="ai-sidebar">

        <button
          className="new-chat-btn"
          onClick={createChat}
        >
          + New Chat
        </button>

       {sessions.map(s => (
  <div
    key={s.id}
    className={`chat-item ${s.id === currentSession ? "active" : ""}`}
  >

    {/* Clickable chat title */}
    <div
      className="chat-title"
      onClick={() => loadMessages(s.id)}
    >
      {s.title || "Chat"}
    </div>

    {/* Delete button */}
    <button
      className="delete-chat"
      onClick={(e) => {
        e.stopPropagation()
        deleteChat(s.id)
      }}
    >
      🗑
    </button>

  </div>
))}

      </div>

      {/* -------- CHAT AREA -------- */}

      <div className="ai-page">

        <Navbar />

        <div className="ai-header">
          <h2>🤖 AI Finance Assistant</h2>
        </div>

        <div className="ai-container">

          {/* CHAT */}

          <div className="ai-chat">

            {messages.map((m,i)=>(
              <div
                key={i}
                className={m.role === "user" ? "user-msg" : "ai-msg"}
              >
                {m.text}
              </div>
            ))}

            {messages.length === 1 && (
              <div className="ai-suggestions">

                {suggestions.map((s,i)=>(
                  <button
                    key={i}
                    onClick={()=>sendMessage(s)}
                    className="suggestion-btn"
                  >
                    {s}
                  </button>
                ))}

              </div>
            )}

            {loading && (
              <div className="ai-msg">Thinking...</div>
            )}

            <div ref={chatEndRef}></div>

          </div>

          {/* INPUT */}

          <div className="ai-input-container">

            <div className="ai-input-box">

              <input
                className="ai-input"
                value={input}
                onChange={(e)=>setInput(e.target.value)}
                onKeyDown={handleKey}
                placeholder="Message Finance Assistant..."
              />

              <button
                className="ai-send-btn"
                onClick={()=>sendMessage()}
              >
                ➤
              </button>

            </div>

          </div>

        </div>

      </div>

    </div>
  )
}