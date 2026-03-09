package com.vishnu.finance_tracker.model;
import jakarta.validation.constraints.*;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   @Positive(message = "Amount must be positive")
private Double amount;

@NotBlank(message = "Type is required")
private String type;

@NotBlank(message = "Description cannot be empty")
private String description;

@NotNull(message = "Date is required")
private LocalDate date;

    private boolean deleted = false;


    // Many transactions belong to one user
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Many transactions belong to one category
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public Transaction() {}

    public Transaction(Long id, Double amount, String type,
                       String description, LocalDate date,
                       User user, Category category) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.date = date;
        this.user = user;
        this.category = category;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public boolean isDeleted() {
    return deleted;
}

public void setDeleted(boolean deleted) {
    this.deleted = deleted;
}


    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                ", date=" + date +
                '}';
    }
}
