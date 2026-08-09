package com.vishnu.finance_tracker.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.vishnu.finance_tracker.dto.CategorySummaryDTO;
import com.vishnu.finance_tracker.dto.MonthlyChartDTO;
import com.vishnu.finance_tracker.dto.MonthlySummaryDTO;
import com.vishnu.finance_tracker.dto.TransactionResponseDTO;
import com.vishnu.finance_tracker.model.ApiResponse;
import com.vishnu.finance_tracker.model.Transaction;
import com.vishnu.finance_tracker.service.TransactionService;
import com.vishnu.finance_tracker.service.UserService;
import com.vishnu.finance_tracker.model.User;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

@PostMapping
public Transaction createTransaction(@RequestParam Long categoryId,
                                     @RequestBody Transaction transaction) {

    String email =
        (String) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    return transactionService.createTransactionByEmail(email, categoryId, transaction);
}

    private void checkUserIdOwnership(Long userId) {
        String email =
            (String) org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        User user = userService.getUserByEmail(email);
        if (!user.getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
    }

@GetMapping
public java.util.List<Transaction> getAllTransactions() {
    String email =
        (String) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
    User user = userService.getUserByEmail(email);
    return transactionService.getTransactionsByUser(user.getId());
}
@GetMapping("/user/{userId}")
public java.util.List<Transaction> getTransactionsByUser(@PathVariable Long userId) {
    checkUserIdOwnership(userId);
    return transactionService.getTransactionsByUser(userId);
}
@GetMapping("/user/{userId}/page")
public org.springframework.data.domain.Page<TransactionResponseDTO> getPagedTransactions(
        @PathVariable Long userId,
        @RequestParam int page,
        @RequestParam int size) {
    checkUserIdOwnership(userId);
    return transactionService.getTransactionsByUser(userId, page, size);
}
@PutMapping("/{id}")
public Transaction updateTransaction(@PathVariable Long id,
                                     @Valid @RequestBody Transaction transaction) {

    return transactionService.updateTransaction(id, transaction);
}

@DeleteMapping("/{id}")
public String deleteTransaction(@PathVariable Long id) {

    transactionService.deleteTransaction(id);
    return "Transaction soft deleted successfully";
}

@GetMapping("/user/{userId}/range")
public java.util.List<TransactionResponseDTO> getTransactionsByDateRange(
        @PathVariable Long userId,
        @RequestParam String start,
        @RequestParam String end) {
    checkUserIdOwnership(userId);
    return transactionService.getTransactionsByDateRange(
            userId,
            java.time.LocalDate.parse(start),
            java.time.LocalDate.parse(end)
    );
}
@GetMapping("/user/{userId}/summary")
public MonthlySummaryDTO getSummary(@PathVariable Long userId) {
    checkUserIdOwnership(userId);
    return transactionService.getMonthlySummary(userId);
}

@GetMapping("/my/page")
public ApiResponse<org.springframework.data.domain.Page<TransactionResponseDTO>> getMyTransactions(
        @RequestParam int page,
        @RequestParam int size) {

    return new ApiResponse<>(true,
            transactionService.getMyTransactions(page, size));
}

@GetMapping("/my/summary")
public MonthlySummaryDTO getMySummary() {

    return transactionService.getMyMonthlySummary();
}
@GetMapping("/my/range")
public java.util.List<TransactionResponseDTO> getMyTransactionsByRange(
        @RequestParam String start,
        @RequestParam String end) {

    String email =
        (String) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    return transactionService.getTransactionsByEmailAndDateRange(
            email,
            java.time.LocalDate.parse(start),
            java.time.LocalDate.parse(end)
    );
}
@GetMapping("/my/category-summary")
public java.util.List<CategorySummaryDTO> getMyCategorySummary() {
    return transactionService.getMyCategorySummary();
}
@GetMapping("/my/monthly-chart")
public List<MonthlyChartDTO> getMyMonthlyChart(){
    return transactionService.getMyMonthlyChart();
}

}
