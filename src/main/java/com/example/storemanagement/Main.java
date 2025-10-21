package com.example.storemanagement;

/*
 * =============================================================
 *  Store Management System – Java + MySQL (no login for now)
 *  Main.java – Điểm khởi chạy JavaFX, nạp FXML chính, đọc db.properties
 *
 *
 *  Thư mục liên quan (theo README):
 *  src/main/resources/
 *    ├─ fxml/main.fxml            ← Giao diện khung chính (controller: MainController)
 *    ├─ fxml/customers.fxml       ← Màn hình Khách hàng
 *    ├─ fxml/inventory.fxml       ← Màn hình Kho hàng
 *    ├─ fxml/finance.fxml         ← Màn hình Tài chính
 *    ├─ css/style.css             ← CSS cho UI (tùy chọn)
 *    └─ database/db.properties    ← Thông tin kết nối MySQL
 *
 *  Cách chạy (Maven):
 *  mvn clean compile
 *  mvn exec:java -Dexec.mainClass="com.example.storemanagement.Main"
 *
 *  Nếu dùng JavaFX SDK độc lập (không dùng BOM), VM Options cần:
 *    --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml
 *  (Trên macOS/Linux dùng $PATH_TO_FX)
 *
 *  Gợi ý kiến trúc:
 *  - Controller: com.example.storemanagement.controller.*
 *  - Service:    com.example.storemanagement.service.*
 *  - DAO:        com.example.storemanagement.dao.* (JDBC → MySQL)
 *  - Model:      com.example.storemanagement.model.*
 *  - Util:       com.example.storemanagement.util.*
 *
 *  Ghi chú:
 *  - File này **không** gọi trực tiếp DAO/Service để đảm bảo compile chạy được
 *    ngay cả khi bạn chưa tạo xong các lớp còn lại. Phần "kiểm tra kết nối DB"
 *    có ví dụ nhưng được COMMENT lại. Bạn mở comment khi đã tạo DBConnection.
 * =============================================================
 */
// 1️⃣ Thêm import:
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javafx.application.Application;
import javafx.fxml.FXMLLoader; // Import lớp InputStream để đọc file từ classpath
import javafx.scene.Parent; // Import URL để tham chiếu tài nguyên (FXML/CSS) trong classpath
import javafx.scene.Scene; // Import Properties để nạp cấu hình từ db.properties
import javafx.scene.control.Alert; // Lớp nền của mọi ứng dụng JavaFX
import javafx.scene.control.Label; // Tiện ích nạp file FXML thành cây UI
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane; // Gốc của cây UI (root node)
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage; // Cảnh (scene) chứa các node UI

public class Main extends Application { // Khai báo lớp Main kế thừa Application để chạy JavaFX
    private MediaPlayer mediaPlayer;
    // Tên app hiển thị trên cửa sổ
    public static final String APP_NAME = "Quản lý cửa hàng"; // Hằng số tiêu đề cửa sổ

    // Thuộc tính CSDL đọc từ resources/database/db.properties
    private static Properties dbProps; // Biến tĩnh giữ cấu hình DB để dùng chung

    public static void main(String[] args) { // Hàm main tiêu chuẩn của Java
        // Điểm vào chuẩn JavaFX – sẽ gọi start(Stage)
        launch(args); // Gọi launch() để khởi động runtime JavaFX, sau đó gọi start()
    }

    @Override // Chú thích cho trình biên dịch biết ta đang ghi đè phương thức từ lớp cha
    public void start(Stage primaryStage) { // JavaFX sẽ truyền vào Stage chính (cửa sổ)

        // 🔊 Gọi hàm phát nhạc nền
        playBackgroundMusic("/audio/music_background.mp3");

        // 1) Đặt tiêu đề cửa sổ
        primaryStage.setTitle(APP_NAME); // Gán tiêu đề cho cửa sổ chính

        // 2️⃣ Nạp icon từ resources và gắn vào cửa sổ
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/logostb.jpeg"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("⚠️ Không tìm thấy /images/logostb.jpeg: " + e.getMessage());
        }

        // 2) Đọc cấu hình DB (nếu có). Không bắt buộc để chạy UI.
        dbProps = loadDbProperties(); // Nạp db.properties; nếu thiếu vẫn trả về giá trị mặc định

