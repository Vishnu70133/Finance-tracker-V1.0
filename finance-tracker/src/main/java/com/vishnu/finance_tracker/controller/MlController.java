package com.vishnu.finance_tracker.controller;

import com.vishnu.finance_tracker.model.Transaction;
import com.vishnu.finance_tracker.service.MlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ml")
public class MlController {

    @Autowired
    private MlService mlService;

    @GetMapping("/predict")
public double predict() {

    String email = (String) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    return mlService.predictByEmail(email);
}

@GetMapping("/predict-income")
public double predictIncome() {

    String email = (String) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    return mlService.predictIncomeByEmail(email);
}

@GetMapping("/anomalies")
public List<String> getAnomalies() {

    String email = (String) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    return mlService.detectAnomaliesByEmail(email);
}

@GetMapping("/predict-category")
public Map<String, Double> predictCategory() {

    String email = (String) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    return mlService.predictCategoryByEmail(email);
}

}