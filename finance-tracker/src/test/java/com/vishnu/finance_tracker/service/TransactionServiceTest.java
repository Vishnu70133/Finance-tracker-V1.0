package com.vishnu.finance_tracker.service;

import com.vishnu.finance_tracker.model.*;
import com.vishnu.finance_tracker.repository.*;
import com.vishnu.finance_tracker.service.TransactionService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void testCreateTransaction() {

        User user = new User();
        user.setEmail("test@gmail.com");

        Category category = new Category();
        category.setId(1L);

        Transaction transaction = new Transaction();
        transaction.setAmount(500.0);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(user);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(transaction);

        Transaction result = transactionService.createTransactionByEmail(
                "test@gmail.com",
                1L,
                transaction
        );

        assertNotNull(result);
        assertEquals(500.0, result.getAmount());
    }

    @Test
    void testUpdateTransaction() {

        // Step 1: Existing transaction
        Transaction existing = new Transaction();
        existing.setId(1L);
        existing.setAmount(500.0);
        User user = new User();
        user.setEmail("test@gmail.com");
        existing.setUser(user);

        // Step 2: Updated data
        Transaction updated = new Transaction();
        updated.setAmount(800.0);

        // Step 3: Mock DB
        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(existing);

        // Set Concrete Security Context to avoid Byte Buddy Java 24 issue
        org.springframework.security.core.context.SecurityContext secCtx = 
                new org.springframework.security.core.context.SecurityContextImpl();
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test@gmail.com", null, java.util.Collections.emptyList());
        secCtx.setAuthentication(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(secCtx);

        // Step 4: Call method
        Transaction result = transactionService.updateTransaction(1L, updated);

        // Step 5: Verify
        assertNotNull(result);
        assertEquals(800.0, result.getAmount());
    }
    @Test
    void testUpdateTransaction_NotFound() {

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.empty());

        Transaction updated = new Transaction();

        assertThrows(RuntimeException.class, () -> {
            transactionService.updateTransaction(1L, updated);
        });
    }

    @Test
    void testDeleteTransaction() {

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setDeleted(false);
        User user = new User();
        user.setEmail("test@gmail.com");
        transaction.setUser(user);

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(transaction);

        // Set Concrete Security Context to avoid Byte Buddy Java 24 issue
        org.springframework.security.core.context.SecurityContext secCtx = 
                new org.springframework.security.core.context.SecurityContextImpl();
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test@gmail.com", null, java.util.Collections.emptyList());
        secCtx.setAuthentication(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(secCtx);

        transactionService.deleteTransaction(1L);

        assertTrue(transaction.isDeleted());
    }

    @Test
    void testDeleteTransaction_NotFound() {

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            transactionService.deleteTransaction(1L);
        });
    }

    @Test
    void testGetHighestCategoryThisYear() {
        User user = new User();
        user.setId(123L);
        user.setEmail("test@gmail.com");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(user);
        
        java.time.LocalDate start = java.time.LocalDate.now().withDayOfYear(1);
        java.time.LocalDate end = java.time.LocalDate.now();
        
        when(transactionRepository.getHighestCategoryInDateRange(eq(123L), eq(start), eq(end)))
                .thenReturn("Food");

        String result = transactionService.getHighestCategory("test@gmail.com", "this_year", null);
        
        assertEquals("Your highest spending category this year was Food.", result);
    }

    @Test
    void testHandleAddTransactionToday() {
        User user = new User();
        user.setId(123L);
        user.setEmail("test@gmail.com");

        Category category = new Category();
        category.setName("Food");

        com.vishnu.finance_tracker.dto.FinanceQueryDTO query = new com.vishnu.finance_tracker.dto.FinanceQueryDTO();
        query.setAmount(500.0);
        query.setCategory("Food");
        query.setType("EXPENSE");
        query.setDescription("Biryani");
        query.setDate("today");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(user);
        when(categoryRepository.findByNameIgnoreCase("Food")).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        String result = transactionService.handleAddTransaction(query, "test@gmail.com");

        assertTrue(result.contains("Expense added"));
        
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getDate().equals(java.time.LocalDate.now()) &&
            transaction.getAmount() == 500.0 &&
            "Biryani".equals(transaction.getDescription())
        ));
    }

    @Test
    void testHandleAddTransactionYesterday() {
        User user = new User();
        user.setId(123L);
        user.setEmail("test@gmail.com");

        Category category = new Category();
        category.setName("Food");

        com.vishnu.finance_tracker.dto.FinanceQueryDTO query = new com.vishnu.finance_tracker.dto.FinanceQueryDTO();
        query.setAmount(500.0);
        query.setCategory("Food");
        query.setType("EXPENSE");
        query.setDescription("Biryani");
        query.setDate("yesterday");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(user);
        when(categoryRepository.findByNameIgnoreCase("Food")).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        String result = transactionService.handleAddTransaction(query, "test@gmail.com");

        assertTrue(result.contains("Expense added"));
        
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getDate().equals(java.time.LocalDate.now().minusDays(1))
        ));
    }

    @Test
    void testHandleUpdateProfileNameChange() {
        User user = new User();
        user.setId(123L);
        user.setName("Vishnu");
        user.setEmail("test@gmail.com");

        com.vishnu.finance_tracker.dto.FinanceQueryDTO query = new com.vishnu.finance_tracker.dto.FinanceQueryDTO();
        query.setIntent("UPDATE_PROFILE");
        query.setProfileField("NAME");
        query.setNewValue("Vishnu Kumar");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(user);

        String result = transactionService.handleUpdateProfile(query, "test@gmail.com");

        assertTrue(result.contains("I can change your name from Vishnu to Vishnu Kumar. Would you like me to proceed?"));
    }

    @Test
    void testHandleExecuteUpdateProfile() {
        User user = new User();
        user.setId(123L);
        user.setName("Vishnu");
        user.setEmail("test@gmail.com");

        com.vishnu.finance_tracker.dto.FinanceQueryDTO query = new com.vishnu.finance_tracker.dto.FinanceQueryDTO();
        query.setIntent("UPDATE_PROFILE");
        query.setProfileField("NAME");
        query.setNewValue("Vishnu Kumar");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(user);

        String result = transactionService.handleExecuteUpdateProfile(query, "test@gmail.com");

        assertEquals("Done. Your name has been updated to Vishnu Kumar.", result);
        verify(userRepository).save(argThat(u -> "Vishnu Kumar".equals(u.getName())));
    }

    @Test
    void testHandleUpdateProfileEmailChangeRejected() {
        User user = new User();
        user.setId(123L);
        user.setName("Vishnu");
        user.setEmail("test@gmail.com");

        com.vishnu.finance_tracker.dto.FinanceQueryDTO query = new com.vishnu.finance_tracker.dto.FinanceQueryDTO();
        query.setIntent("UPDATE_PROFILE");
        query.setProfileField("EMAIL");
        query.setNewValue("newemail@example.com");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(user);

        String result = transactionService.handleUpdateProfile(query, "test@gmail.com");

        assertTrue(result.contains("Email updates cannot be completed via AI as it would invalidate your active authentication session"));
    }

    @Test
    void testHandleAddTransactionCategoryMissing() {
        com.vishnu.finance_tracker.dto.FinanceQueryDTO query = new com.vishnu.finance_tracker.dto.FinanceQueryDTO();
        query.setAmount(500.0);
        query.setCategory(null);
        query.setType("EXPENSE");
        query.setDescription("Biryani");
        query.setDate("today");

        String result = transactionService.handleAddTransaction(query, "test@gmail.com");

        assertEquals("Please provide the category for the transaction.", result);
        assertNotNull(transactionService.getPendingAction("test@gmail.com"));
        assertEquals("ADD_TRANSACTION", transactionService.getPendingAction("test@gmail.com").getAction());
    }

    @Test
    void testHandleAddTransactionCategoryInvalidDescriptionPreservation() {

        com.vishnu.finance_tracker.dto.FinanceQueryDTO query = new com.vishnu.finance_tracker.dto.FinanceQueryDTO();
        query.setAmount(500.0);
        query.setCategory("Cake"); // Invalid category
        query.setType("EXPENSE");
        query.setDescription(null);
        query.setDate("today");

        when(categoryRepository.findByNameIgnoreCase("Cake")).thenReturn(java.util.Optional.empty());

        String result = transactionService.handleAddTransaction(query, "test@gmail.com");

        assertTrue(result.contains("Category 'Cake' not found. Please provide a valid category."));
        
        PendingAction pending = transactionService.getPendingAction("test@gmail.com");
        assertNotNull(pending);
        assertEquals("ADD_TRANSACTION", pending.getAction());
        assertNull(pending.getQuery().getCategory());
        assertEquals("Cake", pending.getQuery().getDescription());
    }

    @Test
    void testDetectDescriptionFromText() {
        assertEquals("cinema", transactionService.detectDescriptionFromText("Add an expense of ₹500 for cinema."));
        assertEquals("Biryani", transactionService.detectDescriptionFromText("Add an expense of ₹500 for Biryani today."));
        assertEquals("Pizza", transactionService.detectDescriptionFromText("Add ₹300 for Pizza."));
        assertEquals("dinner", transactionService.detectDescriptionFromText("Add an expense of 700 for dinner."));
        assertEquals("groceries", transactionService.detectDescriptionFromText("Spend ₹1200 on groceries."));
        assertEquals("food delivery", transactionService.detectDescriptionFromText("Add ₹500 for food delivery."));
    }

    @Test
    void testGetTotalExpenseForToday() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(user);
        when(transactionRepository.sumExpensesByDateRange(anyLong(), any(java.time.LocalDate.class), any(java.time.LocalDate.class)))
                .thenReturn(1000.0);

        String result = transactionService.getTotalExpense("test@gmail.com", "today", null);

        assertTrue(result.contains("were ₹1000"));
    }

    @Test
    void testGetTotalIncomeForToday() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(user);
        when(transactionRepository.sumIncomeByDateRange(anyLong(), any(java.time.LocalDate.class), any(java.time.LocalDate.class)))
                .thenReturn(15000.0);

        String result = transactionService.getTotalIncome("test@gmail.com", "today", null);

        assertTrue(result.contains("was ₹15000"));
    }

    @Test
    void testGetNetBalanceForToday() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(user);
        when(transactionRepository.sumIncomeByDateRange(anyLong(), any(java.time.LocalDate.class), any(java.time.LocalDate.class)))
                .thenReturn(15000.0);
        when(transactionRepository.sumExpensesByDateRange(anyLong(), any(java.time.LocalDate.class), any(java.time.LocalDate.class)))
                .thenReturn(1000.0);

        String result = transactionService.getNetBalance("test@gmail.com", "today", null);

        assertTrue(result.contains("is ₹14,000"));
        assertTrue(result.contains("earned ₹15,000"));
        assertTrue(result.contains("spent ₹1,000"));
    }
}