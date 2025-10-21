package com.example.storemanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FinanceReport {
    public enum Type { INCOME, EXPENSE }

    private Long id;
    private Long storeId;
    private LocalDate reportDate;
    private Type type;
    private String category;
    private BigDecimal amount;
    private String note;
    private LocalDateTime createdAt;

    public FinanceReport() {}

    public FinanceReport(Long id, Long storeId, LocalDate reportDate, Type type, String category,
                         BigDecimal amount, String note, LocalDateTime createdAt) {
        this.id = id; this.storeId = storeId; this.reportDate = reportDate; this.type = type;
        this.category = category; this.amount = amount; this.note = note; this.createdAt = createdAt;
    }

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}