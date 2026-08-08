package com.vishnu.finance_tracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vishnu.finance_tracker.dto.CategorySummaryDTO;
import com.vishnu.finance_tracker.dto.FinanceQueryDTO;
import com.vishnu.finance_tracker.dto.MonthlyChartDTO;
import com.vishnu.finance_tracker.dto.MonthlySummaryDTO;
import com.vishnu.finance_tracker.dto.TransactionResponseDTO;
import com.vishnu.finance_tracker.model.Category;
import com.vishnu.finance_tracker.model.PendingAction;
import com.vishnu.finance_tracker.model.Transaction;
import com.vishnu.finance_tracker.model.User;
import com.vishnu.finance_tracker.repository.CategoryRepository;
import com.vishnu.finance_tracker.repository.TransactionRepository;
import com.vishnu.finance_tracker.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private final Map<String, PendingAction> pendingActions = new HashMap<>();

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

public String getFinancialSummaryForAI(String email) {

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    List<Transaction> transactions =
            transactionRepository.findByUserIdAndDeletedFalse(user.getId());

    double income = 0;
    double expense = 0;

    Map<String, Double> categoryTotals = new HashMap<>();

    for (Transaction t : transactions) {

        if ("EXPENSE".equalsIgnoreCase(t.getType())) {

            expense += t.getAmount();

            String category = t.getCategory().getName();

            categoryTotals.put(
                    category,
                    categoryTotals.getOrDefault(category, 0.0) + t.getAmount()
            );

        } else if ("INCOME".equalsIgnoreCase(t.getType())) {

            income += t.getAmount();
        }
    }

    StringBuilder categorySummary = new StringBuilder();

    for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {

        categorySummary.append(entry.getKey())
                .append(": ₹")
                .append(String.format("%.0f", entry.getValue()))
                .append("\n");
    }

    return """
User Financial Summary:

Total Income: ₹%s
Total Expenses: ₹%s

Category Breakdown:
%s

Analyze the spending pattern and provide financial advice.
""".formatted(
            String.format("%.0f", income),
            String.format("%.0f", expense),
            categorySummary.toString()
    );
}

public double getCategoryExpenseLastMonth(String email, String categoryName) {

    User user = userRepository.findByEmail(email);

    LocalDate start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
    LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

    return transactionRepository
            .sumExpenseByCategoryAndDateRange(
                    user.getId(),
                    categoryName,
                    start,
                    end
            );
}

public String getBiggestExpenseLastMonth(String email) {

    User user = userRepository.findByEmail(email);

    LocalDate start = LocalDate.now()
            .minusMonths(1)
            .withDayOfMonth(1);

    LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

    Transaction biggest = transactionRepository
            .findTopByUserIdAndTypeAndDeletedFalseAndDateBetweenOrderByAmountDesc(
                    user.getId(),
                    "EXPENSE",
                    start,
                    end
            );

    if (biggest == null) {
        return "No expenses found last month.";
    }

    return "Your biggest expense last month was ₹"
            + String.format("%.0f", biggest.getAmount())
            + " on "
            + biggest.getCategory().getName();
}

public String getTotalExpenseLastMonth(String email) {

    User user = userRepository.findByEmail(email);

    LocalDate start = LocalDate.now()
            .minusMonths(1)
            .withDayOfMonth(1);

    LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

    double total = transactionRepository.sumExpensesByDateRange(
            user.getId(),
            start,
            end
    );

    return "Your total expenses last month were ₹" +
            String.format("%.0f", total);
}

