package com.example.storemanagement.service; // Package service: chứa các lớp xử lý nghiệp vụ (Business Logic)

/*
 * FinanceService – Tầng nghiệp vụ Tài chính
 * - Thêm/Sửa/Xoá báo cáo thu/chi
 * - Lọc & Phân trang theo store/date/type
 * - Tổng hợp Income/Expense/Balance
 *
 * (Giữ nguyên code, CHỈ bổ sung chú thích giải thích từng phần cho dễ hiểu.)
 */

import java.math.BigDecimal;  // Dùng BigDecimal để xử lý số tiền chính xác
import java.sql.SQLException; // Ngoại lệ JDBC ném lên cho UI quyết định hiển thị
import java.time.LocalDate;   // Ngày (không kèm thời gian) cho báo cáo
import java.util.List;        // Danh sách kết quả
import java.util.Optional;    // Kết quả có thể rỗng

import com.example.storemanagement.dao.FinanceDAO;     // DAO làm việc với bảng/report tài chính
import com.example.storemanagement.model.FinanceReport; // POJO đại diện 1 bản ghi báo cáo

public class FinanceService { // Lớp service trung gian giữa Controller và DAO đối với nghiệp vụ tài chính

    private final FinanceDAO financeDAO = new FinanceDAO(); // Khởi tạo DAO (sau này có thể inject qua constructor)

    // ===================== CRUD =====================

    public long addIncome(long storeId, LocalDate date, BigDecimal amount, String category, String note) throws SQLException {
        // API tiện lợi để tạo báo cáo THU (INCOME); ủy quyền cho hàm add() chung
        return add(storeId, date, amount, category, note, FinanceReport.Type.INCOME);
    }

    public long addExpense(long storeId, LocalDate date, BigDecimal amount, String category, String note) throws SQLException {
        // API tiện lợi để tạo báo cáo CHI (EXPENSE); ủy quyền cho hàm add() chung
        return add(storeId, date, amount, category, note, FinanceReport.Type.EXPENSE);
    }

    public boolean updateReport(FinanceReport r) throws SQLException {
        // Cập nhật 1 bản ghi báo cáo sau khi kiểm tra hợp lệ
        validateReport(r);            // kiểm tra các trường bắt buộc
        return financeDAO.update(r);  // gọi DAO thực hiện UPDATE
    }

    public boolean deleteReport(long id) throws SQLException {
        // Xoá 1 báo cáo theo ID
        return financeDAO.delete(id);
    }

    public Optional<FinanceReport> getById(long id) throws SQLException {
        // Lấy chi tiết 1 báo cáo theo ID (trả Optional để caller tự xử lý nếu null)
        return financeDAO.findById(id);
    }

    // ===================== Query & Summary =====================

    public List<FinanceReport> filter(Long storeId, LocalDate from, LocalDate to, FinanceReport.Type type,
                                      int page, int pageSize) throws SQLException {
        // Lọc báo cáo theo cửa hàng/khoảng thời gian/loại giao dịch + phân trang
        return financeDAO.filter(storeId, from, to, type, page, pageSize);
    }

    public FinanceDAO.Summary summarize(Long storeId, LocalDate from, LocalDate to, FinanceReport.Type type) throws SQLException {
        // Tổng hợp số liệu (Income/Expense/Balance) trong khoảng thời gian và phạm vi cho trước
        return financeDAO.summarize(storeId, from, to, type);
    }

    // ===================== Helpers =====================

    private long add(long storeId, LocalDate date, BigDecimal amount, String category, String note, FinanceReport.Type type) throws SQLException {
        // Hàm nội bộ để tạo báo cáo (dùng chung cho Income/Expense)
        if (storeId <= 0) throw new IllegalArgumentException("storeId không hợp lệ");           // bắt buộc storeId > 0
        if (date == null) throw new IllegalArgumentException("Ngày báo cáo không được để trống"); // bắt buộc có ngày
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Số tiền phải > 0");                              // số tiền phải dương
        FinanceReport r = new FinanceReport();  // Tạo đối tượng POJO và gán dữ liệu
        r.setStoreId(storeId);                  // ID cửa hàng
        r.setReportDate(date);                  // Ngày báo cáo
        r.setType(type);                        // Loại (INCOME/EXPENSE)
        r.setCategory(category);                // Danh mục (tùy chọn)
        r.setAmount(amount);                    // Số tiền
        r.setNote(note);                        // Ghi chú (tùy chọn)
        return financeDAO.insert(r);            // Gọi DAO thực hiện INSERT, trả về ID tự tăng
    }

    private void validateReport(FinanceReport r) {
        // Kiểm tra hợp lệ khi UPDATE (vì add() đã kiểm tra riêng ở trên)
        if (r == null) throw new IllegalArgumentException("Thiếu dữ liệu báo cáo");
        if (r.getId() == null) throw new IllegalArgumentException("Thiếu ID báo cáo"); // Update phải có ID
        if (r.getStoreId() == null || r.getStoreId() <= 0) throw new IllegalArgumentException("storeId không hợp lệ");
        if (r.getReportDate() == null) throw new IllegalArgumentException("Ngày báo cáo không được để trống");
        if (r.getAmount() == null || r.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Số tiền phải > 0");
        if (r.getType() == null) throw new IllegalArgumentException("Type không hợp lệ");
    }
}
