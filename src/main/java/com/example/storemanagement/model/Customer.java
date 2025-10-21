package com.example.storemanagement.model;

/*
 * Customer.java – Lớp mô hình (Model) biểu diễn một khách hàng trong hệ thống.
 * ✅ Mục đích:
 *   - Lưu trữ thông tin khách hàng (id, tên, số điện thoại, email, điểm thưởng, ngày tạo).
 *   - Dùng trong tầng Model của kiến trúc MVC.
 *   - Được hiển thị trên giao diện TableView trong customers.fxml.
 */

import java.time.LocalDateTime; // kiểu dữ liệu lưu thời gian chính xác đến giây

public class Customer {

    // ====== THUỘC TÍNH ======
    private Long id;              // ID khách hàng (khóa chính trong database)
    private String name;          // Tên khách hàng
    private String phone;         // Số điện thoại
    private String email;         // Địa chỉ email
    private Integer points;       // Điểm tích lũy (dành cho chương trình khách hàng thân thiết)
    private LocalDateTime createdAt; // Ngày và giờ tạo bản ghi

    // ====== CONSTRUCTOR MẶC ĐỊNH ======
    public Customer() {} // cần thiết để JavaFX và JDBC có thể khởi tạo đối tượng rỗng

    // ====== CONSTRUCTOR ĐẦY ĐỦ ======
    public Customer(Long id, String name, String phone, String email, Integer points, LocalDateTime createdAt) {
        // Khởi tạo đầy đủ các thuộc tính khi tạo mới Customer
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.points = points;
        this.createdAt = createdAt;
    }

    // ====== GETTERS & SETTERS ======
    // Các phương thức truy cập cho phép JavaFX Binding và DAO sử dụng linh hoạt.

    public Long getId() { return id; }                       // lấy ID khách hàng
    public void setId(Long id) { this.id = id; }             // đặt ID khách hàng

    public String getName() { return name; }                 // lấy tên
    public void setName(String name) { this.name = name; }   // đặt tên

    public String getPhone() { return phone; }               // lấy số điện thoại
    public void setPhone(String phone) { this.phone = phone; } // đặt số điện thoại

    public String getEmail() { return email; }               // lấy email
    public void setEmail(String email) { this.email = email; } // đặt email

    public Integer getPoints() { return points; }             // lấy điểm tích lũy
    public void setPoints(Integer points) { this.points = points; } // đặt điểm tích lũy

    public LocalDateTime getCreatedAt() { return createdAt; } // lấy ngày tạo
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // đặt ngày tạo
}
