package com.vishnu.finance_tracker.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vishnu.finance_tracker.dto.FinanceQueryDTO;
import com.vishnu.finance_tracker.service.TransactionService;

class AiControllerTest {

    private TransactionService transactionService;
    private AiController aiController;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService() {
            @Override
            public String detectCategoryFromText(String question) {
                String lower = question.toLowerCase();
                if (lower.contains("food")) return "Food";
                if (lower.contains("entertainment")) return "entertainment";
                if (lower.contains("travel")) return "travel";
                return null;
            }
        };
        aiController = new AiController(null, transactionService);
    }

    @Test
    void testGenerateChatTitle_WithIntents() {
        // ADD_TRANSACTION with detected category
        assertEquals("Food Expense", aiController.generateChatTitle("Add 500 for Food today", "ADD_TRANSACTION", null));

        // ADD_TRANSACTION without category
        assertEquals("New Expense", aiController.generateChatTitle("Add 500 today", "ADD_TRANSACTION", null));

        // CATEGORY_EXPENSE with category
        assertEquals("Entertainment Spending", aiController.generateChatTitle("What did I spend on entertainment?", "CATEGORY_EXPENSE", null));

        // Intent direct maps
        assertEquals("Expense Summary", aiController.generateChatTitle("Show total expense", "TOTAL_EXPENSE", null));
        assertEquals("Spending Trend", aiController.generateChatTitle("spending trend", "SPENDING_TREND", null));
        assertEquals("Spending Analysis", aiController.generateChatTitle("analyze my spending", "ANALYZE_SPENDING", null));
        assertEquals("Profile Update", aiController.generateChatTitle("change name to Vishnu", "UPDATE_PROFILE", null));
        assertEquals("Profile Information", aiController.generateChatTitle("what is my name?", "GET_PROFILE_NAME", null));
    }

    @Test
    void testGenerateChatTitle_WithQueryData() {
        // ADD_TRANSACTION with description present in query
        FinanceQueryDTO query1 = new FinanceQueryDTO();
        query1.setIntent("ADD_TRANSACTION");
        query1.setDescription("Cake");
        assertEquals("Cake Expense", aiController.generateChatTitle("Add 500 for Cake today", "ADD_TRANSACTION", query1));

        // ADD_TRANSACTION with only category present in query
        FinanceQueryDTO query2 = new FinanceQueryDTO();
        query2.setIntent("ADD_TRANSACTION");
        query2.setCategory("Food");
        assertEquals("Food Expense", aiController.generateChatTitle("Add 500 for Food today", "ADD_TRANSACTION", query2));

        // CATEGORY_EXPENSE with category present in query
        FinanceQueryDTO query3 = new FinanceQueryDTO();
        query3.setIntent("CATEGORY_EXPENSE");
        query3.setCategory("Groceries");
        assertEquals("Groceries Spending", aiController.generateChatTitle("groceries spending", "CATEGORY_EXPENSE", query3));
    }

    @Test
    void testGenerateChatTitle_KeywordDetection() {
        // Keyword predict
        assertEquals("Spending Prediction", aiController.generateChatTitle("Can you predict next month?", null, null));

        // Keyword anomaly
        assertEquals("Spending Anomalies", aiController.generateChatTitle("Detect anomalies", null, null));

        // Keyword profile
        assertEquals("Profile Information", aiController.generateChatTitle("what email is registered?", null, null));

        // Keyword spending with category
        assertEquals("Travel Spending", aiController.generateChatTitle("How much did I spend on travel?", null, null));
    }

    @Test
    void testCleanAndTruncateQuestion() {
        // Standard truncation to first 5 words
        assertEquals("Which category did I spend", aiController.cleanAndTruncateQuestion("Which category did I spend the most on?"));

        // Remove special characters
        assertEquals("Add 500 expense for Biryani", aiController.cleanAndTruncateQuestion("Add 500 expense for Biryani!!!"));

        // Fallback for empty
        assertEquals("New Chat", aiController.cleanAndTruncateQuestion("!!!"));
    }

    @Test
    void testParseIntentLocally() {
        // A. Today's expense
        FinanceQueryDTO qA = aiController.parseIntentLocally("What did I spend today?");
        assertEquals("TOTAL_EXPENSE", qA.getIntent());
        assertEquals("today", qA.getTimePeriod());

        // B. Today's income
        FinanceQueryDTO qB = aiController.parseIntentLocally("What did I earn today?");
        assertEquals("TOTAL_INCOME", qB.getIntent());
        assertEquals("today", qB.getTimePeriod());

        // C. Today's net balance
        FinanceQueryDTO qC = aiController.parseIntentLocally("What was my net balance today?");
        assertEquals("NET_BALANCE", qC.getIntent());
        assertEquals("today", qC.getTimePeriod());

        // D. Highest category
        FinanceQueryDTO qD = aiController.parseIntentLocally("Which category did I spend the most on?");
        assertEquals("HIGHEST_CATEGORY", qD.getIntent());

        // E. Highest category this year
        FinanceQueryDTO qE = aiController.parseIntentLocally("Which category did I spend the most on this year?");
        assertEquals("HIGHEST_CATEGORY", qE.getIntent());
        assertEquals("this_year", qE.getTimePeriod());
    }
}
