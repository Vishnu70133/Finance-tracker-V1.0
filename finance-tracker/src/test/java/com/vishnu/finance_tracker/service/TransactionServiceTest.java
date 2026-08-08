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

        // Step 2: Updated data
        Transaction updated = new Transaction();
        updated.setAmount(800.0);

        // Step 3: Mock DB
        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(existing);

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

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(transaction);

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

        String result = transactionService.handleAddTransaction(query, "test@gmail.com");

        assertEquals("Transaction added successfully.", result);
        
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

        String result = transactionService.handleAddTransaction(query, "test@gmail.com");

        assertEquals("Transaction added successfully.", result);
        
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getDate().equals(java.time.LocalDate.now().minusDays(1))
        ));
    }
}