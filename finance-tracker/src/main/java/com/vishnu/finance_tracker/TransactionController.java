package com.vishnu.finance_tracker;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
private TransactionService transactionService;

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

@GetMapping
public java.util.List<Transaction> getAllTransactions() {
    return transactionService.getAllTransactions();
}
@GetMapping("/user/{userId}")
public java.util.List<Transaction> getTransactionsByUser(@PathVariable Long userId) {
    return transactionService.getTransactionsByUser(userId);
}
@GetMapping("/user/{userId}/page")
public org.springframework.data.domain.Page<TransactionResponseDTO> getPagedTransactions(
        @PathVariable Long userId,
        @RequestParam int page,
        @RequestParam int size) {

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

    return transactionService.getTransactionsByDateRange(
            userId,
            java.time.LocalDate.parse(start),
            java.time.LocalDate.parse(end)
    );
}
@GetMapping("/user/{userId}/summary")
public MonthlySummaryDTO getSummary(@PathVariable Long userId) {
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
