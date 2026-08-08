package com.vishnu.finance_tracker.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.vishnu.finance_tracker.model.Transaction;
import com.vishnu.finance_tracker.model.User;
import com.vishnu.finance_tracker.repository.TransactionRepository;
import com.vishnu.finance_tracker.repository.UserRepository;

@Service
public class MlService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
private UserRepository userRepository;

    public double predict(Long userId) {

    String url = "http://localhost:8000/predict";

    try {
        // Step 1: Fetch transactions
        List<Transaction> transactions =
                transactionRepository.findByUserIdAndDeletedFalse(userId);

        // Step 2: Convert to monthly totals
        List<Double> values = convertToMonthlyTotals(transactions);

        System.out.println("Monthly values: " + values);

        // Step 3: Call ML API
        Map<String, Object> request = new HashMap<>();
        request.put("values", values);

        Map response = restTemplate.postForObject(url, request, Map.class);

        System.out.println("Response: " + response);

        return Double.parseDouble(response.get("prediction").toString());

    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("ML service failed");
    }
}

public Map<String, Double> predictCategoryWise(Long userId) {

    String url = "http://localhost:8000/predict";

    try {
        // Step 1: Fetch transactions
        List<Transaction> transactions =
                transactionRepository.findByUserIdAndDeletedFalse(userId);

        // Step 2: Convert to category-wise monthly totals
        Map<String, List<Double>> categoryData =
                convertToCategoryMonthlyTotals(transactions);

        Map<String, Double> predictions = new HashMap<>();

        // Step 3: Run ML for each category
        for (String category : categoryData.keySet()) {

            List<Double> values = categoryData.get(category);

            // Skip if not enough data
            if (values.size() < 2) {
                double avg = values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

                predictions.put(category, avg);
                continue;
            }

            Map<String, Object> request = new HashMap<>();
            request.put("values", values);

            Map response = restTemplate.postForObject(url, request, Map.class);

            double prediction = Double.parseDouble(response.get("prediction").toString());

            prediction = Math.max(0, prediction);
predictions.put(category, prediction);
        }

        return predictions;

    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Category ML failed");
    }
}

public double predictIncome(Long userId) {

    String url = "http://localhost:8000/predict";

    try {
        // Step 1: Fetch transactions
        List<Transaction> transactions =
                transactionRepository.findByUserIdAndDeletedFalse(userId);

        // Step 2: Convert to monthly income totals
        List<Double> values = convertToMonthlyIncome(transactions);

        System.out.println("Monthly income values: " + values);

        // Step 3: Call ML API
        Map<String, Object> request = new HashMap<>();
        request.put("values", values);

        Map response = restTemplate.postForObject(url, request, Map.class);

        System.out.println("Income Prediction Response: " + response);

        return Double.parseDouble(response.get("prediction").toString());

    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Income ML failed");
    }
}

private List<Double> convertToMonthlyTotals(List<Transaction> transactions) {

    Map<String, Double> monthlyTotals = new TreeMap<>();

    for (Transaction t : transactions) {

        // Only consider expenses
        if (!t.getType().equalsIgnoreCase("EXPENSE")) continue;

        String monthKey = t.getDate().getYear() + "-" + t.getDate().getMonthValue();

        monthlyTotals.put(
                monthKey,
                monthlyTotals.getOrDefault(monthKey, 0.0) + t.getAmount()
        );
    }

    return new ArrayList<>(monthlyTotals.values());
}
    

private List<Double> convertToMonthlyIncome(List<Transaction> transactions) {

    Map<String, Double> monthlyTotals = new TreeMap<>();

    for (Transaction t : transactions) {

        // Only INCOME
        if (!t.getType().equalsIgnoreCase("INCOME")) continue;

        String monthKey = t.getDate().getYear() + "-" + t.getDate().getMonthValue();

        monthlyTotals.put(
                monthKey,
                monthlyTotals.getOrDefault(monthKey, 0.0) + t.getAmount()
        );
    }

    return new ArrayList<>(monthlyTotals.values());
}

public List<String> detectAnomalies(Long userId) {

    List<Transaction> transactions =
            transactionRepository.findByUserIdAndDeletedFalse(userId);

    List<Double> amounts = transactions.stream()
            .filter(t -> t.getType().equalsIgnoreCase("EXPENSE"))
            .map(Transaction::getAmount)
            .toList();

    double mean = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0);

    double variance = amounts.stream()
            .mapToDouble(a -> Math.pow(a - mean, 2))
            .average()
            .orElse(0);

    double stdDev = Math.sqrt(variance);

    List<String> anomalies = new ArrayList<>();

    for (Transaction t : transactions) {

        if (!t.getType().equalsIgnoreCase("EXPENSE")) continue;

        double z = (t.getAmount() - mean) / stdDev;

        if (Math.abs(z) > 2) {
            anomalies.add(
                "Unusual spending: ₹" + t.getAmount() +
                " on " + t.getCategory().getName()
            );
        }
    }

    return anomalies;
}

private Map<String, List<Double>> convertToCategoryMonthlyTotals(List<Transaction> transactions) {

    Map<String, Map<String, Double>> categoryMonthMap = new HashMap<>();

    for (Transaction t : transactions) {

        if (!t.getType().equalsIgnoreCase("EXPENSE")) continue;

        String category = t.getCategory().getName();
        String monthKey = t.getDate().getYear() + "-" + t.getDate().getMonthValue();

        categoryMonthMap.putIfAbsent(category, new TreeMap<>());

        Map<String, Double> monthMap = categoryMonthMap.get(category);

        monthMap.put(
                monthKey,
                monthMap.getOrDefault(monthKey, 0.0) + t.getAmount()
        );
    }

    // Convert to required format
    Map<String, List<Double>> result = new HashMap<>();

    for (String category : categoryMonthMap.keySet()) {
        result.put(category, new ArrayList<>(categoryMonthMap.get(category).values()));
    }

    return result;
}

public double predictByEmail(String email) {

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    return predict(user.getId());
}

public double predictIncomeByEmail(String email) {

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    return predictIncome(user.getId());
}

public List<String> detectAnomaliesByEmail(String email) {

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    return detectAnomalies(user.getId());
}

public Map<String, Double> predictCategoryByEmail(String email) {

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    return predictCategoryWise(user.getId());
}

}
