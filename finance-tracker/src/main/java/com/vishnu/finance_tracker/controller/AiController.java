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

        if (pendingQuery.getDescription() == null) {

            pendingQuery.setDescription(question.trim());

            response = transactionService.executePendingAction(email);

            saveAiMessage(session, response);

            return response;
        }
    }

    /* -------------------------------------------------------
       AI INTERPRETATION (SAFE)
    ------------------------------------------------------- */

    FinanceQueryDTO query;

    try {
        query = aiService.interpretFinanceQuery(question);
    } catch (Exception e) {

        System.out.println("AI parsing failed. Falling back to general chat.");

        response = aiService.generalChat(sessionId, question);

        saveAiMessage(session, response);

        return response;
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

        case "ANALYZE_SPENDING":

            String financialData =
                    transactionService.getFinancialSummaryForAI(email);

            response = aiService.generateFinancialInsights(financialData);
            break;

        default:
            response = aiService.generalChat(sessionId, question);
    }

    /* -------------------------------------------------------
       SAVE AI MESSAGE
    ------------------------------------------------------- */

    saveAiMessage(session, response);

    return response;
}

    /* -------------------------------------------------------
       CHAT HISTORY
    ------------------------------------------------------- */

    @GetMapping("/history/{sessionId}")
    public List<ChatMessage> getChatHistory(@PathVariable Long sessionId) {

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
}