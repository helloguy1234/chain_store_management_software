package com.example.storemanagement.dao; // Package DAO: lớp truy cập dữ liệu cho bảng/report tài chính

/*
 * FinanceDAO – CRUD báo cáo tài chính + lọc theo store/date/type và tổng hợp.
 * (Giữ nguyên logic, chỉ bổ sung chú thích để dễ hiểu từng dòng.)
 */

import java.math.BigDecimal;           // Kiểu số tiền chính xác
import java.sql.Connection;            // Kết nối JDBC
import java.sql.Date;                  // java.sql.Date cho cột DATE (không kèm thời gian)
import java.sql.PreparedStatement;     // Câu lệnh SQL có tham số (an toàn)
import java.sql.ResultSet;             // Kết quả SELECT
import java.sql.SQLException;          // Ngoại lệ JDBC
import java.sql.Statement;             // Để yêu cầu trả về generated keys
import java.sql.Timestamp;             // Thời điểm (map sang LocalDateTime)
import java.time.LocalDate;            // Ngày thuần (API thời gian mới)
import java.util.ArrayList;            // Danh sách động
import java.util.List;                 // Interface List
import java.util.Optional;             // Giá trị có/không

import com.example.storemanagement.model.FinanceReport; // POJO FinanceReport

public class FinanceDAO {              // DAO làm việc với bảng finance_reports