public String getExpensesSorted(String email, String order) {

    User user = userRepository.findByEmail(email);

    List<Transaction> transactions;

    if ("ASC".equalsIgnoreCase(order)) {

        transactions = transactionRepository
                .findByUserIdAndTypeAndDeletedFalseOrderByAmountAsc(
                        user.getId(),
                        "EXPENSE"
                );

    } else {

        transactions = transactionRepository
                .findByUserIdAndTypeAndDeletedFalseOrderByAmountDesc(
                        user.getId(),
                        "EXPENSE"
                );
    }

    if (transactions.isEmpty()) {
        return "No expenses found.";
    }

    StringBuilder response = new StringBuilder();

    if ("ASC".equalsIgnoreCase(order)) {
        response.append("Your expenses in ascending order:\n\n");
    } else {
        response.append("Your expenses in descending order:\n\n");
    }

    int index = 1;

    for (Transaction t : transactions) {

        response.append(index)
                .append("️. ")
                .append(t.getCategory().getName())
                .append(" — ₹")
                .append(t.getAmount())
                .append("\n");

        index++;
    }

    return response.toString();
}
public String getHighestCategoryLastMonth(String email) {
    return getHighestCategory(email, "last_month", null);
}
public String getLowestCategoryLastMonth(String email) {
    return getLowestCategory(email, "last_month", null);
}

public String getHighestCategory(String email, String timePeriod, String date) {
    User user = userRepository.findByEmail(email);
    LocalDate[] range = resolveDateRange(timePeriod, date);
    LocalDate start = range[0];
    LocalDate end = range[1];

    String category = transactionRepository.getHighestCategoryInDateRange(user.getId(), start, end);

    String label;
    if (date != null) {
        label = "on " + date;
    } else {
        String periodLabel = (timePeriod != null ? timePeriod : "this_month").toLowerCase();
        switch (periodLabel) {
            case "yesterday":
                label = "yesterday";
                break;
            case "day_before_yesterday":
                label = "day before yesterday";
                break;
            case "this_week":
                label = "this week";
                break;
            case "last_week":
                label = "last week";
                break;
            case "this_month":
                label = "this month";
                break;
            case "last_month":
                label = "last month";
                break;
            case "this_year":
                label = "this year";
                break;
            case "last_year":
                label = "last year";
                break;
            default:
                label = "for " + periodLabel.replace("_", " ");
                break;
        }
    }

    if (category == null) {
        return "No expenses found " + label + ".";
    }

    return "Your highest spending category " + label + " was " + category + ".";
}

public String getLowestCategory(String email, String timePeriod, String date) {
    User user = userRepository.findByEmail(email);
    LocalDate[] range = resolveDateRange(timePeriod, date);
    LocalDate start = range[0];
    LocalDate end = range[1];

    String category = transactionRepository.getLowestCategoryInDateRange(user.getId(), start, end);

    String label;
    if (date != null) {
        label = "on " + date;
    } else {
        String periodLabel = (timePeriod != null ? timePeriod : "this_month").toLowerCase();
        switch (periodLabel) {
            case "yesterday":
                label = "yesterday";
                break;
            case "day_before_yesterday":
                label = "day before yesterday";
                break;
            case "this_week":
                label = "this week";
                break;
            case "last_week":
                label = "last week";
                break;
            case "this_month":
                label = "this month";
                break;
            case "last_month":
                label = "last month";
                break;
            case "this_year":
                label = "this year";
                break;
            case "last_year":
                label = "last year";
                break;
            default:
                label = "for " + periodLabel.replace("_", " ");
                break;
        }
    }

    if (category == null) {
        return "No expenses found " + label + ".";
    }

    return "Your lowest spending category " + label + " was " + category + ".";
}
public String filterExpensesByAmount(String email, String comparison, Double amount) {

    User user = userRepository.findByEmail(email);

    List<Transaction> transactions;

    if ("GREATER_THAN".equalsIgnoreCase(comparison)) {

        transactions = transactionRepository
                .findByUserIdAndAmountGreaterThanAndDeletedFalse(user.getId(), amount);

    } else if ("LESS_THAN".equalsIgnoreCase(comparison)) {

        transactions = transactionRepository
                .findByUserIdAndAmountLessThanAndDeletedFalse(user.getId(), amount);

    } else {

        transactions = transactionRepository
                .findByUserIdAndAmountAndDeletedFalse(user.getId(), amount);
    }

    if(transactions.isEmpty()){
        return "No expenses found.";
    }

    StringBuilder response = new StringBuilder("Matching expenses:\n\n");

    int i = 1;

    for(Transaction t : transactions){

        response.append(i++)
                .append(". ")
                .append(t.getCategory().getName())
                .append(" — ₹")
                .append(t.getAmount())
                .append("\n");
    }

    return response.toString();
}

