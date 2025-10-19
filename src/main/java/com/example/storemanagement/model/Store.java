// ============================================================================
// File: src/main/java/com/example/storemanagement/model/Store.java
// Mô tả: POJO cho bảng `stores` (xem init.sql)
//  - id (BIGINT), code (UNIQUE), name, address, phone, created_at
//  - Dùng LocalDateTime để map TIMESTAMP
//  (Giữ nguyên code, CHỈ bổ sung chú thích giải thích từng dòng.)
// ============================================================================
package com.example.storemanagement.model; // Package model: nơi chứa các lớp dữ liệu thuần (POJO)

import java.time.LocalDateTime; // Kiểu ngày-giờ hiện đại của Java (không kèm múi giờ)

public class Store { // Lớp Store đại diện một cửa hàng
    private Long id; // PK: Khóa chính của bảng stores
    private String code; // Mã cửa hàng duy nhất (UNIQUE)
    private String name; // Tên hiển thị của cửa hàng
    private String address; // Địa chỉ cửa hàng
    private String phone; // Số điện thoại liên hệ
    private LocalDateTime createdAt; // Thời điểm tạo bản ghi trong DB

    public Store() {
    } // Constructor mặc định (cần cho ORM/Jackson/JavaFX)

    public Store(Long id, String code, String name, String address, String phone, LocalDateTime createdAt) {
        this.id = id; // Gán ID
        this.code = code; // Gán mã cửa hàng
        this.name = name; // Gán tên
        this.address = address; // Gán địa chỉ
        this.phone = phone; // Gán số điện thoại
        this.createdAt = createdAt; // Gán thời điểm tạo
    }

    // Constructor tiện cho tạo mới (chưa có id/createdAt)
    public Store(String code, String name, String address, String phone) {
        this(null, code, name, address, phone, null); // gọi sang constructor đầy đủ với id/createdAt = null
    }

    // ===== Getters/Setters =====
    public Long getId() {
        return id;
    } // Lấy ID

    public void setId(Long id) {
        this.id = id;
    } // Đặt ID

    public String getCode() {
        return code;
    } // Lấy mã cửa hàng

    public void setCode(String code) {
        this.code = code;
    } // Đặt mã cửa hàng

    public String getName() {
        return name;
    } // Lấy tên cửa hàng

    public void setName(String name) {
        this.name = name;
    } // Đặt tên cửa hàng

    public String getAddress() {
        return address;
    } // Lấy địa chỉ

    public void setAddress(String address) {
        this.address = address;
    } // Đặt địa chỉ

    public String getPhone() {
        return phone;
    } // Lấy số điện thoại

    public void setPhone(String phone) {
        this.phone = phone;
    } // Đặt số điện thoại

    public LocalDateTime getCreatedAt() {
        return createdAt;
    } // Lấy thời điểm tạo

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    } // Đặt thời điểm tạo

    @Override
    public String toString() { // Hiển thị đẹp trong ComboBox/Log
        return (name == null ? "Store" : name) + (code != null ? " (" + code + ")" : "");
    }
}