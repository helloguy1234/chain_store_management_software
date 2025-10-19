package com.example.storemanagement.model; // Gói (package) chứa các lớp mô hình dữ liệu (Model/POJO)

import java.math.BigDecimal;   // Kiểu số thập phân chính xác, dùng cho giá tiền (tránh sai số của double/float)
import java.time.LocalDateTime; // Kiểu ngày-giờ hiện đại của Java (không kèm múi giờ)

public class Product { // Lớp Product biểu diễn một sản phẩm trong hệ thống

    // ===== Thuộc tính (fields) lưu trữ dữ liệu =====
    private Long id;                // ID tự tăng của sản phẩm (PRIMARY KEY)
    private Long supplierId;        // ID nhà cung cấp (có thể null nếu chưa gán)
    private String sku;             // Mã SKU (Stock Keeping Unit) – mã định danh nội bộ
    private String name;            // Tên sản phẩm
    private String unit;            // Đơn vị tính (vd: pcs, box, kg, ...)
    private BigDecimal unitPrice;   // Giá bán đề xuất (dùng BigDecimal để chính xác)
    private BigDecimal unitCost;    // Giá vốn / chi phí nhập (BigDecimal)
    private boolean active;         // Trạng thái còn kinh doanh (true) hay ngừng (false)
    private LocalDateTime createdAt; // Thời điểm tạo bản ghi trong CSDL

    // ===== Constructor mặc định (bắt buộc cho nhiều framework/Jackson/JFX) =====
    public Product() {} // Cho phép tạo đối tượng rỗng rồi set từng thuộc tính

    // ===== Constructor đầy đủ tham số để khởi tạo nhanh một sản phẩm =====
    public Product(Long id, Long supplierId, String sku, String name, String unit,
                   BigDecimal unitPrice, BigDecimal unitCost, boolean active, LocalDateTime createdAt) {
        this.id = id;                                // gán ID
        this.supplierId = supplierId;                // gán nhà cung cấp (có thể null)
        this.sku = sku;                              // gán SKU
        this.name = name;                            // gán tên
        this.unit = unit;                            // gán đơn vị
        this.unitPrice = unitPrice;                  // gán giá bán
        this.unitCost = unitCost;                    // gán giá vốn
        this.active = active;                        // gán trạng thái hoạt động
        this.createdAt = createdAt;                  // gán thời điểm tạo
    }

    // ===== Getter/Setter: truy cập & thay đổi giá trị field một cách an toàn =====
    public Long getId() { return id; }               // Lấy ID
    public void setId(Long id) { this.id = id; }     // Đặt ID

    public Long getSupplierId() { return supplierId; }                  // Lấy ID nhà cung cấp
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; } // Đặt ID nhà cung cấp

    public String getSku() { return sku; }           // Lấy SKU
    public void setSku(String sku) { this.sku = sku; } // Đặt SKU

    public String getName() { return name; }         // Lấy tên
    public void setName(String name) { this.name = name; } // Đặt tên

    public String getUnit() { return unit; }         // Lấy đơn vị tính
    public void setUnit(String unit) { this.unit = unit; } // Đặt đơn vị tính

    public BigDecimal getUnitPrice() { return unitPrice; }                // Lấy giá bán
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; } // Đặt giá bán

    public BigDecimal getUnitCost() { return unitCost; }                  // Lấy giá vốn
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; } // Đặt giá vốn

    public boolean isActive() { return active; }      // Lấy trạng thái còn kinh doanh?
    public void setActive(boolean active) { this.active = active; } // Đặt trạng thái hoạt động

    public LocalDateTime getCreatedAt() { return createdAt; }               // Lấy thời điểm tạo
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // Đặt thời điểm tạo
}
