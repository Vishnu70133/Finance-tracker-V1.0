package com.vishnu.finance_tracker.dto;

import java.time.LocalDate;

public class TransactionResponseDTO {

    private Long id;
    private Double amount;
    private String type;
    private String description;
    private LocalDate date;
    private String userName;
    private String categoryName;
    private Long categoryId;

    public TransactionResponseDTO(Long id, Double amount, String type,
                                  String description, LocalDate date,
                                  String userName, String categoryName,Long categoryId) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.date = date;
        this.userName = userName;
        this.categoryName = categoryName;
        this.categoryId = categoryId;
    }

    public Long getId() { return id; }
    public Double getAmount() { return amount; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public LocalDate getDate() { return date; }
    public String getUserName() { return userName; }
    public String getCategoryName() { return categoryName; }
    public Long getCategoryId() {
    return categoryId;
}
}
