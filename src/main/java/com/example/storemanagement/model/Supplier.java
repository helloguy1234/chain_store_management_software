// ============================================================================
// File: src/main/java/com/example/storemanagement/model/Supplier.java
// Mô tả: POJO cho bảng `suppliers` (xem init.sql)
//  - id, name, contact, phone, email, address, created_at
// ============================================================================
package com.example.storemanagement.model;

import java.time.LocalDateTime;

public class Supplier {
    private Long id;               // PK
    private String name;           // Tên nhà cung cấp
    private String contact;        // Người liên hệ (tuỳ chọn)
    private String phone;          // SĐT liên hệ
    private String email;          // Email liên hệ
    private String address;        // Địa chỉ NCC
    private LocalDateTime createdAt; // Thời điểm tạo

    public Supplier() {}

    public Supplier(Long id, String name, String contact, String phone, String email,
                    String address, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.contact = contact;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.createdAt = createdAt;
    }

    // Constructor tiện cho tạo mới (chưa có id/createdAt)
    public Supplier(String name, String contact, String phone, String email, String address) {
        this(null, name, contact, phone, email, address, null);
    }

    // ===== Getters/Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name == null ? "Supplier" : name;
    }
}