public String getTotalExpense(String email, String timePeriod, String date) {

    User user = userRepository.findByEmail(email);

    LocalDate[] range = resolveDateRange(timePeriod, date);

    LocalDate start = range[0];
    LocalDate end = range[1];

    double total = transactionRepository.sumExpensesByDateRange(
            user.getId(),
            start,
            end
    );

    String label;

    if (date != null) {
        label = "on " + date;
    } else {
        label = "for " + timePeriod.replace("_", " ");
    }

    return "Your total expenses " + label + " were ₹"
            + String.format("%.0f", total);
}

public String getCategoryExpense(String email, String category, String timePeriod, String date) {

    User user = userRepository.findByEmail(email);

    LocalDate[] range = resolveDateRange(timePeriod, date);

    LocalDate start = range[0];
    LocalDate end = range[1];

    double total = transactionRepository.sumCategoryExpensesByDateRange(
            user.getId(),
            category,
            start,
            end
    );

    String label;

    if (date != null) {
        label = "on " + date;
    } else {
        label = "for " + timePeriod.replace("_", " ");
    }

    return "You spent ₹"
            + String.format("%.0f", total)
            + " on "
            + category
            + " "
            + label;
}

public String getTopCategories(String email, Integer limit) {

    if (limit == null || limit <= 0) {
        limit = 3;
    }

    User user = userRepository.findByEmail(email);

    List<Object[]> results =
            transactionRepository.findTopCategories(user.getId());

    if (results.isEmpty()) {
        return "No expense data found.";
    }

    StringBuilder response =
            new StringBuilder("Your top spending categories:\n\n");

    int count = 1;

    for (Object[] row : results) {

        if (count > limit) break;

        String category = (String) row[0];
        Double amount = (Double) row[1];

        response.append(count)
                .append(". ")
                .append(category)
                .append(" — ₹")
                .append(String.format("%.0f", amount))
                .append("\n");

        count++;
    }

    return response.toString();
}

public String getLowestCategories(String email, Integer limit) {

    if (limit == null || limit <= 0) {
        limit = 3;
    }

    User user = userRepository.findByEmail(email);

    List<Object[]> results =
            transactionRepository.findLowestCategories(user.getId());

    if (results.isEmpty()) {
        return "No expense data found.";
    }

    StringBuilder response =
            new StringBuilder("Your lowest spending categories:\n\n");

    int count = 1;

    for (Object[] row : results) {

        if (count > limit) break;

        String category = (String) row[0];
        Double amount = (Double) row[1];

        response.append(count)
                .append(". ")
                .append(category)
                .append(" — ₹")
                .append(String.format("%.0f", amount))
                .append("\n");

        count++;
    }

    return response.toString();
}

public String compareSpendingWithLastMonth(String email) {

    User user = userRepository.findByEmail(email);

    LocalDate currentStart = LocalDate.now().withDayOfMonth(1);
    LocalDate currentEnd = LocalDate.now();

    LocalDate lastStart = LocalDate.now()
            .minusMonths(1)
            .withDayOfMonth(1);

    LocalDate lastEnd = lastStart.withDayOfMonth(lastStart.lengthOfMonth());

    double currentMonth = transactionRepository.sumExpensesByDateRange(
            user.getId(),
            currentStart,
            currentEnd
    );

    double lastMonth = transactionRepository.sumExpensesByDateRange(
            user.getId(),
            lastStart,
            lastEnd
    );

    if (currentMonth > lastMonth) {

        double diff = currentMonth - lastMonth;

        return "Your spending increased by ₹"
                + String.format("%.0f", diff)
                + " compared to last month.";
    }

    else if (currentMonth < lastMonth) {

        double diff = lastMonth - currentMonth;

        return "Good job! Your spending decreased by ₹"
                + String.format("%.0f", diff)
                + " compared to last month.";
    }

    else {

        return "Your spending is the same as last month (₹"
                + String.format("%.0f", currentMonth) + ").";
    }
}

