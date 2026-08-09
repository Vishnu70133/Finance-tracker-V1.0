package com.vishnu.finance_tracker.controller;

import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.vishnu.finance_tracker.dto.FinanceQueryDTO;
import com.vishnu.finance_tracker.model.ChatMessage;
import com.vishnu.finance_tracker.model.ChatSession;
import com.vishnu.finance_tracker.model.PendingAction;
import com.vishnu.finance_tracker.model.User;
import com.vishnu.finance_tracker.repository.ChatMessageRepository;
import com.vishnu.finance_tracker.repository.ChatSessionRepository;
import com.vishnu.finance_tracker.repository.UserRepository;
import com.vishnu.finance_tracker.service.AiService;
import com.vishnu.finance_tracker.service.TransactionService;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    private final AiService aiService;
    private final TransactionService transactionService;

    public AiController(AiService aiService, TransactionService transactionService) {
        this.aiService = aiService;
        this.transactionService = transactionService;
    }

    /* -------------------------------------------------------
       SIMPLE CHAT ENDPOINT
    ------------------------------------------------------- */

    @GetMapping("/chat")
    public String chat(@RequestParam String data) {
        return aiService.getFinancialAdvice(data);
    }

    /* -------------------------------------------------------
       FINANCIAL INSIGHTS
    ------------------------------------------------------- */

    @GetMapping("/my-insights")
    public String getMyInsights() {

        String email =
                (String) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        String financialData =
                transactionService.getFinancialSummaryForAI(email);

        System.out.println("DATA SENT TO AI:\n" + financialData);

        return aiService.generateFinancialInsights(financialData);
    }

    /* -------------------------------------------------------
       MAIN AI QUERY
    ------------------------------------------------------- */

   @PostMapping("/query")