    public long insert(FinanceReport r) throws SQLException { // Thêm một bản ghi tài chính, trả ID tự tăng
        String sql = "INSERT INTO finance_reports(store_id, report_date, type, category, amount, note) VALUES(?,?,?,?,?,?)"; // Câu INSERT
        try (Connection cn = DBConnection.getInstance().getConnection();                 // Mở kết nối
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // Yêu cầu trả về id
            ps.setLong(1, r.getStoreId());                       // store_id
            ps.setDate(2, Date.valueOf(r.getReportDate()));      // report_date (java.sql.Date từ LocalDate)
            ps.setString(3, r.getType().name());                 // type: INCOME/EXPENSE
            ps.setString(4, r.getCategory());                    // category
            ps.setBigDecimal(5, r.getAmount());                  // amount
            ps.setString(6, r.getNote());                        // note
            ps.executeUpdate();                                   // Thực thi
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getLong(1); } // Lấy ID tự tăng
        }
        return -1L;                                              // Không có id trả về
    }

    public boolean update(FinanceReport r) throws SQLException { // Cập nhật bản ghi tài chính
        String sql = "UPDATE finance_reports SET store_id=?, report_date=?, type=?, category=?, amount=?, note=? WHERE id=?"; // UPDATE theo id
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, r.getStoreId());                       // store_id
            ps.setDate(2, Date.valueOf(r.getReportDate()));      // report_date
            ps.setString(3, r.getType().name());                 // type
            ps.setString(4, r.getCategory());                    // category
            ps.setBigDecimal(5, r.getAmount());                  // amount
            ps.setString(6, r.getNote());                        // note
            ps.setLong(7, r.getId());                            // WHERE id = ?
            return ps.executeUpdate() > 0;                       // true nếu có dòng bị ảnh hưởng
        }
    }

    public boolean delete(long id) throws SQLException {         // Xoá bản ghi theo id
        String sql = "DELETE FROM finance_reports WHERE id=?";  // Câu DELETE
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);                                   // id cần xoá
            return ps.executeUpdate() > 0;                       // trả về true nếu xoá thành công
        }
    }

    public Optional<FinanceReport> findById(long id) throws SQLException { // Tìm theo id
        String sql = "SELECT id, store_id, report_date, type, category, amount, note, created_at FROM finance_reports WHERE id=?"; // Lấy đủ trường cần map
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);                                   // gán id
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); } // map nếu có
            return Optional.empty();                              // không có
        }
    }

    /**
     * Lọc theo storeId (tuỳ chọn), khoảng ngày (tuỳ chọn), type (tuỳ chọn), có phân trang.
     */
    public List<FinanceReport> filter(Long storeId, LocalDate from, LocalDate to, FinanceReport.Type type, int page, int pageSize) throws SQLException { // Truy vấn có điều kiện + phân trang
        if (page < 1) page = 1; if (pageSize <= 0) pageSize = 20; int offset = (page - 1) * pageSize; // Chuẩn hoá tham số
        StringBuilder sb = new StringBuilder();                    // Ghép SQL theo điều kiện
        sb.append("SELECT id, store_id, report_date, type, category, amount, note, created_at FROM finance_reports WHERE 1=1 "); // WHERE 1=1 để nối AND thuận tiện
        List<Object> params = new ArrayList<>();                   // Danh sách tham số cho PreparedStatement
        if (storeId != null) { sb.append("AND store_id = ? "); params.add(storeId); }               // Lọc theo store
        if (from != null)    { sb.append("AND report_date >= ? "); params.add(Date.valueOf(from)); } // Từ ngày
        if (to != null)      { sb.append("AND report_date <= ? "); params.add(Date.valueOf(to)); }   // Đến ngày
        if (type != null)    { sb.append("AND type = ? "); params.add(type.name()); }                // Theo loại
        sb.append("ORDER BY report_date DESC, id DESC LIMIT ? OFFSET ?");                             // Sắp xếp + phân trang
        params.add(pageSize); params.add(offset);                                                      // Thêm LIMIT/OFFSET vào danh sách tham số

        List<FinanceReport> list = new ArrayList<>();             // Kết quả trả về
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sb.toString())) { // Tạo PreparedStatement với SQL đã ghép
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));               // Gán tham số theo thứ tự
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }            // Thực thi & map từng dòng
        }
        return list;                                              // Danh sách bản ghi thỏa điều kiện
    }

    /**
     * Tổng hợp: trả về income, expense, balance cho bộ lọc hiện tại.
     */
    public Summary summarize(Long storeId, LocalDate from, LocalDate to, FinanceReport.Type type) throws SQLException { // Tính tổng thu/chi/cân bằng
        StringBuilder sb = new StringBuilder();                    // Ghép SQL
        sb.append("SELECT ")
          .append("SUM(CASE WHEN type='INCOME' THEN amount ELSE 0 END) AS income, ") // Tổng thu
          .append("SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END) AS expense ") // Tổng chi
          .append("FROM finance_reports WHERE 1=1 ");
        List<Object> params = new ArrayList<>();                   // Tham số lọc
        if (storeId != null) { sb.append("AND store_id = ? "); params.add(storeId); }               // Lọc theo store
        if (from != null)    { sb.append("AND report_date >= ? "); params.add(Date.valueOf(from)); } // Từ ngày
        if (to != null)      { sb.append("AND report_date <= ? "); params.add(Date.valueOf(to)); }   // Đến ngày
        if (type != null)    { sb.append("AND type = ? "); params.add(type.name()); }                // Theo loại (nếu muốn chỉ 1 loại)

        BigDecimal income = BigDecimal.ZERO; BigDecimal expense = BigDecimal.ZERO; // Giá trị mặc định nếu không có bản ghi
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));               // Gán tham số
            try (ResultSet rs = ps.executeQuery()) {                                                  // Thực thi
                if (rs.next()) {                                                                      // Đọc dòng tổng hợp
                    income = rs.getBigDecimal("income");   if (income == null) income = BigDecimal.ZERO;  // Null → 0
                    expense = rs.getBigDecimal("expense"); if (expense == null) expense = BigDecimal.ZERO; // Null → 0
                }
            }
        }
        return new Summary(income, expense, income.subtract(expense)); // Trả về đối tượng tổng hợp
    }

    private FinanceReport map(ResultSet rs) throws SQLException { // Chuyển 1 dòng ResultSet → FinanceReport
        FinanceReport r = new FinanceReport();                    // Tạo POJO rỗng
        r.setId(rs.getLong("id"));                               // id
        r.setStoreId(rs.getLong("store_id"));                    // store_id
        Date d = rs.getDate("report_date");                      // report_date (DATE)
        r.setReportDate(d != null ? d.toLocalDate() : null);      // Map sang LocalDate an toàn null
        String t = rs.getString("type");                          // type: INCOME/EXPENSE
        r.setType(t != null ? FinanceReport.Type.valueOf(t) : null); // Ép kiểu về enum nếu không null
        r.setCategory(rs.getString("category"));                 // category
        r.setAmount(rs.getBigDecimal("amount"));                 // amount
        r.setNote(rs.getString("note"));                         // note
        Timestamp ts = rs.getTimestamp("created_at");            // created_at (TIMESTAMP)
        r.setCreatedAt(ts != null ? ts.toLocalDateTime() : null); // Map sang LocalDateTime an toàn null
        return r;                                                 // Trả về kết quả đã map
    }

    // DTO tổng hợp đơn giản để trả về cho Service/Controller
    public static class Summary {
        public final BigDecimal income;  // Tổng thu
        public final BigDecimal expense; // Tổng chi
        public final BigDecimal balance; // Cân bằng = income - expense
        public Summary(BigDecimal income, BigDecimal expense, BigDecimal balance) {
            this.income = income; this.expense = expense; this.balance = balance; // Gán ba trường bất biến
        }
    }
}