public String handleAddTransaction(FinanceQueryDTO query, String email) {

    // Validate amount
    if (query.getAmount() == null) {
        return "Please provide the amount for the transaction.";
    }

    // Validate category
    if (query.getCategory() == null) {
        return "Please provide the category for the transaction.";
    }

    // Default type if missing (most natural sentences imply expense)
    if (query.getType() == null) {
        query.setType("EXPENSE");
    }

    // Handle missing description using pending conversation
    if (query.getDescription() == null) {

        pendingActions.put(email,
                new PendingAction("ADD_TRANSACTION", query));

        return "Please provide a description for this transaction.";
    }

    // Find logged-in user
    User user = userRepository.findByEmail(email);

    if (user == null) {
        return "User not found.";
    }

    // Find category
    Category category = categoryRepository
            .findByNameIgnoreCase(query.getCategory())
            .orElse(null);

    // Category suggestion logic
    if (category == null) {

        String originalCategory = query.getCategory();

        Category suggestion = findClosestCategory(originalCategory);

        if (suggestion != null) {

            pendingActions.put(email,
                    new PendingAction("ADD_TRANSACTION", query));

            return "Category '" + originalCategory +
                    "' not found. Did you mean '" +
                    suggestion.getName() + "'?";
        }

        return "Category '" + query.getCategory()
                + "' not found. Please provide a valid category.";
    }

    // Date handling
    LocalDate date;

    try {
        date = resolveDate(query.getDate());
        if (date == null) {
            date = LocalDate.now();
        }
    } catch (Exception e) {
        return "Invalid date format. Please provide a valid date.";
    }

    // Create transaction
    Transaction transaction = new Transaction();

    transaction.setUser(user);
    transaction.setCategory(category);
    transaction.setAmount(query.getAmount());
    transaction.setType(query.getType().toUpperCase());
    transaction.setDescription(query.getDescription().trim());
    transaction.setDate(date);

    // Save transaction
    transactionRepository.save(transaction);

    return "Transaction added successfully.";
}

private LocalDate resolveDate(String dateStr) {
    if (dateStr == null) {
        return null;
    }
    String normalized = dateStr.trim().toLowerCase();
    switch (normalized) {
        case "today":
            return LocalDate.now();
        case "yesterday":
            return LocalDate.now().minusDays(1);
        case "tomorrow":
            return LocalDate.now().plusDays(1);
        case "day_before_yesterday":
            return LocalDate.now().minusDays(2);
        default:
            return LocalDate.parse(dateStr);
    }
}
public Category findClosestCategory(String input) {

    input = input.toLowerCase();

    // 1️⃣ Check synonyms
    if (CATEGORY_SYNONYMS.containsKey(input)) {

        String mappedCategory = CATEGORY_SYNONYMS.get(input);

        return categoryRepository
                .findByNameIgnoreCase(mappedCategory)
                .orElse(null);
    }

    // 2️⃣ Check partial match
    List<Category> categories = categoryRepository.findAll();

    for (Category c : categories) {

        if (c.getName().toLowerCase().contains(input)
                || input.contains(c.getName().toLowerCase())) {

            return c;
        }
    }

    return null;
}

