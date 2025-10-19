<<<<<<< HEAD
# chain_store_management_software
Project for CNPM
=======
# 🏪 PHẦN MỀM QUẢN LÝ CHUỖI CỬA HÀNG (JAVA + MYSQL)

**Tác giả:** Phạm Ngọc Hưng – MSSV: 20235342
**Ngôn ngữ:** Java
**CSDL:** MySQL
**Mục tiêu:** Xây dựng phần mềm quản lý đa chức năng (khách hàng – kho hàng – tài chính) có thể dễ dàng mở rộng, thêm/sửa/xóa chức năng trong tương lai.

---

## 🎯 1. Mô tả hệ thống

Phần mềm hỗ trợ 3 phân hệ chính:

1. **Quản lý khách hàng:**

   * Thêm/sửa/xóa thông tin khách hàng.
   * Ghi nhận đơn hàng, điểm tích lũy.
   * Theo dõi lịch sử mua hàng.

2. **Quản lý tài nguyên (hàng hóa, kho, nguồn cung):**

   * Quản lý số lượng hàng trong từng cửa hàng.
   * Cập nhật khi nhập/xuất hàng.
   * Quản lý danh sách nhà cung cấp.

3. **Quản lý tài chính:**

   * Ghi nhận báo cáo thu/chi của từng cửa hàng.
   * Thêm/sửa/xóa báo cáo tài chính.
   * Tổng hợp chi phí đầu tư cơ sở vật chất.

> Phiên bản hiện tại chưa có đăng nhập (login), chỉ tập trung vào lõi quản lý dữ liệu và giao diện người dùng cơ bản.

---

## 🗂️ 2. Cấu trúc thư mục dự án (chuẩn Maven)

```
StoreManagementSystem/
├── pom.xml                         ← File cấu hình Maven, quản lý thư viện (MySQL Connector, JavaFX,...)
├── README.md                       ← Tài liệu hướng dẫn (file này)
├── src/
│   ├── main/
│   │   ├── java/com/example/storemanagement/
│   │   │   ├── Main.java                   ← Điểm khởi chạy chương trình (JavaFX Application)
│   │   │   ├── controller/                ← Các lớp điều khiển giao diện (Controller layer)
│   │   │   │   ├── CustomerController.java      ← Xử lý logic CRUD khách hàng
│   │   │   │   ├── InventoryController.java     ← Xử lý logic kho hàng, cập nhật tồn kho
│   │   │   │   ├── FinanceController.java       ← Xử lý logic báo cáo tài chính
│   │   │   │   └── MainController.java          ← Giao tiếp giữa giao diện chính và các module
│   │   │   ├── dao/                      ← Data Access Object – truy vấn MySQL
│   │   │   │   ├── DBConnection.java           ← Kết nối MySQL (JDBC)
│   │   │   │   ├── CustomerDAO.java            ← Truy vấn bảng khách hàng
│   │   │   │   ├── ProductDAO.java             ← Truy vấn bảng sản phẩm, kho
│   │   │   │   └── FinanceDAO.java             ← Truy vấn bảng tài chính
│   │   │   ├── model/                    ← Các lớp mô hình (POJO)
│   │   │   │   ├── Customer.java               ← Thông tin khách hàng
│   │   │   │   ├── Product.java                ← Thông tin hàng hóa
│   │   │   │   ├── Supplier.java               ← Thông tin nhà cung cấp
│   │   │   │   ├── FinanceReport.java          ← Báo cáo tài chính
│   │   │   │   └── Store.java                  ← Thông tin từng cửa hàng
│   │   │   ├── service/                   ← Xử lý nghiệp vụ (Business logic)
│   │   │   │   ├── CustomerService.java        ← Kiểm tra, xử lý logic khi thêm/sửa khách hàng
│   │   │   │   ├── InventoryService.java       ← Quản lý nhập xuất hàng
│   │   │   │   └── FinanceService.java         ← Tổng hợp báo cáo thu chi
│   │   │   └── util/                     ← Tiện ích chung
│   │   │       ├── AlertUtils.java             ← Hiển thị thông báo (JavaFX Alert)
│   │   │       ├── DateUtils.java              ← Chuyển đổi định dạng ngày giờ
│   │   │       └── ValidationUtils.java        ← Kiểm tra dữ liệu nhập từ người dùng
│   │   └── resources/
│   │       ├── fxml/                      ← Giao diện JavaFX (XML)
│   │       │   ├── main.fxml                   ← Giao diện chính
│   │       │   ├── customers.fxml              ← Giao diện quản lý khách hàng
│   │       │   ├── inventory.fxml              ← Giao diện kho hàng
│   │       │   └── finance.fxml                ← Giao diện tài chính
│   │       ├── css/                        ← File giao diện (nếu có dùng JavaFX CSS)
│   │       │   └── style.css
│   │       └── database/
│   │           ├── init.sql                    ← Script khởi tạo MySQL (bảng + dữ liệu mẫu)
│   │           └── db.properties               ← File cấu hình kết nối DB (user, password, URL)
│   └── test/java/                      ← Thư mục kiểm thử đơn vị (JUnit)
│       ├── CustomerDAOTest.java
│       ├── ProductDAOTest.java
│       └── FinanceDAOTest.java
└── docs/
    ├── ER_Diagram.png                 ← Sơ đồ quan hệ CSDL
    ├── UseCaseDiagram.png             ← Sơ đồ ca sử dụng
    └── Project_Description.pdf        ← Tài liệu mô tả hệ thống
```

---

## ⚙️ 3. Cài đặt & chạy dự án

### Bước 1: Chuẩn bị môi trường

* Cài **JDK 21** hoặc mới hơn.
* Cài **MySQL Server + MySQL Workbench**.
* Cài **Apache Maven** (để build project).
* Cài **JavaFX SDK** (đặt biến môi trường `PATH_TO_FX`).

### Bước 2: Tạo database

1. Mở MySQL Workbench.
2. Chạy file `src/main/resources/database/init.sql` để tạo database và các bảng mẫu.

### Bước 3: Cấu hình kết nối

Mở file `db.properties` và sửa thông tin:

```properties
url=jdbc:mysql://localhost:3306/store_management
user=root
password=yourpassword
```

### Bước 4: Chạy ứng dụng

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.storemanagement.Main" -f pom.xml
```

> Nếu dùng JavaFX: thêm vào VM options khi chạy:
>
> ```
> --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml
> ```

---

## 🧠 4. Nguyên tắc thiết kế

* Áp dụng **MVC (Model–View–Controller)** tách biệt rõ logic, giao diện và dữ liệu.
* Code chuẩn **DAO + Service layer** để dễ mở rộng.
* Mọi thực thể (Customer, Product, FinanceReport...) đều có CRUD đầy đủ.
* Kết nối DB qua **JDBC**, tách cấu hình khỏi code.
* Dễ dàng thêm module mới (ví dụ sau này thêm LoginController, ReportModule, ...).

---

## 🧩 5. Hướng mở rộng tương lai

* Thêm **module đăng nhập (Login + Role)**.
* Xuất báo cáo **PDF/Excel**.
* Quản lý nhân viên và ca làm.
* Tích hợp API giao hàng hoặc thanh toán.
* Dùng Hibernate hoặc Spring Boot để nâng cấp kiến trúc.

---

📅 **Tác giả:** Phạm Ngọc Hưng – MSSV: 20235342



---
BONUS:
+ dir    (list các file & thư mục ở nơi đứng hiện tại)
+ 
>>>>>>> 346a99f (demo pham ngoc hung)
