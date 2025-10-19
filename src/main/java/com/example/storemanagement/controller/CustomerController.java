package com.example.storemanagement.controller;

/*
 * CustomerController – Điều khiển màn hình customers.fxml
 * ✅ Mục đích:
 *  - Quản lý logic giao diện "Quản lý khách hàng" (Customers).
 *  - Thực hiện các thao tác: tìm kiếm, thêm, sửa, xóa, phân trang.
 *  - Tương tác với CustomerService để làm việc với cơ sở dữ liệu.
 *  - Sử dụng AlertUtils để hiển thị thông báo đẹp và thân thiện.
 */

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.example.storemanagement.model.Customer;
import com.example.storemanagement.service.CustomerService;
import com.example.storemanagement.util.AlertUtils;
import com.example.storemanagement.util.DateUtils;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class CustomerController {

    // ====== KHAI BÁO CÁC THÀNH PHẦN TRONG FXML ======
    @FXML private TextField customerSearchField; // ô nhập nội dung tìm kiếm
    @FXML private Button searchCustomerBtn;      // nút tìm kiếm
    @FXML private Button addCustomerBtn;         // nút thêm khách hàng
    @FXML private Button editCustomerBtn;        // nút sửa khách hàng
    @FXML private Button deleteCustomerBtn;      // nút xóa khách hàng
    @FXML private Button refreshCustomersBtn;    // nút làm mới danh sách

    @FXML private TableView<Customer> customersTable; // bảng hiển thị khách hàng
    @FXML private TableColumn<Customer, Long> colId;          // cột ID
    @FXML private TableColumn<Customer, String> colName;      // cột tên
    @FXML private TableColumn<Customer, String> colPhone;     // cột số điện thoại
    @FXML private TableColumn<Customer, String> colEmail;     // cột email
    @FXML private TableColumn<Customer, Integer> colPoints;   // cột điểm tích lũy
    @FXML private TableColumn<Customer, String> colCreatedAt; // cột ngày tạo

    @FXML private Label customerStatusLabel;     // nhãn hiển thị số dòng / dòng chọn
    @FXML private Pagination customerPagination; // điều khiển phân trang

    // ====== LOGIC NGHIỆP VỤ ======
    private final CustomerService customerService = new CustomerService(); // lớp xử lý dữ liệu khách hàng
    private final int pageSize = 20;          // số bản ghi trên mỗi trang
    private int currentPage = 1;              // trang hiện tại
    private String currentKeyword = "";       // từ khóa tìm kiếm hiện tại

    // ====== HÀM KHỞI TẠO GIAO DIỆN ======
    @FXML
    public void initialize() {
        // Liên kết các cột TableView với thuộc tính của lớp Customer
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPoints.setCellValueFactory(new PropertyValueFactory<>("points"));

        // Định dạng ngày tạo (LocalDateTime → String) khi hiển thị
        colCreatedAt.setCellValueFactory(cd -> {
            String s = DateUtils.format(cd.getValue().getCreatedAt());
            return new ReadOnlyStringWrapper(s);
        });

        // Khi người dùng chọn dòng khác ➜ cập nhật trạng thái
        customersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateStatus());

        // Gán hành động cho các nút
        searchCustomerBtn.setOnAction(e -> doSearch(1));              // tìm kiếm mới
        refreshCustomersBtn.setOnAction(e -> doSearch(currentPage));  // tải lại trang hiện tại
        addCustomerBtn.setOnAction(e -> onAdd());                     // thêm
        editCustomerBtn.setOnAction(e -> onEdit());                   // sửa
        deleteCustomerBtn.setOnAction(e -> onDelete());               // xóa

        // Lắng nghe thay đổi trang trong Pagination
        customerPagination.currentPageIndexProperty().addListener((obs, ov, nv) -> {
            currentPage = nv.intValue() + 1; // JavaFX đánh index từ 0 → cộng 1
            doSearch(currentPage);
        });

        // Khi mở form lần đầu, tự động tải trang 1
        doSearch(1);
    }

    /**
     * Hàm tìm kiếm & phân trang khách hàng.
     * @param page Số trang cần hiển thị
     */
    private void doSearch(int page) {
        try {
            // Lấy từ khóa tìm kiếm từ TextField (nếu có)
            currentKeyword = customerSearchField.getText() == null ? "" : customerSearchField.getText().trim();

            // Tính tổng số bản ghi phù hợp
            int total = customerService.count(currentKeyword);

            // Tính tổng số trang dựa trên pageSize
            int pageCount = Math.max(1, (int) Math.ceil(total / (double) pageSize));
            customerPagination.setPageCount(pageCount);       // hiển thị số trang
            currentPage = Math.min(page, pageCount);          // đảm bảo không vượt quá số trang
            customerPagination.setCurrentPageIndex(currentPage - 1);

            // Lấy danh sách khách hàng cho trang hiện tại
            List<Customer> list = customerService.search(currentKeyword, currentPage, pageSize);
            customersTable.getItems().setAll(list);           // hiển thị vào bảng
            updateStatus();                                   // cập nhật thanh trạng thái
        } catch (SQLException ex) {
            AlertUtils.error("Search failed", ex.getMessage()); // báo lỗi nếu truy vấn thất bại
        }
    }

    /**
     * Xử lý khi nhấn nút "Add" ➜ mở hộp thoại thêm khách hàng mới.
     */
    private void onAdd() {
        Dialog<Customer> dlg = buildCustomerDialog(null); // tạo dialog trống
        Optional<Customer> res = dlg.showAndWait();       // hiển thị dialog & chờ kết quả
        res.ifPresent(c -> {                              // nếu người dùng bấm OK
            try {
                long id = customerService.createCustomer(c.getName(), c.getPhone(), c.getEmail());
                AlertUtils.info("Created", "Đã tạo khách hàng #" + id);
                doSearch(1); // tải lại danh sách
            } catch (Exception ex) {
                AlertUtils.error("Create failed", ex.getMessage());
            }
        });
    }

    /**
     * Xử lý khi nhấn "Edit" ➜ mở dialog để sửa khách hàng đang chọn.
     */
    private void onEdit() {
        Customer sel = customersTable.getSelectionModel().getSelectedItem();
        if (sel == null) { // nếu chưa chọn dòng nào
            AlertUtils.warn("No selection", "Chọn một khách hàng để sửa");
            return;
        }

        Dialog<Customer> dlg = buildCustomerDialog(sel); // tạo dialog với dữ liệu có sẵn
        Optional<Customer> res = dlg.showAndWait();      // chờ kết quả
        res.ifPresent(c -> {
            try {
                c.setId(sel.getId());            // giữ nguyên ID cũ
                c.setPoints(sel.getPoints());    // không chỉnh điểm trong dialog này
                customerService.updateCustomer(c);
                AlertUtils.info("Updated", "Đã cập nhật khách hàng #" + sel.getId());
                doSearch(currentPage);           // tải lại trang hiện tại
            } catch (Exception ex) {
                AlertUtils.error("Update failed", ex.getMessage());
            }
        });
    }

    /**
     * Xử lý khi nhấn "Delete" ➜ xác nhận & xóa khách hàng đang chọn.
     */
    private void onDelete() {
        Customer sel = customersTable.getSelectionModel().getSelectedItem();
        if (sel == null) { // chưa chọn dòng nào
            AlertUtils.warn("No selection", "Chọn một khách hàng để xoá");
            return;
        }

        // Hỏi xác nhận người dùng trước khi xóa
        if (!AlertUtils.confirm("Xác nhận", "Xoá khách hàng '" + sel.getName() + "'?")) return;

        try {
            customerService.deleteCustomer(sel.getId());
            doSearch(currentPage); // tải lại bảng sau khi xóa
        } catch (SQLException ex) {
            AlertUtils.error("Delete failed", ex.getMessage());
        }
    }

    /**
     * Tạo hộp thoại nhập thông tin khách hàng (thêm/sửa).
     * @param init  dữ liệu ban đầu (null nếu thêm mới)
     */
    private Dialog<Customer> buildCustomerDialog(Customer init) {
        Dialog<Customer> d = new Dialog<>();
        d.setTitle(init == null ? "Thêm khách hàng" : "Sửa khách hàng");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Tạo các ô nhập dữ liệu
        TextField tfName = new TextField(init != null ? init.getName() : "");
        TextField tfPhone = new TextField(init != null ? (init.getPhone() == null ? "" : init.getPhone()) : "");
        TextField tfEmail = new TextField(init != null ? (init.getEmail() == null ? "" : init.getEmail()) : "");

        // Tạo layout dạng lưới để sắp xếp các nhãn và ô nhập
        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(8);
        gp.addRow(0, new Label("Name"), tfName);
        gp.addRow(1, new Label("Phone"), tfPhone);
        gp.addRow(2, new Label("Email"), tfEmail);
        d.getDialogPane().setContent(gp);

        // Khi người dùng nhấn OK ➜ tạo đối tượng Customer mới với dữ liệu nhập
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Customer c = new Customer();
                c.setName(tfName.getText());
                c.setPhone(tfPhone.getText());
                c.setEmail(tfEmail.getText());
                return c;
            }
            return null; // nếu bấm Cancel thì trả về null
        });
        return d;
    }

    /**
     * Cập nhật nhãn trạng thái (bao nhiêu dòng chọn / tổng số dòng).
     */
    private void updateStatus() {
        int selected = customersTable.getSelectionModel().getSelectedIndices().size(); // số dòng được chọn
        int total = customersTable.getItems().size();                                  // tổng số dòng hiển thị
        customerStatusLabel.setText(selected + " selected • " + total + " item(s)");
    }
}
