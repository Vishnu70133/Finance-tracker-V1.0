package com.vishnu.finance_tracker.service;

import java.time.LocalDateTime;
import java.util.List;  
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnu.finance_tracker.dto.FinanceQueryDTO;
import com.vishnu.finance_tracker.model.ChatMessage;
import com.vishnu.finance_tracker.model.ChatSession;
import com.vishnu.finance_tracker.repository.ChatMessageRepository;
import com.vishnu.finance_tracker.repository.ChatSessionRepository;
import com.vishnu.finance_tracker.repository.UserRepository;

@Service
public class AiService {

@Autowired
private ChatMessageRepository chatMessageRepository;

@Autowired
private UserRepository userRepository;
@Autowired
private ChatSessionRepository chatSessionRepository;

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String getFinancialAdvice(String data) {

    return chatClient
            .prompt()
            .system("""
            You are a professional financial advisor.
            Give clear and short financial advice based on the user's spending data.
            Respond in simple language.
            """)
            .user(data)
            .call()
            .content();
}
public String generateFinancialInsights(String financialData) {

    return chatClient
            .prompt()
            .system("""
You are a professional financial advisor.

Analyze the user's financial data carefully.
Do NOT invent numbers.
Use ONLY the numbers provided in the data.

Give:
1. Spending analysis
2. Top spending category
3. Short financial advice
""")
            .user(financialData)
            .call()
            .content();
}
public FinanceQueryDTO interpretFinanceQuery(String question) {

    String result = chatClient
            .prompt()
            .system("""
You are a finance query interpreter.

Your task is to convert a user finance question into structured JSON.

Return ONLY raw JSON.
Do NOT include explanations.
Do NOT include markdown.
Do NOT include ```.

Possible intents:

CATEGORY_EXPENSE
BIGGEST_EXPENSE
TOTAL_EXPENSE
TOTAL_INCOME
NET_BALANCE
SORT_EXPENSES
HIGHEST_CATEGORY
LOWEST_CATEGORY
FILTER_AMOUNT
UPDATE_PROFILE
GET_PROFILE_NAME
GET_PROFILE_EMAIL


Examples:

User: How much did I spend on food last month?
{{
 "intent":"CATEGORY_EXPENSE",
 "category":"food",
 "timePeriod":"last_month"
}}

User: How much I spent on food in last month
{{
 "intent":"CATEGORY_EXPENSE",
 "category":"food",
 "timePeriod":"last_month"
}}

User: What was my food expense last month?
{{
 "intent":"CATEGORY_EXPENSE",
 "category":"food",
 "timePeriod":"last_month"
}}

User: Which category did I spend the most on last month?
{{
 "intent":"HIGHEST_CATEGORY",
 "timePeriod":"last_month"
}}

User: Which category did I spend the most on this year?
{{
 "intent":"HIGHEST_CATEGORY",
 "timePeriod":"this_year"
}}

User: Which category did I spend the most this month?
{{
 "intent":"HIGHEST_CATEGORY",
 "timePeriod":"this_month"
}}

User: Which category did I spend the most?
{{
 "intent":"HIGHEST_CATEGORY"
}}

User: Which category did I spend the least on last month?
{{
 "intent":"LOWEST_CATEGORY",
 "timePeriod":"last_month"
}}

User: Which category did I spend the least on this year?
{{
 "intent":"LOWEST_CATEGORY",
 "timePeriod":"this_year"
}}

User: Show expenses greater than 1000
{{
 "intent":"FILTER_AMOUNT",
 "comparison":"GREATER_THAN",
 "amount":1000
}}

User: Show expenses less than 500
{{
 "intent":"FILTER_AMOUNT",
 "comparison":"LESS_THAN",
 "amount":500
}}

User: Show expenses equal to 200
{{
 "intent":"FILTER_AMOUNT",
 "comparison":"EQUAL",
 "amount":200
}}

User: Give my expenses in ascending order
{{
 "intent":"SORT_EXPENSES",
 "order":"ASC"
}}

User: Give my expenses in descending order
{{
 "intent":"SORT_EXPENSES",
 "order":"DESC"
}}
 User: How much did I spend this week?
{{
 "intent":"TOTAL_EXPENSE",
 "timePeriod":"this_week"
}}

User: How much did I spend this month?
{{
 "intent":"TOTAL_EXPENSE",
 "timePeriod":"this_month"
}}

User: How much did I spend this year?
{{
 "intent":"TOTAL_EXPENSE",
 "timePeriod":"this_year"
}}

User: How much did I spend last month?
{{
 "intent":"TOTAL_EXPENSE",
 "timePeriod":"last_month"
}}
 User: How much did I spend yesterday?
{{
 "intent":"TOTAL_EXPENSE",
 "timePeriod":"yesterday"
}}

User: How much did I spend day before yesterday?
{{
 "intent":"TOTAL_EXPENSE",
 "timePeriod":"day_before_yesterday"
}}

User: How much did I spend last week?
{{
 "intent":"TOTAL_EXPENSE",
 "timePeriod":"last_week"
}}

User: How much did I spend last year?
{{
 "intent":"TOTAL_EXPENSE",
 "timePeriod":"last_year"
}}

User: How much did I spend on Feb 24?
{{
 "intent":"TOTAL_EXPENSE",
 "date":"2026-02-24"
}}
 User: What are my top 3 spending categories?
{{
 "intent":"TOP_CATEGORIES",
 "limit":3
}}

User: Show my top spending categories
{{
 "intent":"TOP_CATEGORIES",
 "limit":3
}}

User: Which categories do I spend the most on?
{{
 "intent":"TOP_CATEGORIES",
 "limit":3
}}

User: What are my least spending categories?
{{
 "intent":"LOWEST_CATEGORIES",
 "limit":3
}}
 User: What are my top 5 spending categories?
{{
 "intent":"TOP_CATEGORIES",
 "limit":5
}}

User: Show my top 10 spending categories
{{
 "intent":"TOP_CATEGORIES",
 "limit":10
}}

User: What are my least 3 spending categories?
{{
 "intent":"LOWEST_CATEGORIES",
 "limit":3
}}
 User: Did my spending increase compared to last month?
{{
 "intent":"SPENDING_TREND"
}}

User: Am I spending more than last month?
{{
 "intent":"SPENDING_TREND"
}}

User: Did my expenses increase this month?
{{
 "intent":"SPENDING_TREND"
}}
 NEW TRANSACTION INTENTS:

ADD_TRANSACTION
UPDATE_TRANSACTION
DELETE_TRANSACTION

Rules for transactions:
- Extract amount if mentioned.
- Extract category if mentioned (e.g. food, groceries, entertainment, health, transportation, salary, income).
- Extract description: Extract the clean semantic description of the item, purpose, or activity (e.g. "cinema", "pizza", "biryani", "food delivery", "movie ticket", "groceries"). Never include prepositions ("for", "on", "of"), amounts, or date text.
- Extract date if mentioned.
- Extract type (EXPENSE or INCOME).
- If date is relative (today, yesterday, tomorrow), return the relative term itself (e.g. "today", "yesterday", "tomorrow") in the "date" field.
- If date is an absolute date (e.g. "March 10 2026", "2026-03-10"), convert to ISO format "YYYY-MM-DD" and return in the "date" field.
- If information is missing, leave fields null

Examples:

User: Add an expense of 500 for food today
{{
 "intent":"ADD_TRANSACTION",
 "amount":500,
 "category":"food",
 "description":"food",
 "type":"EXPENSE",
 "date":"today"
}}

User: Add an expense of ₹500 for cinema today
{{
 "intent":"ADD_TRANSACTION",
 "amount":500,
 "category":"entertainment",
 "description":"cinema",
 "type":"EXPENSE",
 "date":"today"
}}

User: Add an expense of ₹500 for Biryani today.
{{
 "intent":"ADD_TRANSACTION",
 "amount":500,
 "category":"food",
 "description":"Biryani",
 "type":"EXPENSE",
 "date":"today"
}}

User: Add ₹300 for Pizza.
{{
 "intent":"ADD_TRANSACTION",
 "amount":300,
 "category":"food",
 "description":"Pizza",
 "type":"EXPENSE"
}}

User: Add an expense of 700 for dinner.
{{
 "intent":"ADD_TRANSACTION",
 "amount":700,
 "category":"food",
 "description":"dinner",
 "type":"EXPENSE"
}}

User: Spend ₹1200 on groceries.
{{
 "intent":"ADD_TRANSACTION",
 "amount":1200,
 "category":"groceries",
 "description":"groceries",
 "type":"EXPENSE"
}}

User: Add ₹500 Food expense for lunch.
{{
 "intent":"ADD_TRANSACTION",
 "amount":500,
 "category":"food",
 "description":"lunch",
 "type":"EXPENSE"
}}

User: Add 2000 income for salary
{{
 "intent":"ADD_TRANSACTION",
 "amount":2000,
 "category":"salary",
 "description":"salary",
 "type":"INCOME"
}}

User: Add 500 food
{{
 "intent":"ADD_TRANSACTION",
 "amount":500,
 "category":"food",
 "description":"food",
 "type":"EXPENSE"
}}

User: Update my food expense yesterday to 800
{{
 "intent":"UPDATE_TRANSACTION",
 "category":"food",
 "amount":800,
 "type":"EXPENSE",
 "date":"yesterday"
}}

User: Update entertainment expense on Feb 24 to 2000
{{
 "intent":"UPDATE_TRANSACTION",
 "category":"entertainment",
 "amount":2000,
 "type":"EXPENSE",
 "date":"2026-02-24"
}}

User: Delete my health expense from yesterday
{{
 "intent":"DELETE_TRANSACTION",
 "category":"health",
 "type":"EXPENSE",
 "date":"yesterday"
}}

User: Delete food expense on Feb 24
{{
 "intent":"DELETE_TRANSACTION",
 "category":"food",
 "date":"2026-02-24"
}}

User: Add 300 entertainment expense yesterday for movie
{{
 "intent":"ADD_TRANSACTION",
 "amount":300,
 "category":"entertainment",
 "type":"EXPENSE",
 "description":"movie",
 "date":"yesterday"
}}

User: Add 2000 salary income today for freelance work
{{
 "intent":"ADD_TRANSACTION",
 "amount":2000,
 "category":"salary",
 "type":"INCOME",
 "description":"freelance work",
 "date":"today"
}}

User: Update my food expense yesterday to 800 for lunch
{{
 "intent":"UPDATE_TRANSACTION",
 "category":"food",
 "amount":800,
 "type":"EXPENSE",
 "description":"lunch",
 "date":"yesterday"
}}

User: Update entertainment expense on Feb 24 to 2000 for movie
{{
 "intent":"UPDATE_TRANSACTION",
 "category":"entertainment",
 "amount":2000,
 "type":"EXPENSE",
 "description":"movie",
 "date":"2026-02-24"
}}

User: Update my health expense today to 500 for hospital checkup
{{
 "intent":"UPDATE_TRANSACTION",
 "category":"health",
 "amount":500,
 "type":"EXPENSE",
 "description":"hospital checkup",
 "date":"today"
}}

User: Update my food expense yesterday to 700
{{
 "intent":"UPDATE_TRANSACTION",
 "category":"food",
 "amount":700,
 "type":"EXPENSE",
 "date":"yesterday"
}}
 User: Delete my food expense yesterday
{{
 "intent":"DELETE_TRANSACTION",
 "category":"food",
 "type":"EXPENSE",
 "date":"yesterday"
}}
 User: Delete entertainment expense on Feb 24
{{
 "intent":"DELETE_TRANSACTION",
 "category":"entertainment",
 "type":"EXPENSE",
 "date":"2026-02-24"
}}
 User: Delete my health expense today
{{
 "intent":"DELETE_TRANSACTION",
 "category":"health",
 "type":"EXPENSE",
 "date":"today"
}}
 User: Analyze my spending
{{
 "intent":"ANALYZE_SPENDING"
}}

User: Change my name to Vishnu Kumar
{{
 "intent":"UPDATE_PROFILE",
 "profileField":"NAME",
 "newValue":"Vishnu Kumar"
}}

User: Update my name to Vishnu Das
{{
 "intent":"UPDATE_PROFILE",
 "profileField":"NAME",
 "newValue":"Vishnu Das"
}}

User: Change my email to newemail@example.com
{{
 "intent":"UPDATE_PROFILE",
 "profileField":"EMAIL",
 "newValue":"newemail@example.com"
}}

User: What is my name?
{{
 "intent":"GET_PROFILE_NAME"
}}

User: What's my name?
{{
 "intent":"GET_PROFILE_NAME"
}}

User: What is my email?
{{
 "intent":"GET_PROFILE_EMAIL"
}}

User: What email is associated with my account?
{{
 "intent":"GET_PROFILE_EMAIL"
}}

User: Which email did I register with?
{{
 "intent":"GET_PROFILE_EMAIL"
}}

User: Tell me my account email
{{
 "intent":"GET_PROFILE_EMAIL"
}}

User: What did I earn today?
{{
 "intent":"TOTAL_INCOME",
 "timePeriod":"today"
}}

User: What was my income today?
{{
 "intent":"TOTAL_INCOME",
 "timePeriod":"today"
}}

User: How much did I earn this month?
{{
 "intent":"TOTAL_INCOME",
 "timePeriod":"this_month"
}}

User: What was my net balance today?
{{
 "intent":"NET_BALANCE",
 "timePeriod":"today"
}}

User: What is my net balance this month?
{{
 "intent":"NET_BALANCE",
 "timePeriod":"this_month"
}}

User: How much balance do I have left?
{{
 "intent":"NET_BALANCE"
}}

User: What did I learn today?
{{
 "intent":null
}}

User: How are you doing?
{{
 "intent":null
}}

Rules for Ambiguous Queries:
- Distinguish between "earn" (which maps to TOTAL_INCOME) and "learn" (which is NOT a financial query, and maps to null intent).
- Queries about non-financial activities, general knowledge, or features not tracked by the application (e.g. learning, fitness, habits) MUST map to intent null.
""")
            .user(question)
            .call()
            .content();

    System.out.println("AI response: " + result);

    String cleanedJson = result.trim();
    int firstBrace = cleanedJson.indexOf('{');
    int lastBrace = cleanedJson.lastIndexOf('}');
    if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
        cleanedJson = cleanedJson.substring(firstBrace, lastBrace + 1);
    }

