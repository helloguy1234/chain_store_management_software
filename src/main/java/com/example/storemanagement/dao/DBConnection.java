package com.example.storemanagement.dao; // Khai báo package: nhóm lớp theo không gian tên com.example.storemanagement.dao

/*
 * DBConnection – Singleton quản lý kết nối MySQL bằng JDBC.
 *
 * Mục tiêu chú thích:
 * - Giải thích TỪNG DÒNG, dễ hiểu, không đổi logic/mã thực thi (chỉ thay đổi/ghi chú thích).
 * - Nguồn cấu hình: /database/db.properties; có thể ghi đè bằng biến môi trường.
 * - Cách dùng: DBConnection.getInstance().getConnection() trong DAO/Service.
 * - Gợi ý: Đặt mysql-connector-j trong pom.xml để DriverManager tìm thấy driver.
 */

import java.io.InputStream;             // Đọc file cấu hình từ classpath (db.properties)
import java.sql.Connection;              // Kiểu đối tượng kết nối JDBC tới CSDL
import java.sql.DriverManager;           // Lớp tạo Connection từ URL, user, password
import java.sql.SQLException;            // Ngoại lệ sinh ra khi lỗi thao tác JDBC
import java.util.Objects;                // Tiện ích null-safe (Objects.toString, ...)
import java.util.Properties;             // Lưu cặp key=value cấu hình DB

public class DBConnection {              // Lớp chịu trách nhiệm quản lý kết nối DB theo mẫu Singleton
    private static volatile DBConnection instance; // Tham chiếu Singleton; volatile để an toàn đồng bộ đa luồng
    private final Properties props = new Properties(); // Đối tượng lưu cấu hình (url, user, password)

    private DBConnection() {             // Constructor private: ngăn tạo mới từ bên ngoài (bắt buộc cho Singleton)
        loadProps();                     // Gọi hàm nạp cấu hình từ file + biến môi trường + mặc định
        // Đảm bảo driver được nạp (MySQL 8: com.mysql.cj.jdbc.Driver). Không bắt buộc với JDBC 4+, nhưng an toàn.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Cố gắng nạp lớp driver MySQL; nếu thiếu thì DriverManager có thể không kết nối được
        } catch (ClassNotFoundException ignored) {}     // Nếu không tìm thấy driver, bỏ qua (vì đa số JDBC 4+ auto-load), nhưng nên kiểm tra pom.xml
    }

    @SuppressWarnings("DoubleCheckedLocking")
    public static DBConnection getInstance() {          // Phương thức truy cập Singleton (global access point)
        if (instance == null) {                         // Kiểm tra nhanh (không khóa) để tránh tạo thừa
            synchronized (DBConnection.class) {         // Đồng bộ hóa để chỉ một luồng tạo instance tại một thời điểm
                if (instance == null) instance = new DBConnection(); // Double-checked locking: tạo đối tượng nếu vẫn chưa được tạo
            }
        }
        return instance;                                // Trả về thể hiện Singleton
    }

    private void loadProps() {                          // Nạp cấu hình vào props theo thứ tự: file → ENV → mặc định
        // 1) Từ classpath
        try (InputStream in = getClass().getResourceAsStream("/database/db.properties")) { // Mở luồng đọc file db.properties trong resources/database
            if (in != null) props.load(in);            // Nếu file tồn tại, nạp toàn bộ cặp key=value vào props
        } catch (Exception e) {                        // Bắt mọi lỗi IO/đọc file để không làm app dừng
            System.err.println("[DB] Không đọc được /database/db.properties: " + e.getMessage()); // In cảnh báo ra STDERR (hữu ích khi debug)
        }
        
        // 2) Fallback ENV
        // props.putIfAbsent("url", System.getenv("DB_URL"));       // Nếu thiếu key "url" thì lấy từ biến môi trường DB_URL
        // props.putIfAbsent("user", System.getenv("DB_USER"));     // Nếu thiếu key "user" thì lấy từ DB_USER
        // props.putIfAbsent("password", System.getenv("DB_PASSWORD")); // Nếu thiếu key "password" thì lấy từ DB_PASSWORD
        // 3) Giá trị mặc định an toàn
        // props.putIfAbsent("url", "jdbc:mysql://localhost:3306/store_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"); // URL mặc định: localhost, schema store_management, tắt SSL, set timezone
        // props.putIfAbsent("user", "root");          // Tên người dùng mặc định (tùy máy có thể khác)
        // props.putIfAbsent("password", "");          // Mật khẩu mặc định rỗng (hãy đổi trong môi trường thực tế)
    }

    public Connection getConnection() throws SQLException { // Cấp một Connection mới dựa trên cấu hình hiện tại
        String url = Objects.toString(props.getProperty("url"), "");         // Lấy URL; nếu null thì thành chuỗi rỗng để tránh NullPointerException
        String user = Objects.toString(props.getProperty("user"), "");       // Lấy user; fallback rỗng
        String pass = Objects.toString(props.getProperty("password"), "");   // Lấy password; fallback rỗng
        return DriverManager.getConnection(url, user, pass); // Tạo và trả về kết nối JDBC tới MySQL; có thể ném SQLException nếu sai thông số/DB down
    }
}