public String handleUpdateTransaction(FinanceQueryDTO query, String email) {

    if (query.getCategory() == null) {
        return "Please specify the category of the transaction you want to update.";
    }

    if (query.getAmount() == null) {
        return "Please specify the new amount.";
    }

    User user = userRepository.findByEmail(email);

    Category category = categoryRepository
            .findByNameIgnoreCase(query.getCategory())
            .orElse(null);

    if (category == null) {
        return "Category '" + query.getCategory() + "' not found.";
    }

    LocalDate date;

    try {
        date = resolveDate(query.getDate());
        if (date == null) {
            date = LocalDate.now();
        }
    } catch (Exception e) {
        return "Invalid date format. Please provide a valid date.";
    }

    List<Transaction> transactions =
            transactionRepository.findByUserIdAndDateAndCategoryIdAndDeletedFalse(
                    user.getId(),
                    date,
                    category.getId()
            );

    if (transactions.isEmpty()) {

    // store pending action
    pendingActions.put(email,
            new PendingAction("ADD_TRANSACTION", query));

    return "No such transaction found. Do you want me to create it instead?";
}

    Transaction transaction = transactions.get(0);

    transaction.setAmount(query.getAmount());

    if (query.getDescription() != null) {
        transaction.setDescription(query.getDescription());
    }

    transactionRepository.save(transaction);

    return "Transaction updated successfully.";
}

public String handleDeleteTransaction(FinanceQueryDTO query, String email) {

    if (query.getCategory() == null) {
        return "Please specify the category of the transaction you want to delete.";
    }

    User user = userRepository.findByEmail(email);

    Category category = categoryRepository
            .findByNameIgnoreCase(query.getCategory())
            .orElse(null);

    if (category == null) {
        return "Category '" + query.getCategory() + "' not found.";
    }

    LocalDate date;

    try {
        date = resolveDate(query.getDate());
        if (date == null) {
            date = LocalDate.now();
        }
    } catch (Exception e) {
        return "Invalid date format. Please provide a valid date.";
    }

    List<Transaction> transactions =
            transactionRepository.findByUserIdAndDateAndCategoryIdAndDeletedFalse(
                    user.getId(),
                    date,
                    category.getId()
            );

    if (transactions.isEmpty()) {
        return "No such transaction found.";
    }

    Transaction transaction = transactions.get(0);

    transaction.setDeleted(true);

    transactionRepository.save(transaction);

    return "Transaction deleted successfully.";
}
private static final Map<String, String> CATEGORY_SYNONYMS = Map.of(
        "movie", "Entertainment",
        "cinema", "Entertainment",
        "burger", "Food",
        "restaurant", "Food",
        "hospital", "Health",
        "doctor", "Health",
        "uber", "Transport",
        "taxi", "Transport"
);


public PendingAction getPendingAction(String email) {
    return pendingActions.get(email);
}

public Object executePendingAction(String email){

    PendingAction pending = pendingActions.get(email);

    if(pending == null){
        return "No pending action.";
    }

    FinanceQueryDTO query = pending.getQuery();

    pendingActions.remove(email);

    switch (pending.getAction()) {

        case "ADD_TRANSACTION":

            if(query.getDescription() == null){
                query.setDescription(query.getCategory() + " expense");
            }

            if(query.getType() == null){
                query.setType("EXPENSE");
            }

            return handleAddTransaction(query,email);

        default:
            return "Unknown pending action.";
    }
}

public String detectCategoryFromText(String question) {

    question = question.toLowerCase();

    // Smart keyword mapping
    Map<String,String> keywordMap = Map.ofEntries(

        Map.entry("pizza","Food"),
        Map.entry("burger","Food"),
        Map.entry("coffee","Food"),
        Map.entry("lunch","Food"),
        Map.entry("dinner","Food"),

        Map.entry("movie","Entertainment"),
        Map.entry("netflix","Entertainment"),
        Map.entry("game","Entertainment"),

        Map.entry("uber","Transportation"),
        Map.entry("bus","Transportation"),
        Map.entry("taxi","Transportation"),

        Map.entry("hospital","Health"),
        Map.entry("medicine","Health"),

        Map.entry("salary","Salary"),
        Map.entry("bonus","Salary"),
        Map.entry("freelance","Income"),
        Map.entry("interest","Income")
);

    for(String keyword : keywordMap.keySet()){

        if(question.contains(keyword)){
            return keywordMap.get(keyword);
        }
    }

    // fallback to DB categories
    List<Category> categories = categoryRepository.findAll();

    for (Category category : categories) {

        String name = category.getName().toLowerCase();

        if (question.contains(name)) {
            return category.getName();
        }
    }

    return null;
}

