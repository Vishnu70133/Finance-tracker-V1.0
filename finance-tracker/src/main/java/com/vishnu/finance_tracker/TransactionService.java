package com.vishnu.finance_tracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public Transaction createTransactionByEmail(String email,
                                            Long categoryId,
                                            Transaction transaction) {

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));

    transaction.setUser(user);
    transaction.setCategory(category);

    return transactionRepository.save(transaction);
}

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
    public List<Transaction> getTransactionsByUser(Long userId) {
    return transactionRepository.findByUserIdAndDeletedFalse(userId);
}
public Page<TransactionResponseDTO> getTransactionsByUser(Long userId, int page, int size) {

    Page<Transaction> transactions =
            transactionRepository.findByUserIdAndDeletedFalse(userId, PageRequest.of(page, size));

    return transactions.map(this::mapToDTO);
}
private TransactionResponseDTO mapToDTO(Transaction t) {
    return new TransactionResponseDTO(
            t.getId(),
            t.getAmount(),
            t.getType(),
            t.getDescription(),
            t.getDate(),
            t.getUser().getName(),
            t.getCategory().getName(),
            t.getCategory().getId() 
    );
}
public Transaction updateTransaction(Long transactionId, Transaction updatedTransaction) {

    Transaction existing = transactionRepository.findById(transactionId)
            .orElseThrow();

    existing.setAmount(updatedTransaction.getAmount());
    existing.setType(updatedTransaction.getType());
    existing.setDescription(updatedTransaction.getDescription());
    existing.setDate(updatedTransaction.getDate());

    return transactionRepository.save(existing);
}
public void deleteTransaction(Long id) {

    Transaction transaction = transactionRepository.findById(id)
            .orElseThrow();

    transaction.setDeleted(true);
    transactionRepository.save(transaction);
}
public List<TransactionResponseDTO> getTransactionsByDateRange(
        Long userId,
        LocalDate start,
        LocalDate end) {

    List<Transaction> transactions =
            transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(
                    userId, start, end);

    return transactions.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
}
public MonthlySummaryDTO getMonthlySummary(Long userId) {

    List<Transaction> transactions =
            transactionRepository.findByUserIdAndDeletedFalse(userId);

    double income = 0;
    double expense = 0;

    for (Transaction t : transactions) {

        if ("INCOME".equalsIgnoreCase(t.getType())) {
            income += t.getAmount();
        } else if ("EXPENSE".equalsIgnoreCase(t.getType())) {
            expense += t.getAmount();
        }
    }

    return new MonthlySummaryDTO(income, expense);
}

public String getLoggedInEmail() {

    return (String) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
}
public org.springframework.data.domain.Page<TransactionResponseDTO> getMyTransactions(int page, int size) {

    String email = getLoggedInEmail();

    User user = userRepository.findByEmail(email);

    org.springframework.data.domain.Page<Transaction> transactions =
            transactionRepository.findByUserIdAndDeletedFalse(
                    user.getId(),
                    org.springframework.data.domain.PageRequest.of(page, size)
            );

    return transactions.map(this::mapToDTO);
}

public MonthlySummaryDTO getMyMonthlySummary() {

    String email = getLoggedInEmail();

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    List<Transaction> transactions =
            transactionRepository.findByUserIdAndDeletedFalse(user.getId());

    double income = 0;
    double expense = 0;

    for (Transaction t : transactions) {

        if ("INCOME".equalsIgnoreCase(t.getType())) {
            income += t.getAmount();
        } else if ("EXPENSE".equalsIgnoreCase(t.getType())) {
            expense += t.getAmount();
        }
    }

    return new MonthlySummaryDTO(income, expense);
}

public List<TransactionResponseDTO> getTransactionsByEmailAndDateRange(
        String email,
        LocalDate start,
        LocalDate end) {

    User user = userRepository.findByEmail(email);

    List<Transaction> transactions =
            transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(
                    user.getId(), start, end);

    return transactions.stream()
            .map(this::mapToDTO)
            .toList();
}
public List<CategorySummaryDTO> getMyCategorySummary() {

    String email = getLoggedInEmail();

    User user = userRepository.findByEmail(email);

    return transactionRepository.getCategorySummary(user.getId());
}
public List<MonthlyChartDTO> getMyMonthlyChart() {

    String email = getLoggedInEmail();
    User user = userRepository.findByEmail(email);

    List<Transaction> list =
        transactionRepository.findByUserIdAndDeletedFalse(user.getId());

    Map<String, Double> incomeMap = new HashMap<>();
    Map<String, Double> expenseMap = new HashMap<>();

    for (Transaction t : list) {

        String month = t.getDate().getMonth().toString().substring(0,3)
                       + " " + t.getDate().getYear();

        if ("INCOME".equalsIgnoreCase(t.getType())) {
            incomeMap.put(month,
                incomeMap.getOrDefault(month,0.0) + t.getAmount());
        } else {
            expenseMap.put(month,
                expenseMap.getOrDefault(month,0.0) + t.getAmount());
        }
    }

    Set<String> months = new TreeSet<>();
    months.addAll(incomeMap.keySet());
    months.addAll(expenseMap.keySet());

    List<MonthlyChartDTO> result = new ArrayList<>();

    for(String m : months){
        result.add(new MonthlyChartDTO(
            m,
            incomeMap.getOrDefault(m,0.0),
            expenseMap.getOrDefault(m,0.0)
        ));
    }

    return result;
}

}
