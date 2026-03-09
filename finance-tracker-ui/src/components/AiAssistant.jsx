import { useState } from "react"
import api from "../api"

export default function AiAssistant() {

  const [messages, setMessages] = useState([])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)

  async function sendMessage() {

    if (!input.trim()) return

    const userMessage = { role: "user", text: input }

    setMessages(prev => [...prev, userMessage])
    setInput("")
    setLoading(true)

    try {

      const res = await api.post("/api/ai/query", {
        question: userMessage.text
      })

      const aiMessage = {
        role: "ai",
        text: typeof res.data === "string"
          ? res.data
          : JSON.stringify(res.data, null, 2)
      }

      setMessages(prev => [...prev, aiMessage])

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

  function handleKey(e) {
    if (e.key === "Enter") sendMessage()
  }

  return (
    <div className="ai-panel">

      <h3 className="ai-title">AI Finance Assistant</h3>

      <div className="ai-messages">

        {messages.map((m, i) => (
          <div
            key={i}
            className={m.role === "user" ? "ai-user" : "ai-bot"}
          >
            {m.text}
          </div>
        ))}

        {loading && (
          <div className="ai-bot">Thinking...</div>
        )}

      </div>

      <div className="ai-input-row">

        <input
          type="text"
          placeholder="Ask anything about your finances..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKey}
        />

        <button onClick={sendMessage}>
          Send
        </button>

      </div>

    </div>
  )
}