package com.example.storemanagement.controller; // Khai báo package: nhóm lớp controller của ứng dụng vào cùng không gian tên

/*
 * MainController.java – Controller cho main.fxml
 *
 * Nhiệm vụ chính:
 * 1) Điều hướng giữa các màn hình: Customers, Inventory, Finance.
 * 2) Nạp FXML con vào vùng contentArea (StackPane) ở giữa.
 * 3) Hiển thị trạng thái (DB URL, user) ở thanh status (statusLabel).
 * 4) Xử lý các action cơ bản: About, Exit.
 *
 * Phụ thuộc tối thiểu:
 * - Chỉ JavaFX + lớp Main (để đọc cấu hình DB). Không phụ thuộc DAO/Service
 *   nên có thể chạy ngay khi chưa tạo xong backend.
 *
 * Đường dẫn tài nguyên (resource path):
 * - "/fxml/customers.fxml"
 * - "/fxml/inventory.fxml"
 * - "/fxml/finance.fxml"
 *
 * Gợi ý sau này:
 * - Có thể tạo lớp NavigationService để tách việc nạp view.
 * - Có thể thêm Breadcrumb, hoặc TabPane nếu muốn nhiều tab.
 *
 * (Phiên bản này CHỈ bổ sung/chỉnh sửa CHÚ THÍCH cho dễ hiểu; KHÔNG đổi bất kỳ dòng code thực thi nào.)
 */

import java.io.IOException; // IOException: ném khi đọc/nạp FXML thất bại
import java.net.URL; // URL: tham chiếu tài nguyên trong classpath (FXML)
import java.util.Properties; // Properties: đọc cấu hình DB từ Main

import com.example.storemanagement.Main; // Tham chiếu tới Main để lấy dbProps

