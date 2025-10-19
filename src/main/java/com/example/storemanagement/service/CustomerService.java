package com.example.storemanagement.service; // Package service: chứa các lớp xử lý nghiệp vụ (Business Logic)

/*
 * CustomerService – Tầng nghiệp vụ cho Khách hàng
 * - Validate dữ liệu nhập (name/email/phone)
 * - Gọi CustomerDAO để CRUD + search + điều chỉnh điểm
 * - Ẩn bớt chi tiết SQLException, trả message dễ hiểu
 *
 * (Phiên bản này chỉ bổ sung chú thích chi tiết; KHÔNG thay đổi logic/code.)
 */

import java.sql.SQLException;                                  // Ngoại lệ chung JDBC
import java.sql.SQLIntegrityConstraintViolationException;       // Ngoại lệ vi phạm ràng buộc UNIQUE/FOREIGN KEY
import java.time.LocalDateTime;                                 // Thời điểm hiện tại khi tạo khách hàng
import java.util.List;                                          // Danh sách kết quả
import java.util.Optional;                                      // Kiểu kết quả có thể rỗng
import java.util.regex.Pattern;                                 // Regex kiểm tra định dạng email/phone

import com.example.storemanagement.dao.CustomerDAO;             // DAO làm việc với bảng customers
import com.example.storemanagement.model.Customer;              // POJO Customer

public class CustomerService {                                  // Lớp Service: trung gian giữa Controller/UI và DAO

    private final CustomerDAO customerDAO = new CustomerDAO();  // Khởi tạo DAO (có thể inject sau này)

    // Regex đơn giản – có thể thay bằng validator xịn hơn sau này
    private static final Pattern EMAIL_RE = Pattern.compile("^[^@\n]+@[^@\n]+\\.[^@\n]+$"); // Email tối giản: có @ và dấu chấm sau domain
    private static final Pattern PHONE_RE = Pattern.compile("^[0-9 +()-]{7,20}$");                 // Số, khoảng trắng, + ( ), độ dài 7–20

    /** Tạo khách hàng mới sau khi validate. Trả về ID vừa tạo. */
    public long createCustomer(String name, String phone, String email) throws SQLException {
        validateName(name);        // Kiểm tra tên: không trống, >= 2 ký tự
        validatePhone(phone);      // Kiểm tra số điện thoại (cho phép trống)
        validateEmail(email);      // Kiểm tra email (cho phép trống)

        Customer c = new Customer();                 // Tạo đối tượng Customer rỗng
        c.setName(name.trim());                      // Lưu tên đã trim
        c.setPhone(isBlank(phone) ? null : phone.trim()); // Nếu để trống → lưu null để rõ ràng
        c.setEmail(isBlank(email) ? null : email.trim()); // Tương tự cho email
        c.setPoints(0);                              // Điểm khởi tạo = 0
        c.setCreatedAt(LocalDateTime.now());         // Ghi nhận thời điểm tạo

        try {
            return customerDAO.insert(c);            // Gọi DAO chèn vào DB, trả về ID tự tăng
        } catch (SQLIntegrityConstraintViolationException dup) { // Bắt lỗi vi phạm UNIQUE (trùng phone/email)
            // Bắt trùng email/phone do UNIQUE ở DB, trả thông điệp dễ hiểu
            String msg = "Phone hoặc Email đã tồn tại";                                // Thông báo mặc định
            if (dup.getMessage() != null && dup.getMessage().contains("phone")) msg = "Số điện thoại đã tồn tại"; // Nhắm đúng trường
            if (dup.getMessage() != null && dup.getMessage().contains("email")) msg = "Email đã tồn tại";
            throw new SQLException(msg, dup);        // Quấn lại thành SQLException (ẩn chi tiết vendor-specific)
        }
    }

    /** Cập nhật thông tin khách hàng */
    public boolean updateCustomer(Customer c) throws SQLException {
        if (c.getId() == null) throw new IllegalArgumentException("Thiếu ID khách hàng"); // Update bắt buộc phải có ID
        validateName(c.getName());        // Validate tên
        validatePhone(c.getPhone());      // Validate số điện thoại (cho phép trống)
        validateEmail(c.getEmail());      // Validate email (cho phép trống)
        try {
            return customerDAO.update(c); // Gọi DAO thực hiện UPDATE
        } catch (SQLIntegrityConstraintViolationException dup) { // Bắt UNIQUE
            String msg = "Phone hoặc Email đã tồn tại";                                          // Thông điệp thân thiện
            if (dup.getMessage() != null && dup.getMessage().contains("phone")) msg = "Số điện thoại đã tồn tại";
            if (dup.getMessage() != null && dup.getMessage().contains("email")) msg = "Email đã tồn tại";
            throw new SQLException(msg, dup);     // Ném ra SQLException cho tầng trên xử lý hiển thị
        }
    }

    /** Xoá khách hàng theo ID */
    public boolean deleteCustomer(long id) throws SQLException {
        return customerDAO.delete(id);    // Ủy quyền cho DAO; trả về true nếu xóa thành công
    }

    /** Lấy chi tiết khách hàng */
    public Optional<Customer> getById(long id) throws SQLException {
        return customerDAO.findById(id);  // Trả về Optional để UI tự xử lý nếu không tồn tại
    }

    /** Tìm kiếm + phân trang */
    public List<Customer> search(String keyword, int page, int pageSize) throws SQLException {
        return customerDAO.search(keyword, page, pageSize); // Gọi DAO search (LIKE name/phone/email)
    }

    /** Tổng số dòng cho bộ lọc hiện tại (phục vụ Pagination) */
    public int count(String keyword) throws SQLException {
        return customerDAO.count(keyword); // Đếm tổng số bản ghi phù hợp với keyword
    }

    /** Cộng/trừ điểm tích luỹ (delta âm hoặc dương) */
    public void adjustPoints(long customerId, int delta) throws SQLException {
        // Đơn giản: đọc hiện tại và cập nhật, đảm bảo không âm
        Customer c = customerDAO.findById(customerId)                                      // Tìm khách theo ID
                .orElseThrow(() -> new SQLException("Không tìm thấy khách hàng"));        // Không có → ném lỗi rõ ràng
        int now = c.getPoints() == null ? 0 : c.getPoints();                                // Điểm hiện tại (nếu null coi như 0)
        int next = Math.max(0, now + delta);                                               // Không cho phép âm
        c.setPoints(next);                                                                  // Gán điểm mới
        customerDAO.update(c);                                                              // Lưu vào DB
    }

    // ===================== VALIDATION =====================
    private void validateName(String name) {                                                // Kiểm tra tên bắt buộc
        if (isBlank(name) || name.trim().length() < 2)
            throw new IllegalArgumentException("Tên khách hàng không hợp lệ");             // Thông điệp cho UI
    }

    private void validateEmail(String email) {                                              // Kiểm tra email (tùy chọn)
        if (isBlank(email)) return; // cho phép trống
        if (!EMAIL_RE.matcher(email.trim()).matches())
            throw new IllegalArgumentException("Email không hợp lệ");
    }

    private void validatePhone(String phone) {                                              // Kiểm tra số điện thoại (tùy chọn)
        if (isBlank(phone)) return; // cho phép trống
        if (!PHONE_RE.matcher(phone.trim()).matches())
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
    }

    private boolean isBlank(String s) {                                                     // Tiện ích: null hoặc toàn khoảng trắng?
        return s == null || s.trim().isEmpty();
    }
}
