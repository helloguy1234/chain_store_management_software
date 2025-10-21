package com.example.storemanagement.dao;

/*
 * CustomerDAO – Lớp Data Access Object (DAO) quản lý truy vấn bảng "customers".
 * ✅ Mục đích:
 *   - Thực hiện các thao tác CRUD (Create, Read, Update, Delete) trên bảng customers.
 *   - Cung cấp phương thức tìm kiếm, phân trang, và đếm số lượng khách hàng.
 *   - Chuyển đổi giữa dữ liệu trong MySQL (ResultSet) và đối tượng Customer trong Java.
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.storemanagement.model.Customer;

public class CustomerDAO {

    /**
     * Thêm mới một khách hàng vào cơ sở dữ liệu.
     * @param c Đối tượng Customer chứa dữ liệu cần thêm
     * @return ID tự tăng (nếu thành công), hoặc -1 nếu thất bại
     */
    public long insert(Customer c) throws SQLException {
        // Câu lệnh SQL: thêm tên, sđt, email, điểm
        String sql = "INSERT INTO customers(name, phone, email, points) VALUES(?,?,?,?)";
        // Tạo kết nối + PreparedStatement trong try-with-resources (tự đóng sau khi xong)
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Gán giá trị vào các tham số (?)
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setInt(4, c.getPoints() == null ? 0 : c.getPoints());
            ps.executeUpdate(); // thực thi lệnh INSERT

            // Lấy ID tự tăng (nếu có)
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L; // nếu không có ID được trả về
    }

    /**
     * Cập nhật thông tin khách hàng.
     * @param c đối tượng Customer đã có id
     * @return true nếu update thành công
     */
    public boolean update(Customer c) throws SQLException {
        String sql = "UPDATE customers SET name=?, phone=?, email=?, points=? WHERE id=?";
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setInt(4, c.getPoints() == null ? 0 : c.getPoints());
            ps.setLong(5, c.getId());
            return ps.executeUpdate() > 0; // trả về true nếu có ít nhất 1 dòng được cập nhật
        }
    }

    /**
     * Xóa khách hàng theo id.
     * @param id khóa chính của khách hàng
     * @return true nếu xóa thành công
     */
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM customers WHERE id=?";
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Tìm khách hàng theo id.
     * @param id khóa chính
     * @return Optional<Customer> (có thể rỗng nếu không tìm thấy)
     */
    public Optional<Customer> findById(long id) throws SQLException {
        String sql = "SELECT id, name, phone, email, points, created_at FROM customers WHERE id=?";
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs)); // ánh xạ bản ghi thành đối tượng Customer
                return Optional.empty();
            }
        }
    }

    /**
     * Tìm kiếm khách hàng theo từ khóa (name / phone / email) + hỗ trợ phân trang.
     * @param keyword từ khóa tìm kiếm
     * @param page trang hiện tại (bắt đầu từ 1)
     * @param pageSize số bản ghi mỗi trang
     * @return danh sách khách hàng phù hợp
     */
    public List<Customer> search(String keyword, int page, int pageSize) throws SQLException {
        // Đảm bảo giá trị hợp lệ
        if (page < 1) page = 1;
        if (pageSize <= 0) pageSize = 20;
        int offset = (page - 1) * pageSize; // vị trí bắt đầu lấy dữ liệu

        // Ghép câu SQL động (nếu có từ khóa)
        String base = "SELECT id, name, phone, email, points, created_at FROM customers ";
        String where = "";
        boolean hasKw = keyword != null && !keyword.isBlank();
        if (hasKw) {
            where = "WHERE (name LIKE ? OR phone LIKE ? OR email LIKE ?) ";
        }
        String tail = "ORDER BY created_at DESC LIMIT ? OFFSET ?";

        List<Customer> list = new ArrayList<>();
        // Mở kết nối + chuẩn bị truy vấn
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(base + where + tail)) {
            int i = 1;
            // Nếu có từ khóa, gán giá trị cho 3 dấu ?
            if (hasKw) {
                String kw = "%" + keyword.trim() + "%";
                ps.setString(i++, kw);
                ps.setString(i++, kw);
                ps.setString(i++, kw);
            }
            // Gán limit và offset
            ps.setInt(i++, pageSize);
            ps.setInt(i, offset);

            // Thực thi và đọc kết quả
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs)); // ánh xạ từng dòng thành Customer
            }
        }
        return list;
    }

    /**
     * Đếm tổng số khách hàng (có thể theo keyword để phục vụ phân trang).
     */
    public int count(String keyword) throws SQLException {
        String base = "SELECT COUNT(*) FROM customers ";
        String where = "";
        boolean hasKw = keyword != null && !keyword.isBlank();
        if (hasKw) where = "WHERE (name LIKE ? OR phone LIKE ? OR email LIKE ?)";
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(base + where)) {
            if (hasKw) {
                String kw = "%" + keyword.trim() + "%";
                ps.setString(1, kw);
                ps.setString(2, kw);
                ps.setString(3, kw);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1); // lấy giá trị COUNT(*)
            }
        }
    }

    /**
     * Ánh xạ một dòng trong ResultSet thành đối tượng Customer.
     * @param rs kết quả truy vấn
     * @return đối tượng Customer tương ứng
     */
    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setPoints(rs.getInt("points"));
        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts != null ? ts.toLocalDateTime() : null); // chuyển Timestamp → LocalDateTime
        return c;
    }
}