import javafx.event.ActionEvent; // Sự kiện action (click menu/nút)
import javafx.fxml.FXML; // Chú thích @FXML để inject node/handler từ FXML
import javafx.fxml.FXMLLoader; // Tiện ích nạp FXML thành cây Node
import javafx.scene.Node; // Kiểu tổng quát cho một phần tử UI
import javafx.scene.Parent; // Gốc UI khi nạp FXML
import javafx.scene.control.Alert; // Hộp thoại thông báo/cảnh báo
import javafx.scene.control.Label; // Nhãn hiển thị văn bản
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane; // Vùng chứa xếp chồng, dùng làm contentArea
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MainController { // Lớp controller gắn với main.fxml

    // Liên kết các node trong main.fxml qua fx:id
    @FXML
    private StackPane contentArea; // vùng hiển thị màn hình con (được đặt fx:id="contentArea" trong main.fxml)
    @FXML
    private Label statusLabel; // nhãn hiển thị thông tin trạng thái (DB, màn hình đang mở, ...)
    @FXML
    private MenuItem toggleMusicItem;

    private MediaPlayer mediaPlayer;
    private boolean musicPlaying = false;

    /**
     * Hàm này tự động chạy sau khi FXMLLoader nạp xong FXML.
     * Dùng để khởi tạo giao diện mặc định (màn hình chào + status DB).
     */
    @FXML
    public void initialize() { // phương thức lifecycle của JavaFX Controller

        playBackgroundMusic("/audio/music_background.mp3"); // 🔊 tự phát khi khởi động

        // Cập nhật status DB (đọc từ Main.getDbProps())
        updateStatusBar(); // hiển thị URL/User của DB trên thanh trạng thái
        // Hiển thị màn hình chào mừng mặc định
        showWelcome(); // đưa một Label chào mừng vào contentArea để tránh trắng màn hình
    }

    /**
     * Mở màn hình Quản lý Khách hàng.
     */
    @FXML
    public void openCustomers(ActionEvent event) { // handler cho MenuItem/Button "Customers"
        loadContent("/fxml/customers.fxml", "Customers"); // nạp FXML con vào contentArea và cập nhật status
    }

    /**
     * Mở màn hình Quản lý Kho hàng.
     */
    @FXML
    public void openInventory(ActionEvent event) { // handler cho "Inventory"
        loadContent("/fxml/inventory.fxml", "Inventory"); // nạp inventory.fxml
    }

    /**
     * Mở màn hình Quản lý Tài chính.
     */
    @FXML
    public void openFinance(ActionEvent event) { // handler cho "Finance"
        loadContent("/fxml/finance.fxml", "Finance"); // nạp finance.fxml
    }

    /**
     * Hiển thị hộp thoại giới thiệu.
     */
    @FXML
    public void openAbout(ActionEvent event) { // handler cho Menu "Help" → "About"
        String content = "Store Management System\n" +
                "Phiên bản: 0.1 (Core, chưa có login)\n" +
                "Tác giả: Phạm Ngọc Hưng – MSSV: 20235342"; // nội dung giới thiệu
        showAlert(Alert.AlertType.INFORMATION, "About", content); // bật hộp thoại thông tin
    }

    /**
     * Thoát ứng dụng một cách an toàn.
     */
    @FXML
    public void exitApp(ActionEvent event) { // handler cho "File" → "Exit"
        // Tắt toàn bộ JavaFX Application Thread
        javafx.application.Platform.exit(); // đóng ứng dụng JavaFX an toàn
    }

    // ================== Helpers ==================

    /**
     * Hiển thị một màn hình chào đơn giản khi khởi động.
     * Không phụ thuộc FXML con để tránh lỗi nếu chưa tạo customers.fxml, ...
     */
    private void showWelcome() { // tạo Label chào mừng và đưa vào vùng content
        Label welcome = new Label("👋 Welcome!\nHãy dùng menu hoặc nút bên trái để chuyển màn hình.\n" +
                "Module hiện có: Customers, Inventory, Finance.\n" +
                "(Có thể thêm Login/Menu khác trong tương lai)"); // nội dung text của nhãn chào mừng
        welcome.setWrapText(true); // cho phép tự xuống dòng khi dài
        welcome.setStyle("-fx-font-size: 14px; -fx-padding: 24;"); // định dạng giao diện cơ bản

        setContent(welcome); // thay thế nội dung hiện tại của contentArea bằng nhãn này
    }

    /**
     * Tải FXML con và hiển thị vào contentArea. Nếu có lỗi, báo Alert dễ hiểu.
     *
     * @param fxmlPath đường dẫn trong classpath (ví dụ: /fxml/customers.fxml)
     * @param title    tên hiển thị (không bắt buộc, dùng cho thông điệp)
     */
    private void loadContent(String fxmlPath, String title) { // helper trung tâm để nạp view theo đường dẫn
        try {
            URL fxml = getClass().getResource(fxmlPath); // tìm FXML trong resources theo đường dẫn cho trước
            if (fxml == null) { // nếu không thấy file
                throw new IllegalStateException("Không tìm thấy " + fxmlPath + " trong resources."); // ném lỗi có thông
                                                                                                     // điệp rõ ràng
            }
            Parent view = FXMLLoader.load(fxml); // nạp FXML thành cây UI (Parent)
            System.out.println("✅ Loaded view: " + title);
            setContent(view); // đặt cây UI này vào contentArea
            setStatusText("Opened: " + title); // cập nhật status bar thông báo đã mở màn nào
        } catch (IOException | IllegalStateException ex) { // bắt lỗi IO (file hỏng/không đọc được) hoặc lỗi trạng thái
            System.err.println("⚠️ Lỗi khi load " + fxmlPath);
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load View Failed",
                    "Không thể nạp màn hình: " + title + "\n" + ex.getMessage()); // thông báo lỗi thân thiện cho người
                                                                                  // dùng
        }
    }

    /**
     * Thay thế nội dung ở contentArea bằng node mới.
     */
    private void setContent(Node node) { // helper gói gọn thao tác thay đổi nội dung trung tâm
        contentArea.getChildren().setAll(node); // xóa mọi phần tử cũ và thêm duy nhất node mới vào StackPane
    }

    /**
     * Cập nhật thanh trạng thái dựa trên cấu hình DB đang dùng.
     */
    private void updateStatusBar() { // lấy thông tin từ Main.getDbProps() và hiển thị
        Properties p = Main.getDbProps(); // truy xuất cấu hình DB đã nạp ở Main
        String url = p != null ? p.getProperty("url", "jdbc:mysql://localhost:3306/store_management") : "(unknown)"; // lấy
                                                                                                                     // URL
                                                                                                                     // hoặc
                                                                                                                     // mặc
                                                                                                                     // định
        String user = p != null ? p.getProperty("user", "root") : "(unknown)"; // lấy user hoặc mặc định
        setStatusText("DB: " + url + "  |  User: " + user); // ghép chuỗi rồi đặt vào statusLabel
    }

    /**
     * Đặt nội dung text cho statusLabel (null-safe).
     */
    private void setStatusText(String text) { // thay đổi nội dung nhãn trạng thái nếu đã được inject
        if (statusLabel != null) { // tránh NullPointerException nếu FXML chưa map đúng
            statusLabel.setText(text); // cập nhật văn bản hiển thị
        }
    }

    /** Hiển thị Alert tiện lợi */
    private void showAlert(Alert.AlertType type, String title, String content) { // helper tạo hộp thoại theo loại
        Alert a = new Alert(type); // khởi tạo Alert với kiểu (INFORMATION/ERROR/...)
        a.setTitle(title); // tiêu đề hộp thoại
        a.setHeaderText(null); // bỏ header cho gọn
        a.setContentText(content); // nội dung thông điệp
        a.show(); // hiển thị (không chặn luồng)
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
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop vô hạn
            mediaPlayer.setVolume(0.25); // Âm lượng nhẹ
            mediaPlayer.play();
            musicPlaying = true;

            if (toggleMusicItem != null)
                toggleMusicItem.setText("🔊 Tắt nhạc nền");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onToggleMusic() {
        if (mediaPlayer == null)
            return;
        if (musicPlaying) {
            mediaPlayer.pause();
            toggleMusicItem.setText("🔈 Bật nhạc nền");
        } else {
            mediaPlayer.play();
            toggleMusicItem.setText("🔊 Tắt nhạc nền");
        }
        musicPlaying = !musicPlaying;
    }

    public void stopMusic() {
        if (mediaPlayer != null)
            mediaPlayer.stop();
    }
}
