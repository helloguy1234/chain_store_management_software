package com.example.storemanagement.service; // Package service: chứa lớp xử lý nghiệp vụ

/*
 * InventoryService – Tầng nghiệp vụ cho Kho hàng / Tài nguyên
 * 🎯 Vai trò:
 *   - Quản lý CRUD sản phẩm (gọi xuống ProductDAO).
 *   - Nhập/Xuất kho thông qua bảng stock_moves (qua ProductDAO), trigger DB sẽ cập nhật bảng tồn.
 *   - Lấy dữ liệu tổng quan tồn kho từ view v_inventory_overview.
 * ⚠️ File này chỉ thêm chú thích giải thích; không đổi logic.
 */

import java.math.BigDecimal; // Dùng BigDecimal cho giá để chính xác số học
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException; // Ném ngược ra cho UI/Controller hiển thị lỗi thân thiện
import java.util.List; // Danh sách
import java.util.Optional; // Kết quả có/không có

import com.example.storemanagement.dao.DBConnection;
import com.example.storemanagement.dao.ProductDAO; // DAO tương tác DB (products, stock_moves, view)
import com.example.storemanagement.model.Product; // POJO Product

public class InventoryService { // Lớp service trung gian giữa Controller và DAO

    private final ProductDAO productDAO = new ProductDAO(); // Khởi tạo DAO; sau này có thể thay bằng DI

    // ===================== Sản phẩm =====================

    public long addProduct(Product p) throws SQLException {
        validateProduct(p, true);
        long id = productDAO.insert(p);

        // 🔹 Tự động tạo inventory = 0 cho sản phẩm này ở mọi cửa hàng
        try (Connection cn = DBConnection.getInstance().getConnection();
                PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO inventory(store_id, product_id, quantity) " +
                                "SELECT id, ?, 0 FROM stores")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }

        return id;
    }

    public boolean updateProduct(Product p) throws SQLException { // Cập nhật sản phẩm
        if (p.getId() == null)
            throw new IllegalArgumentException("Thiếu ID sản phẩm"); // Update phải có ID
        validateProduct(p, false); // Kiểm tra dữ liệu (không bắt buộc SKU trống nếu không đổi)
        return productDAO.update(p); // Gọi DAO UPDATE
    }

    public boolean deleteProduct(long id) throws SQLException { // Xóa sản phẩm theo ID
        return productDAO.delete(id);
    }

    public Optional<Product> findProductById(long id) throws SQLException { // Tìm sản phẩm theo ID
        return productDAO.findById(id);
    }

    public List<Product> searchProducts(String keyword, int page, int pageSize) throws SQLException { // Tìm kiếm có
                                                                                                      // phân trang
        return productDAO.search(keyword, page, pageSize);
    }

    // ===================== Kho hàng =====================

    public void importStock(long storeId, long productId, int quantity, String note) throws SQLException { // Nhập kho
        if (storeId <= 0 || productId <= 0)
            throw new IllegalArgumentException("storeId/productId không hợp lệ"); // Kiểm tra id hợp lệ
        if (quantity <= 0)
            throw new IllegalArgumentException("Số lượng phải > 0"); // Kiểm tra số lượng dương
        productDAO.importStock(storeId, productId, quantity, note); // Ghi stock_moves với move_type=IMPORT
    }

    public void exportStock(long storeId, long productId, int quantity, String note) throws SQLException { // Xuất kho
        if (storeId <= 0 || productId <= 0)
            throw new IllegalArgumentException("storeId/productId không hợp lệ"); // Kiểm tra id hợp lệ
        if (quantity <= 0)
            throw new IllegalArgumentException("Số lượng phải > 0"); // Kiểm tra số lượng dương
        productDAO.exportStock(storeId, productId, quantity, note); // Ghi stock_moves với move_type=EXPORT; trigger DB
                                                                    // sẽ trừ tồn
    }

    public List<ProductDAO.InventoryOverview> getInventoryOverview(Long storeId, Long supplierId, String keyword,
            int page, int pageSize) throws SQLException {
        return productDAO.getInventoryOverview(storeId, supplierId, keyword, page, pageSize);
    }

    // ===================== VALIDATION =====================

    private void validateProduct(Product p, boolean creating) { // Kiểm tra dữ liệu sản phẩm trước khi ghi DB
        if (p == null)
            throw new IllegalArgumentException("Thiếu dữ liệu sản phẩm"); // Không có dữ liệu
        if (creating && (p.getSku() == null || p.getSku().trim().isEmpty())) // Khi tạo mới bắt buộc có SKU
            throw new IllegalArgumentException("SKU không được để trống");
        if (p.getName() == null || p.getName().trim().isEmpty()) // Tên bắt buộc
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        if (p.getUnitPrice() != null && p.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) // Giá bán >= 0
            throw new IllegalArgumentException("Giá bán không hợp lệ");
        if (p.getUnitCost() != null && p.getUnitCost().compareTo(BigDecimal.ZERO) < 0) // Giá vốn >= 0
            throw new IllegalArgumentException("Giá vốn không hợp lệ");
    }
}