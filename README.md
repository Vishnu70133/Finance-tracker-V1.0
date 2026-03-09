FinTrack – AI Powered Personal Finance Management System

FinTrack is a full-stack personal finance management application that helps users track income, expenses, and financial trends.

The system includes an AI-powered assistant that allows users to manage transactions and query financial insights using natural language.

Example:

Add 500 food expense today
How much did I spend on food this month?
Update my health expense yesterday to 900
Delete my food expense yesterday
🚀 Features

Track income and expenses

Add, update, and delete transactions using AI commands

AI assistant for natural language financial queries

Hybrid AI system supporting both financial and general queries

Interactive dashboard with spending analytics

ChatGPT-style AI assistant interface

Secure authentication using JWT

Transaction filtering and pagination

Chat session history with persistent storage




🧠 AI Capabilities

The AI assistant allows users to manage finances using natural language.

Example Queries

Add transaction

Add 500 food expense today

Update transaction

Update my health expense yesterday to 900

Delete transaction

Delete my food expense yesterday

Financial insights

How much did I spend on food this month?
What are my top spending categories?
Analyze my spending

General queries

What is today's date?

The AI combines:

Natural language understanding

Backend financial logic

Database queries

This hybrid approach allows the assistant to answer both financial and general questions.




🏗️ Tech Stack
Frontend

ReactJS

Axios

CSS

Backend

Spring Boot

Spring Security

JWT Authentication

Spring AI

Database

MySQL




📊 System Architecture
React Frontend
      ↓
Spring Boot REST APIs
      ↓
AI Processing + Business Logic
      ↓
MySQL Database




🔐 Security

JWT-based authentication

Secure REST APIs

User-specific data access

Protected routes in frontend



📂 Project Structure
frontend/
   └── React application

backend/
   └── Spring Boot API
        ├── Controllers
        ├── Services
        ├── Repositories
        └── Entities


        
⚙️ Setup Instructions

1️⃣ Clone Repository
git clone https://github.com/yourusername/fintrack.git
cd fintrack

2️⃣ Backend Setup
cd backend
mvn spring-boot:run



Configure MySQL in:

application.properties
3️⃣ Frontend Setup
cd frontend
npm install
npm start

Frontend runs on:

http://localhost:3000

Backend runs on:

http://localhost:8080




💬 Example AI Commands

Add 200 food expense today
Update my health expense yesterday to 900
Delete my food expense yesterday
How much did I spend this month?
What are my top spending categories?
Analyze my spending