public String detectDescriptionFromText(String question) {

    question = question.toLowerCase();

    // Remove numbers
    question = question.replaceAll("\\d+", "");

    // Remove common finance words
    question = question
            .replace("add", "")
            .replace("expense", "")
            .replace("income", "")
            .replace("spent", "")
            .replace("today", "")
            .replace("yesterday", "")
            .replace("on", "")
            .trim();

    if (question.isEmpty()) {
        return null;
    }

    return question;
}

public boolean categoryExists(String categoryName){

    if(categoryName == null){
        return false;
    }

    return categoryRepository
            .findByNameIgnoreCase(categoryName)
            .isPresent();
}

private LocalDate[] resolveDateRange(String timePeriod, String date) {

    LocalDate start;
    LocalDate end;

    /* -----------------------------------
       EXACT DATE SUPPORT
    ------------------------------------ */

    if (date != null) {
        start = resolveDate(date);
        end = start;
        return new LocalDate[]{start, end};
    }

    if (timePeriod == null) {
        timePeriod = "this_month";
    }

    String lower = timePeriod.toLowerCase();

    /* -----------------------------------
       MONTH NAME SUPPORT
    ------------------------------------ */

    Map<String, Integer> months = Map.ofEntries(
        Map.entry("january", 1),
        Map.entry("february", 2),
        Map.entry("march", 3),
        Map.entry("april", 4),
        Map.entry("may", 5),
        Map.entry("june", 6),
        Map.entry("july", 7),
        Map.entry("august", 8),
        Map.entry("september", 9),
        Map.entry("october", 10),
        Map.entry("november", 11),
        Map.entry("december", 12)
    );

    for (String month : months.keySet()) {

        if (lower.contains(month)) {

            int monthValue = months.get(month);

            int year = LocalDate.now().getYear();

            /* detect year like "march 2025" */

            Pattern yearPattern = Pattern.compile("(\\d{4})");
            Matcher matcher = yearPattern.matcher(lower);

            if (matcher.find()) {
                year = Integer.parseInt(matcher.group(1));
            }

            start = LocalDate.of(year, monthValue, 1);
            end = start.withDayOfMonth(start.lengthOfMonth());

            return new LocalDate[]{start, end};
        }
    }

    /* -----------------------------------
       EXISTING TIME PERIOD LOGIC
    ------------------------------------ */

    switch (timePeriod) {

        case "yesterday":
            start = LocalDate.now().minusDays(1);
            end = start;
            break;

        case "day_before_yesterday":
            start = LocalDate.now().minusDays(2);
            end = start;
            break;

        case "this_week":
            start = LocalDate.now().with(DayOfWeek.MONDAY);
            end = LocalDate.now();
            break;

        case "last_week":
            start = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
            end = start.plusDays(6);
            break;

        case "this_month":
            start = LocalDate.now().withDayOfMonth(1);
            end = LocalDate.now();
            break;

        case "last_month":
            start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            end = start.withDayOfMonth(start.lengthOfMonth());
            break;

        case "this_year":
            start = LocalDate.now().withDayOfYear(1);
            end = LocalDate.now();
            break;

        case "last_year":
            start = LocalDate.now().minusYears(1).withDayOfYear(1);
            end = start.withDayOfYear(start.lengthOfYear());
            break;

        default:
            start = LocalDate.now().withDayOfMonth(1);
            end = LocalDate.now();
    }

    return new LocalDate[]{start, end};
}
}
