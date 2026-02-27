package com.vishnu.finance_tracker;

public class CategorySummaryDTO {

    private String category;
    private Double total;

    public CategorySummaryDTO(String category, Double total) {
        this.category = category;
        this.total = total;
    }

    public String getCategory() { return category; }
    public Double getTotal() { return total; }
}