    ObjectMapper mapper = new ObjectMapper();

    try {
        return mapper.readValue(cleanedJson, FinanceQueryDTO.class);
    } catch (Exception e) {
        throw new RuntimeException("Failed to parse AI response: " + result, e);
    }
}

public String generalChat(Long sessionId, String question){

    // Build last conversation context
    String context = buildConversationContext(sessionId);

    String prompt = """
        You are a helpful AI assistant for a personal finance tracker application.

        Capabilities:
        • Answer finance related questions
        • Help with budgeting and saving advice
        • Answer general knowledge questions
        • Explain financial concepts simply

        Guidelines:
        • Use the previous conversation context if relevant.
        • Answer clearly and concisely.
        • Respond in natural language.
        • Do NOT return JSON unless explicitly requested.
        • If the user asks about their personal daily activities, learning, or non-financial habits that are not recorded in their transactions (e.g. "What did I learn today?", "Where did I go today?"), politely inform them that you only track their financial transactions (income and expenses) and do not have access to learning or non-financial activity logs.
        """;

    String response = chatClient
            .prompt()
            .system(prompt)
            .user(context + "\nuser: " + question)
            .call()
            .content();

    return response;
}
public ChatSession createSession(String email){

    ChatSession session = new ChatSession();

    session.setUserEmail(email);
    session.setTitle("New Chat");
    session.setCreatedAt(LocalDateTime.now());

    return chatSessionRepository.save(session);
}

public void saveUserMessage(Long sessionId, String message){

    ChatSession session =
            chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

    ChatMessage chat = new ChatMessage();

    chat.setSession(session);
    chat.setRole("user");
    chat.setMessage(message);
    chat.setCreatedAt(LocalDateTime.now());

    chatMessageRepository.save(chat);
}

public void saveAiMessage(Long sessionId, String message){

    ChatSession session =
            chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

    ChatMessage chat = new ChatMessage();

    chat.setSession(session);
    chat.setRole("ai");
    chat.setMessage(message);
    chat.setCreatedAt(LocalDateTime.now());

    chatMessageRepository.save(chat);
}

public String buildConversationContext(Long sessionId){

    List<ChatMessage> history =
            chatMessageRepository
            .findTop10BySessionIdOrderByCreatedAtDesc(sessionId);

    StringBuilder context = new StringBuilder();

    for(int i = history.size() - 1; i >= 0; i--){

        ChatMessage m = history.get(i);

        context.append(m.getRole())
               .append(": ")
               .append(m.getMessage())
               .append("\n");
    }

    return context.toString();
}

}