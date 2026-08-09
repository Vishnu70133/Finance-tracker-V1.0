package com.vishnu.finance_tracker.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import com.vishnu.finance_tracker.dto.FinanceQueryDTO;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    private ChatClient chatClient;
    private ChatClient.Builder builder;
    private AiService aiService;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        builder = mock(ChatClient.Builder.class);

        when(builder.build()).thenReturn(chatClient);
        aiService = new AiService(builder);
    }

    private void mockChatClientJson(String jsonResponse) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(jsonResponse);
    }

    @Test
    void testInterpretFinanceQuery_Spent() {
        mockChatClientJson("{\"intent\":\"TOTAL_EXPENSE\",\"timePeriod\":\"today\"}");
        FinanceQueryDTO result = aiService.interpretFinanceQuery("What did I spend today?");
        assertNotNull(result);
        assertEquals("TOTAL_EXPENSE", result.getIntent());
        assertEquals("today", result.getTimePeriod());
    }

    @Test
    void testInterpretFinanceQuery_Earn() {
        mockChatClientJson("{\"intent\":\"TOTAL_INCOME\",\"timePeriod\":\"today\"}");
        FinanceQueryDTO result = aiService.interpretFinanceQuery("What did I earn today?");
        assertNotNull(result);
        assertEquals("TOTAL_INCOME", result.getIntent());
        assertEquals("today", result.getTimePeriod());
    }

    @Test
    void testInterpretFinanceQuery_Learn() {
        mockChatClientJson("{\"intent\":null}");
        FinanceQueryDTO result = aiService.interpretFinanceQuery("What did I learn today?");
        assertNotNull(result);
        assertNull(result.getIntent());
    }

    @Test
    void testInterpretFinanceQuery_Balance() {
        mockChatClientJson("{\"intent\":\"NET_BALANCE\",\"timePeriod\":\"today\"}");
        FinanceQueryDTO result = aiService.interpretFinanceQuery("What was my net balance today?");
        assertNotNull(result);
        assertEquals("NET_BALANCE", result.getIntent());
        assertEquals("today", result.getTimePeriod());
    }

    @Test
    void testInterpretFinanceQuery_Remaining() {
        mockChatClientJson("{\"intent\":\"NET_BALANCE\"}");
        FinanceQueryDTO result = aiService.interpretFinanceQuery("How much balance do I have left?");
        assertNotNull(result);
        assertEquals("NET_BALANCE", result.getIntent());
    }
}