public Object queryFinance(@RequestBody Map<String, Object> request) {
    try {
        Object questionObj = request.get("question");
    Object sessionObj = request.get("sessionId");

    if (questionObj == null) {
        return "Question is missing.";
    }

    if (sessionObj == null) {
        return "Session not found. Please refresh the page.";
    }

    String question = questionObj.toString();
    Long sessionId = Long.valueOf(sessionObj.toString());

    System.out.println("Question received: " + question);

    String email =
            (String) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

    System.out.println("User email: " + email);

    ChatSession session = chatSessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Chat session not found"));

    if (!session.getUserEmail().equals(email)) {
        throw new RuntimeException("Unauthorized");
    }

    /* -------------------------------------------------------
       SAVE USER MESSAGE
    ------------------------------------------------------- */

    ChatMessage userMessage = new ChatMessage();
    userMessage.setRole("user");
    userMessage.setMessage(question);
    userMessage.setSession(session);
    userMessage.setCreatedAt(LocalDateTime.now());

    chatMessageRepository.save(userMessage);
    session.setUpdatedAt(LocalDateTime.now());
    chatSessionRepository.save(session);

    if (isTargetingOtherUser(question, email)) {
        String ans = "I can only access or modify your own profile.";
        saveAiMessage(session, ans);
        return ans;
    }

    String lowerQ = question.toLowerCase().trim();
    if (lowerQ.contains("learn")) {
        String ans = "I don't currently track learning activity in your account.";
        saveAiMessage(session, ans);
        return ans;
    }

    /* -------------------------------------------------------
       DETERMINISTIC PROFILE QUERIES (BYPASS LLM)
    ------------------------------------------------------- */

    String trimmedQuestion = question.trim().toLowerCase();
    if (trimmedQuestion.matches("(?i)what is my name\\??|what's my name\\??|what name is on my account\\??|tell me my name\\??|get my name\\??")) {
        User user = userRepository.findByEmail(email);
        String ans = "Your name is " + user.getName() + ".";
        updateTitleIfNeeded(session, question, "GET_PROFILE_NAME", null);
        saveAiMessage(session, ans);
        return ans;
    }
    if (trimmedQuestion.matches("(?i)what is my email\\??|what's my email\\??|what email did i register with\\??|what is my registered email\\??|tell me my email\\??|what email is associated with my account\\??|tell me my account email\\??|get my email\\??")) {
        User user = userRepository.findByEmail(email);
        String ans = "Your email is " + user.getEmail() + ".";
        updateTitleIfNeeded(session, question, "GET_PROFILE_EMAIL", null);
        saveAiMessage(session, ans);
        return ans;
    }
    if (trimmedQuestion.matches("(?i)what is my password\\??|what's my password\\??|tell me my password\\??|expose my password\\??")) {
        String ans = "For security reasons, your password cannot be exposed.";
        saveAiMessage(session, ans);
        return ans;
    }
    if (trimmedQuestion.matches("(?i)tell me about my profile\\??|what is my profile\\??|show my profile\\??")) {
        User user = userRepository.findByEmail(email);
        String ans = "Profile details:\nName: " + user.getName() + "\nEmail: " + user.getEmail();
        updateTitleIfNeeded(session, question, "GET_PROFILE", null);
        saveAiMessage(session, ans);
        return ans;
    }
    if (trimmedQuestion.matches("(?i)tell me the profile of user \\d+\\??|tell me about user \\d+\\??")) {
        String ans = "Unauthorized: You can only query your own profile details.";
        saveAiMessage(session, ans);
        return ans;
    }
    if (trimmedQuestion.matches("(?i)change user \\d+'s name to .*|update user \\d+'s name to .*")) {
        String ans = "Unauthorized: You can only request profile changes for your own account.";
        saveAiMessage(session, ans);
        return ans;
    }

    Object response;

    /* -------------------------------------------------------
       STEP 1 CONFIRMATION
    ------------------------------------------------------- */

    if (question.matches("(?i)yes|ok|okay|sure|confirm|do it")) {

        PendingAction pending = transactionService.getPendingAction(email);

        if (pending != null) {

            response = transactionService.executePendingAction(email);

            saveAiMessage(session, response);

            return response;
        }
    }

    /* -------------------------------------------------------
       STEP 2 PENDING DESCRIPTION
    ------------------------------------------------------- */

    PendingAction pending = transactionService.getPendingAction(email);

    if (pending != null && "ADD_TRANSACTION".equals(pending.getAction())) {

        FinanceQueryDTO pendingQuery = pending.getQuery();

        if (pendingQuery.getAmount() == null) {
            try {
                String cleaned = question.replaceAll("[^0-9.]", "");
                pendingQuery.setAmount(Double.parseDouble(cleaned));
                response = transactionService.executePendingAction(email);
                updateTitleIfNeeded(session, question, "ADD_TRANSACTION", pendingQuery);
                saveAiMessage(session, response);
                return response;
            } catch (Exception e) {
                // If it is not a valid number, let it fall through
            }
        } else if (pendingQuery.getCategory() == null) {

            pendingQuery.setCategory(question.trim());

            response = transactionService.executePendingAction(email);
            updateTitleIfNeeded(session, question, "ADD_TRANSACTION", pendingQuery);
            saveAiMessage(session, response);
            return response;

        } else if (pendingQuery.getDescription() == null) {

            pendingQuery.setDescription(question.trim());

            response = transactionService.executePendingAction(email);
            updateTitleIfNeeded(session, question, "ADD_TRANSACTION", pendingQuery);
            saveAiMessage(session, response);
            return response;
        }
    }

    /* -------------------------------------------------------
       INTENT DETECTION & ROUTING
    ------------------------------------------------------- */

    FinanceQueryDTO query = parseIntentLocally(question);
    boolean isHandledLocally = (query.getIntent() != null);

    System.out.println("[AI CHAT] Question: " + question);

    if (isHandledLocally) {
        System.out.println("[AI CHAT] Detected intent locally: " + query.getIntent());
        System.out.println("[AI CHAT] Handled locally/backend: true");
    } else {
        System.out.println("[AI CHAT] Sending query to ML/AI service for interpretation.");
        try {
            query = aiService.interpretFinanceQuery(question);
            System.out.println("[AI CHAT] Detected intent from AI service: " + (query != null ? query.getIntent() : "null"));
            System.out.println("[AI CHAT] Handled locally/backend: false");
        } catch (Exception e) {
            System.out.println("[AI CHAT] ML service response/error: " + e.getMessage());
            String ans = "I’m unable to process that request right now because the AI service is unavailable.";
            System.out.println("[AI CHAT] Final response: " + ans);
            saveAiMessage(session, ans);
            return ans;
        }
    }

    /* -------------------------------------------------------
       CATEGORY AUTO DETECTION
    ------------------------------------------------------- */

    String detectedCategory =
            transactionService.detectCategoryFromText(question);

    if (detectedCategory != null) {

        boolean categoryExists =
                transactionService.categoryExists(query.getCategory());

        if (query.getCategory() == null || !categoryExists) {
            query.setCategory(detectedCategory);
        }
    }

    /* -------------------------------------------------------
       DESCRIPTION AUTO DETECTION
    ------------------------------------------------------- */

    if (query.getDescription() == null) {

        String description =
                transactionService.detectDescriptionFromText(question);

        if (description != null) {
            query.setDescription(description);
        }
    }

    if (query.getIntent() == null) {

        response = aiService.generalChat(sessionId, question);

        saveAiMessage(session, response);

        return response;
    }

    /* -------------------------------------------------------
       INTENT SWITCH
    ------------------------------------------------------- */

    switch (query.getIntent()) {

        case "CATEGORY_EXPENSE":

    response = transactionService.getCategoryExpense(
            email,
            query.getCategory(),
            query.getTimePeriod(),
            query.getDate()
    );

    break;

        case "BIGGEST_EXPENSE":
            response = transactionService.getBiggestExpenseLastMonth(email);
            break;

        case "TOTAL_EXPENSE":
            response = transactionService.getTotalExpense(
                    email,
                    query.getTimePeriod(),
                    query.getDate()
            );
            break;

        case "TOTAL_INCOME":
            response = transactionService.getTotalIncome(
                    email,
                    query.getTimePeriod(),
                    query.getDate()
            );
            break;

        case "NET_BALANCE":
            response = transactionService.getNetBalance(
                    email,
                    query.getTimePeriod(),
                    query.getDate()
            );
            break;

        case "SORT_EXPENSES":
            response = transactionService.getExpensesSorted(
                    email,
                    query.getOrder()
            );
            break;

        case "HIGHEST_CATEGORY":
            response = transactionService.getHighestCategory(
                    email,
                    query.getTimePeriod(),
                    query.getDate()
            );
            break;

        case "LOWEST_CATEGORY":
            response = transactionService.getLowestCategory(
                    email,
                    query.getTimePeriod(),
                    query.getDate()
            );
            break;

        case "FILTER_AMOUNT":
            response = transactionService.filterExpensesByAmount(
                    email,
                    query.getComparison(),
                    query.getAmount()
            );
            break;

        case "TOP_CATEGORIES":
            response = transactionService.getTopCategories(email, query.getLimit());
            break;

        case "LOWEST_CATEGORIES":
            response = transactionService.getLowestCategories(email, query.getLimit());
            break;

        case "SPENDING_TREND":
            response = transactionService.compareSpendingWithLastMonth(email);
            break;

        case "ADD_TRANSACTION":
            response = transactionService.handleAddTransaction(query, email);
            break;

        case "UPDATE_TRANSACTION":
            response = transactionService.handleUpdateTransaction(query, email);
            break;

        case "DELETE_TRANSACTION":
            response = transactionService.handleDeleteTransaction(query, email);
            break;

        case "UPDATE_PROFILE":
            response = transactionService.handleUpdateProfile(query, email);
            break;

        case "GET_PROFILE_NAME": {
            User user = userRepository.findByEmail(email);
            response = "Your name is " + user.getName() + ".";
            break;
        }

        case "GET_PROFILE_EMAIL": {
            User user = userRepository.findByEmail(email);
            response = "Your email is " + user.getEmail() + ".";
            break;
        }

        case "ANALYZE_SPENDING":

            String financialData =
                    transactionService.getFinancialSummaryForAI(email);

            response = aiService.generateFinancialInsights(financialData);
            break;

        default:
            try {
                System.out.println("[AI CHAT] Sending general chat query to ML/AI service.");
                response = aiService.generalChat(sessionId, question);
            } catch (Exception e) {
                System.out.println("[AI CHAT] ML service response/error: " + e.getMessage());
                response = "I’m unable to process that request right now because the AI service is unavailable.";
            }
    }

    /* -------------------------------------------------------
       SAVE AI MESSAGE
    ------------------------------------------------------- */

    updateTitleIfNeeded(session, question, query != null ? query.getIntent() : null, query);

    saveAiMessage(session, response);

    System.out.println("[AI CHAT] Final response: " + response);

    return response;
    } catch (Exception e) {
        e.printStackTrace();
        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred: " + e.getMessage());
    }
}

    /* -------------------------------------------------------
       CHAT HISTORY
    ------------------------------------------------------- */

    @GetMapping("/history/{sessionId}")
    public List<ChatMessage> getChatHistory(@PathVariable Long sessionId) {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));

        if (!session.getUserEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        return chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /* -------------------------------------------------------
       CREATE CHAT SESSION
    ------------------------------------------------------- */

    @PostMapping("/session")
    public ChatSession createSession() {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        ChatSession session = new ChatSession(email);

        return chatSessionRepository.save(session);
    }

    /* -------------------------------------------------------
       GET ALL USER SESSIONS
    ------------------------------------------------------- */

    @GetMapping("/sessions")
public List<ChatSession> getSessions() {

    String email = (String) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    return chatSessionRepository
            .findByUserEmailOrderByUpdatedAtDesc(email);
}

    /* -------------------------------------------------------
       GET SESSION MESSAGES
    ------------------------------------------------------- */

    @GetMapping("/messages/{sessionId}")
    public List<ChatMessage> getMessages(@PathVariable Long sessionId) {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));

        if (!session.getUserEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        return chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /* -------------------------------------------------------
       SAVE AI MESSAGE
    ------------------------------------------------------- */

    private void saveAiMessage(ChatSession session, Object response) {

        ChatMessage aiMessage = new ChatMessage();

        aiMessage.setRole("ai");
        aiMessage.setMessage(response.toString());
        aiMessage.setSession(session);
        aiMessage.setCreatedAt(LocalDateTime.now());

        chatMessageRepository.save(aiMessage);
    }

    @DeleteMapping("/session/{sessionId}")
public String deleteSession(@PathVariable Long sessionId){

    String email = (String) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    ChatSession session = chatSessionRepository
            .findById(sessionId)
            .orElseThrow();

    // Security check: user can delete only their session
    if(!session.getUserEmail().equals(email)){
        return "Unauthorized";
    }

    chatSessionRepository.delete(session);

    return "Chat deleted successfully";
}

@PutMapping("/session/{sessionId}/title")
public ChatSession updateSessionTitle(@PathVariable Long sessionId, @RequestBody Map<String, String> body) {
    String email = (String) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    ChatSession session = chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Chat session not found"));

    if (!session.getUserEmail().equals(email)) {
        throw new RuntimeException("Unauthorized");
    }

    String newTitle = body.get("title");
    if (newTitle != null && !newTitle.trim().isEmpty()) {
        session.setTitle(newTitle.trim());
        return chatSessionRepository.save(session);
    }
    return session;
}

    private boolean isTargetingOtherUser(String question, String email) {
        String lowerQ = question.toLowerCase();

        // Pattern like "user 1", "user 5", "user 123"
        java.util.regex.Pattern userPattern = java.util.regex.Pattern.compile("\\buser\\s+(\\d+)\\b");
        java.util.regex.Matcher userMatcher = userPattern.matcher(lowerQ);
        if (userMatcher.find()) {
            String targetIdStr = userMatcher.group(1);
            User currentUser = userRepository.findByEmail(email);
            if (currentUser == null || !currentUser.getId().toString().equals(targetIdStr)) {
                return true;
            }
        }

        // Pattern like "user id 1", "user id: 1"
        java.util.regex.Pattern userIdPattern = java.util.regex.Pattern.compile("\\buser\\s+id\\s*[:=]?\\s*(\\d+)\\b");
        java.util.regex.Matcher userIdMatcher = userIdPattern.matcher(lowerQ);
        if (userIdMatcher.find()) {
            String targetIdStr = userIdMatcher.group(1);
            User currentUser = userRepository.findByEmail(email);
            if (currentUser == null || !currentUser.getId().toString().equals(targetIdStr)) {
                return true;
            }
        }

        // Scan for any email address
        java.util.regex.Pattern emailPattern = java.util.regex.Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        java.util.regex.Matcher emailMatcher = emailPattern.matcher(lowerQ);
        while (emailMatcher.find()) {
            String foundEmail = emailMatcher.group();
            if (!foundEmail.equalsIgnoreCase(email)) {
                return true;
            }
        }

        // Also look for keywords pointing to another user
        if (lowerQ.contains("another user") || lowerQ.contains("other user") || lowerQ.contains("different user")) {
            return true;
        }

        return false;
    }

    private void updateTitleIfNeeded(ChatSession session, String question, String intent, FinanceQueryDTO query) {
        if (session.getTitle() == null || session.getTitle().equalsIgnoreCase("New Chat")) {
            String lowerQ = question.trim().toLowerCase();
            if (!lowerQ.matches("(?i)yes|no|ok|okay|sure|confirm|do it|cancel|stop|hello|hi|hey")) {
                String title = generateChatTitle(question, intent, query);
                session.setTitle(title);
                session.setUpdatedAt(java.time.LocalDateTime.now());
                chatSessionRepository.save(session);
            }
        }
    }

    String generateChatTitle(String question, String intent, FinanceQueryDTO query) {
        if (intent != null) {
            switch (intent) {
                case "ADD_TRANSACTION": {
                    if (query != null && query.getDescription() != null && !query.getDescription().isEmpty()) {
                        return capitalizeWord(query.getDescription()) + " Expense";
                    }
                    if (query != null && query.getCategory() != null && !query.getCategory().isEmpty()) {
                        return capitalizeWord(query.getCategory()) + " Expense";
                    }
                    String category = transactionService.detectCategoryFromText(question);
                    if (category != null) {
                        return capitalizeWord(category) + " Expense";
                    }
                    return "New Expense";
                }
                case "CATEGORY_EXPENSE": {
                    if (query != null && query.getCategory() != null && !query.getCategory().isEmpty()) {
                        return capitalizeWord(query.getCategory()) + " Spending";
                    }
                    String category = transactionService.detectCategoryFromText(question);
                    if (category != null) {
                        return capitalizeWord(category) + " Spending";
                    }
                    return "Category Spending";
                }
                case "TOTAL_EXPENSE":
                    return "Expense Summary";
                case "TOTAL_INCOME":
                    return "Income Summary";
                case "NET_BALANCE":
                    return "Net Balance";
                case "BIGGEST_EXPENSE":
                    return "Biggest Expense";
                case "HIGHEST_CATEGORY":
                    return "Highest Spending Category";
                case "LOWEST_CATEGORY":
                    return "Lowest Spending Category";
                case "TOP_CATEGORIES":
                    return "Top Spending Categories";
                case "LOWEST_CATEGORIES":
                    return "Lowest Spending Categories";
                case "SPENDING_TREND":
                    return "Spending Trend";
                case "ANALYZE_SPENDING":
                    return "Spending Analysis";
                case "UPDATE_PROFILE":
                    return "Profile Update";
                case "GET_PROFILE_NAME":
                case "GET_PROFILE_EMAIL":
                case "GET_PROFILE":
                    return "Profile Information";
            }
        }

        String lowerQ = question.toLowerCase();
        if (lowerQ.contains("predict") || lowerQ.contains("forecast")) {
            return "Spending Prediction";
        }
        if (lowerQ.contains("anomaly") || lowerQ.contains("anomalies") || lowerQ.contains("suspicious")) {
            return "Spending Anomalies";
        }
        if (lowerQ.contains("name") && (lowerQ.contains("change") || lowerQ.contains("update"))) {
            return "Profile Update";
        }
        if (lowerQ.contains("name") || lowerQ.contains("email") || lowerQ.contains("profile")) {
            return "Profile Information";
        }
        if (lowerQ.contains("highest") || lowerQ.contains("most")) {
            return "Highest Spending Category";
        }
        if (lowerQ.contains("lowest") || lowerQ.contains("least")) {
            return "Lowest Spending Category";
        }
        if (lowerQ.contains("spend") || lowerQ.contains("expense")) {
            String category = transactionService.detectCategoryFromText(question);
            if (category != null) {
                return capitalizeWord(category) + " Spending";
            }
            return "Spending Analysis";
        }

        return cleanAndTruncateQuestion(question);
    }

    private String capitalizeWord(String str) {
        if (str == null || str.isEmpty()) return "";
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(w.substring(0, 1).toUpperCase())
              .append(w.substring(1).toLowerCase())
              .append(" ");
        }
        return sb.toString().trim();
    }

    String cleanAndTruncateQuestion(String question) {
        String cleaned = question.replaceAll("[^a-zA-Z0-9\\s₹$€£]", "").trim();
        String[] words = cleaned.split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return "New Chat";
        }
        
        int count = Math.min(words.length, 5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(words[i]).append(" ");
        }
        String result = sb.toString().trim();
        if (result.length() > 30) {
            result = result.substring(0, 27) + "...";
        }
        return result;
    }

    FinanceQueryDTO parseIntentLocally(String question) {
        FinanceQueryDTO query = new FinanceQueryDTO();
        String lowerQ = question.toLowerCase();

        // 1. Detect Time Period
        String timePeriod = null;
        if (lowerQ.contains("today")) {
            timePeriod = "today";
        } else if (lowerQ.contains("yesterday")) {
            timePeriod = "yesterday";
        } else if (lowerQ.contains("this week")) {
            timePeriod = "this_week";
        } else if (lowerQ.contains("last week")) {
            timePeriod = "last_week";
        } else if (lowerQ.contains("this month")) {
            timePeriod = "this_month";
        } else if (lowerQ.contains("last month")) {
            timePeriod = "last_month";
        } else if (lowerQ.contains("this year")) {
            timePeriod = "this_year";
        } else if (lowerQ.contains("last year")) {
            timePeriod = "last_year";
        }
        query.setTimePeriod(timePeriod);

        // 2. Detect Category
        String detectedCategory = transactionService.detectCategoryFromText(question);
        if (detectedCategory != null) {
            query.setCategory(detectedCategory);
        }

        // 3. Detect Intent
        if (lowerQ.contains("net balance") || lowerQ.contains("balance") || lowerQ.contains("remaining") || lowerQ.contains("income minus expenses")) {
            query.setIntent("NET_BALANCE");
        } else if (lowerQ.contains("earn") || lowerQ.contains("earned") || lowerQ.contains("income") || lowerQ.contains("salary") || lowerQ.contains("inflow")) {
            query.setIntent("TOTAL_INCOME");
        } else if (lowerQ.contains("highest") || lowerQ.contains("most")) {
            query.setIntent("HIGHEST_CATEGORY");
        } else if (lowerQ.contains("lowest") || lowerQ.contains("least")) {
            query.setIntent("LOWEST_CATEGORY");
        } else if (lowerQ.contains("spend") || lowerQ.contains("spent") || lowerQ.contains("expense") || lowerQ.contains("expenses") || lowerQ.contains("spending") || lowerQ.contains("outflow")) {
            if (detectedCategory != null) {
                query.setIntent("CATEGORY_EXPENSE");
            } else {
                query.setIntent("TOTAL_EXPENSE");
            }
        }

        return query;
    }
}