        // 3) Cố gắng nạp giao diện chính từ FXML
        try { // Bọc trong try-catch để nếu FXML thiếu thì vẫn chạy với UI dự phòng
            Scene scene = loadMainScene(); // Thử nạp main.fxml thành một Scene
            attachCssIfPresent(scene, "/css/style.css"); // Nếu có CSS trong classpath thì gắn vào Scene
            primaryStage.setScene(scene); // Đặt Scene chính cho Stage
            primaryStage.show(); // Hiển thị cửa sổ ứng dụng
        } catch (Exception ex) { // Nếu có lỗi (ví dụ thiếu FXML), sẽ vào nhánh này
            // Nếu FXML hoặc controller chưa sẵn sàng → dựng giao diện dự phòng
            Scene fallback = buildFallbackScene(ex); // Tạo Scene đơn giản thông báo cần tạo FXML
            primaryStage.setScene(fallback); // Gán Scene dự phòng vào Stage
            primaryStage.show(); // Vẫn hiển thị app để dev dễ sửa
        }

        // 4) (TÙY CHỌN) Kiểm tra kết nối DB khi bạn đã có DBConnection
        // → BỎ COMMENT sau khi tạo lớp dao.DBConnection và MySQL Connector
        try {
            var conn = com.example.storemanagement.dao.DBConnection.getInstance().getConnection(); // Lấy kết nối DB từ
                                                                                                   // singleton
            if (conn != null && !conn.isClosed()) { // Kiểm tra kết nối hợp lệ
                info("Kết nối MySQL thành công: " + dbProps.getProperty("url", "(chưa thiết lập)")); // Báo thông tin
                                                                                                     // thành công
            }
        } catch (Exception e) {
            warn("Không thể kết nối MySQL. Kiểm tra db.properties & MySQL service.\n" + e.getMessage()); // Cảnh báo khi
                                                                                                         // kết nối lỗi
        }
    }

    private void playBackgroundMusic(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                System.err.println("Không tìm thấy file nhạc: " + resourcePath);
                return;
            }

            Media media = new Media(resource.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // 🔁 Loop vô hạn
            mediaPlayer.setVolume(0.9); // Âm lượng 90%
            mediaPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    /**
     * Nạp Scene từ main.fxml (nằm trong resources/fxml/main.fxml)
     */
    private Scene loadMainScene() throws Exception { // Phương thức helper để nạp Scene chính từ FXML
        URL fxml = getResource("/fxml/main.fxml"); // Tìm tài nguyên main.fxml trong classpath

        if (fxml == null) { // Nếu không tìm thấy URL
            throw new IllegalStateException("Không tìm thấy /fxml/main.fxml trong resources."); // Ném lỗi để rơi vào
                                                                                                // catch ở start()
        }
        Parent root = FXMLLoader.load(fxml); // Dùng FXMLLoader để đọc FXML và tạo cây node UI
        Scene scene = new Scene(root); // Gói root node vào một Scene mới
        // Scene scene = new
        // Scene(FXMLLoader.load(getClass().getResource("/fxml/customers.fxml")));

        return scene; // Trả về Scene đã tạo
    }

    /**
     * Đọc file cấu hình DB từ classpath: /database/db.properties
     * Trả về Properties rỗng nếu không tìm thấy để không chặn việc chạy UI.
     */
    private Properties loadDbProperties() { // Phương thức helper nạp cấu hình DB an toàn
        Properties props = new Properties(); // Tạo đối tượng Properties trống
        String path = "/database/db.properties"; // Đường dẫn file cấu hình trong resources
        try (InputStream in = getResourceAsStream(path)) { // Mở luồng đọc file qua classpath (tự đóng nhờ
                                                           // try-with-resources)
            if (in != null) { // Nếu tìm thấy file
                props.load(in); // Nạp các cặp key=value vào đối tượng Properties
                // info("Finded path for properties. \n");
            } else { // Nếu không có file trong resources
                warn("Không tìm thấy " + path + ". Sẽ dùng giá trị mặc định."); // Cảnh báo nhưng không dừng chương
                                                                                // trình
            }
        } catch (Exception e) { // Bắt mọi lỗi IO để không làm crash UI
            warn("Lỗi đọc " + path + ": " + e.getMessage()); // Thông báo lỗi đọc file
        }

        // Giá trị mặc định an toàn (không kết nối thật)
        // info("manual connection \n");
        // props.putIfAbsent("url", "jdbc:mysql://localhost:3306/store_management"); //
        // Nếu chưa có key url thì đặt mặc định
        // props.putIfAbsent("user", "root"); // Mặc định user root (có thể khác máy
        // bạn)
        // props.putIfAbsent("password", "aaaa"); // Mặc định password mẫu (hãy đổi
        // trong db.properties)
        return props; // Trả về Properties (có thể là file thật, có thể là mặc định)
    }

    /**
     * Gắn CSS nếu file tồn tại trong classpath.
     */
    private void attachCssIfPresent(Scene scene, String cssClasspathPath) { // Gắn stylesheet cho Scene nếu có
        URL css = getResource(cssClasspathPath); // Tìm CSS trong resources theo đường dẫn classpath
        if (css != null) { // Nếu tìm thấy
            scene.getStylesheets().add(css.toExternalForm()); // Chuyển URL thành chuỗi và thêm vào danh sách
                                                              // stylesheets
        }
    }

    /**
     * UI dự phòng khi chưa có FXML/Controller – giúp bạn chạy thử ngay.
     * Hiển thị thông báo cần tạo fxml & controller theo đúng cấu trúc.
     */
    private Scene buildFallbackScene(Exception cause) { // Tạo Scene đơn giản khi FXML chưa sẵn sàng
        BorderPane root = new BorderPane(); // Bố cục BorderPane để đặt nội dung giữa
        String message = "👋 Xin chào, dự án đã chạy!\n\n" +
                "Chưa nạp được main.fxml. Hãy tạo: src/main/resources/fxml/main.fxml\n" +
                "Controller gợi ý: com.example.storemanagement.controller.MainController\n\n" +
                "Chi tiết lỗi: " + (cause != null ? cause.getMessage() : "(không rõ)"); // Chuỗi thông báo hướng dẫn
                                                                                        // nhanh
        Label label = new Label(message); // Tạo Label hiển thị thông điệp
        label.setWrapText(true); // Cho phép xuống dòng tự động khi dài
        label.setStyle("-fx-padding: 24; -fx-font-size: 14px;"); // Thêm padding và cỡ chữ cho dễ đọc
        root.setCenter(label); // Đặt Label vào giữa BorderPane
        return new Scene(root, 960, 600); // Tạo Scene với kích thước mặc định để hiển thị
    }

    // ===== Helpers nhỏ cho Alert =====
    private void info(String msg) { // Hiển thị hộp thoại thông tin
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", msg); // Gọi helper chung với kiểu INFORMATION
    }

    private void warn(String msg) { // Hiển thị hộp thoại cảnh báo
        showAlert(Alert.AlertType.WARNING, "Chú ý", msg); // Gọi helper chung với kiểu WARNING
    }

    private void showAlert(Alert.AlertType type, String title, String msg) { // Helper tổng cho mọi loại Alert
        Alert alert = new Alert(type); // Tạo Alert theo kiểu đã chỉ định
        alert.setTitle(title); // Đặt tiêu đề hộp thoại
        alert.setHeaderText(null); // Bỏ phần header để giao diện gọn
        alert.setContentText(msg); // Nội dung thông báo
        alert.show(); // Hiển thị ngay (không chặn luồng)
    }

    // ===== Truy xuất tài nguyên tiện lợi =====
    private URL getResource(String path) { // Helper lấy URL tài nguyên theo đường dẫn classpath (bắt đầu bằng "/")
        return getClass().getResource(path); // Trả về URL; null nếu không tồn tại
    }

    private InputStream getResourceAsStream(String path) { // Helper lấy luồng InputStream của tài nguyên
        return getClass().getResourceAsStream(path); // Trả về InputStream; null nếu không tìm thấy
    }

    /**
     * Cho phép module khác đọc cấu hình DB đã load (nếu cần).
     * Ví dụ trong DAO bạn có thể dùng Main.getDbProps().getProperty("url").
     */
    public static Properties getDbProps() { // Getter tĩnh để truy xuất cấu hình DB ở nơi khác
        return dbProps; // Trả về đối tượng Properties đã nạp
    }
}