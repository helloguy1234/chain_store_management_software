package com.example.storemanagement.controller;

/*
 * InventoryController – Gắn với inventory.fxml
 * Quản lý tồn kho, sản phẩm, nhập/xuất, và nhà cung cấp.
 * - Có thể lọc theo Store / Supplier / Keyword
 * - Thêm sản phẩm mới có chọn Supplier
 * - Khi thêm sản phẩm → tự tạo inventory = 0 cho tất cả store
 */

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.example.storemanagement.dao.DBConnection;
import com.example.storemanagement.dao.ProductDAO;
import com.example.storemanagement.model.Product;
import com.example.storemanagement.service.InventoryService;
import com.example.storemanagement.util.AlertUtils;
import com.example.storemanagement.util.DateUtils;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;

public class InventoryController {

    // ====== FXML references ======
    @FXML private ComboBox<IdName> storeFilter;
    @FXML private ComboBox<IdName> supplierFilter;
    @FXML private TextField productSearchField;
    @FXML private Button searchProductBtn;
    @FXML private Button addProductBtn;
    @FXML private Button editProductBtn;
    @FXML private Button deleteProductBtn;
    @FXML private Button importStockBtn;
    @FXML private Button exportStockBtn;
    @FXML private Button refreshInventoryBtn;

    @FXML private TableView<ProductDAO.InventoryOverview> inventoryTable;
    @FXML private TableColumn<ProductDAO.InventoryOverview, Long> colId;
    @FXML private TableColumn<ProductDAO.InventoryOverview, String> colName;
    @FXML private TableColumn<ProductDAO.InventoryOverview, String> colSku;
    @FXML private TableColumn<ProductDAO.InventoryOverview, String> colStore;
    @FXML private TableColumn<ProductDAO.InventoryOverview, String> colSupplier;
    @FXML private TableColumn<ProductDAO.InventoryOverview, Integer> colStock;
    @FXML private TableColumn<ProductDAO.InventoryOverview, BigDecimal> colUnitPrice;
    @FXML private TableColumn<ProductDAO.InventoryOverview, String> colUpdatedAt;

    @FXML private Label inventoryStatusLabel;
    @FXML private Pagination inventoryPagination;

    // ====== Business ======
    private final InventoryService inventoryService = new InventoryService();
    private final int pageSize = 20;
    private int currentPage = 1;
    private String currentKeyword = "";
    private Long currentStoreId = null;
    private Long currentSupplierId = null;

    // ====== Initialize ======
    @FXML
    public void initialize() {
        // Load danh sách stores / suppliers
        storeFilter.setItems(FXCollections.observableArrayList(loadStores()));
        supplierFilter.setItems(FXCollections.observableArrayList(loadSuppliers()));

        // Cột bảng
        colId.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().productId));
        colName.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().productName));
        colSku.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().sku));
        colStore.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().storeName));
        colSupplier.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().supplierName != null ? cd.getValue().supplierName : "(N/A)"
        ));
        colStock.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().quantity));
        colUnitPrice.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().unitPrice));
        colUpdatedAt.setCellValueFactory(cd -> new ReadOnlyStringWrapper(DateUtils.format(cd.getValue().updatedAt)));

        // Event filters
        storeFilter.valueProperty().addListener((obs, old, val) -> {
            currentStoreId = (val == null ? null : val.id);
            doSearch(1);
        });
        supplierFilter.valueProperty().addListener((obs, old, val) -> {
            currentSupplierId = (val == null ? null : val.id);
            doSearch(1);
        });

        // Event buttons
        searchProductBtn.setOnAction(e -> doSearch(1));
        refreshInventoryBtn.setOnAction(e -> doSearch(currentPage));
        addProductBtn.setOnAction(e -> onAddProduct());
        editProductBtn.setOnAction(e -> onEditProduct());
        deleteProductBtn.setOnAction(e -> onDeleteProduct());
        importStockBtn.setOnAction(e -> onMoveStock(true));
        exportStockBtn.setOnAction(e -> onMoveStock(false));

        inventoryPagination.currentPageIndexProperty().addListener((obs, ov, nv) -> {
            currentPage = nv.intValue() + 1;
            doSearch(currentPage);
        });

        doSearch(1);
    }

    // ====== Main search method ======
    private void doSearch(int page) {
        try {
            currentKeyword = productSearchField.getText() == null ? "" : productSearchField.getText().trim();
            List<ProductDAO.InventoryOverview> list = inventoryService.getInventoryOverview(
                    currentStoreId, currentSupplierId, currentKeyword, page, pageSize);

            int pageCount = list.size() < pageSize && page == 1 ? 1 : Math.max(1, page);
            inventoryPagination.setPageCount(pageCount);
            inventoryPagination.setCurrentPageIndex(page - 1);

            inventoryTable.getItems().setAll(list);
            updateStatus();
        } catch (SQLException ex) {
            AlertUtils.error("Load inventory failed", ex.getMessage());
        }
    }

    // ====== Add product ======
    private void onAddProduct() {
        Dialog<Product> d = buildProductDialog(null);
        d.showAndWait().ifPresent(p -> {
            try {
                long id = inventoryService.addProduct(p);
                AlertUtils.info("Created", "Đã tạo sản phẩm #" + id);
                doSearch(1);
            } catch (SQLException ex) {
                AlertUtils.error("Create failed", ex.getMessage());
            }
        });
    }

    // ====== Edit product ======
    private void onEditProduct() {
        ProductDAO.InventoryOverview sel = inventoryTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("No selection", "Chọn một dòng để sửa sản phẩm");
            return;
        }
        try {
            Product p = inventoryService.findProductById(sel.productId).orElse(null);
            if (p == null) {
                AlertUtils.warn("Not found", "Không tìm thấy sản phẩm");
                return;
            }
            Dialog<Product> d = buildProductDialog(p);
            d.showAndWait().ifPresent(updated -> {
                try {
                    updated.setId(p.getId());
                    inventoryService.updateProduct(updated);
                    doSearch(currentPage);
                } catch (SQLException ex) {
                    AlertUtils.error("Update failed", ex.getMessage());
                }
            });
        } catch (SQLException ex) {
            AlertUtils.error("Load product failed", ex.getMessage());
        }
    }

    // ====== Delete product ======
    private void onDeleteProduct() {
        ProductDAO.InventoryOverview sel = inventoryTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("No selection", "Chọn một sản phẩm để xoá");
            return;
        }
        if (!AlertUtils.confirm("Xác nhận", "Xoá sản phẩm #" + sel.productId + "?")) return;
        try {
            inventoryService.deleteProduct(sel.productId);
            doSearch(currentPage);
        } catch (SQLException ex) {
            AlertUtils.error("Delete failed", ex.getMessage());
        }
    }

    // ====== Import / Export stock ======
    private void onMoveStock(boolean isImport) {
        ProductDAO.InventoryOverview sel = inventoryTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("No selection", "Chọn một dòng kho để " + (isImport ? "nhập" : "xuất"));
            return;
        }
        TextInputDialog d = new TextInputDialog("1");
        d.setTitle(isImport ? "Import Stock" : "Export Stock");
        d.setHeaderText("Số lượng");
        d.setContentText("Quantity:");
        d.showAndWait().ifPresent(s -> {
            try {
                int qty = Integer.parseInt(s.trim());
                if (qty <= 0) throw new NumberFormatException();
                if (isImport)
                    inventoryService.importStock(sel.storeId, sel.productId, qty, "Manual import");
                else
                    inventoryService.exportStock(sel.storeId, sel.productId, qty, "Manual export");
                doSearch(currentPage);
            } catch (NumberFormatException ne) {
                AlertUtils.warn("Giá trị không hợp lệ", "Quantity phải là số nguyên dương");
            } catch (SQLException ex) {
                AlertUtils.error("Stock move failed", ex.getMessage());
            }
        });
    }

    // ====== Product dialog with Supplier ======
    private Dialog<Product> buildProductDialog(Product init) {
        Dialog<Product> d = new Dialog<>();
        d.setTitle(init == null ? "Thêm sản phẩm" : "Sửa sản phẩm");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<IdName> cbSupplier = new ComboBox<>();
        cbSupplier.getItems().setAll(loadSuppliers());
        if (init != null && init.getSupplierId() != null) {
            cbSupplier.getSelectionModel().select(
                    cbSupplier.getItems().stream()
                            .filter(i -> i.id.equals(init.getSupplierId())).findFirst().orElse(null)
            );
        }

        TextField tfSku = new TextField(init != null ? init.getSku() : "");
        TextField tfName = new TextField(init != null ? init.getName() : "");
        TextField tfUnit = new TextField(init != null ? init.getUnit() : "pcs");
        TextField tfPrice = new TextField(init != null && init.getUnitPrice()!=null? init.getUnitPrice().toPlainString() : "0");
        TextField tfCost  = new TextField(init != null && init.getUnitCost()!=null ? init.getUnitCost().toPlainString() : "0");
        CheckBox cbActive = new CheckBox("Active");
        cbActive.setSelected(init == null || init.isActive());

        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(8);
        gp.addRow(0, new Label("Supplier"), cbSupplier);
        gp.addRow(1, new Label("SKU"), tfSku);
        gp.addRow(2, new Label("Name"), tfName);
        gp.addRow(3, new Label("Unit"), tfUnit);
        gp.addRow(4, new Label("Unit Price"), tfPrice);
        gp.addRow(5, new Label("Unit Cost"), tfCost);
        gp.addRow(6, new Label("Status"), cbActive);
        d.getDialogPane().setContent(gp);

        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    Product p = new Product();
                    IdName sup = cbSupplier.getValue();
                    p.setSupplierId(sup != null ? sup.id : null);
                    p.setSku(tfSku.getText());
                    p.setName(tfName.getText());
                    p.setUnit(tfUnit.getText());
                    p.setUnitPrice(new BigDecimal(tfPrice.getText().trim()));
                    p.setUnitCost(new BigDecimal(tfCost.getText().trim()));
                    p.setActive(cbActive.isSelected());
                    return p;
                } catch (Exception ex) {
                    AlertUtils.warn("Giá trị không hợp lệ", "Giá/Cost phải là số hợp lệ");
                    return null;
                }
            }
            return null;
        });
        return d;
    }

    // ====== Helpers ======
    private void updateStatus() {
        int selected = inventoryTable.getSelectionModel().getSelectedIndices().size();
        int total = inventoryTable.getItems().size();
        inventoryStatusLabel.setText(total + " item(s) • " + selected + " selected");
    }

    private List<IdName> loadStores() {
        List<IdName> list = new ArrayList<>();
        String sql = "SELECT id, name FROM stores ORDER BY name";
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new IdName(rs.getLong(1), rs.getString(2)));
        } catch (SQLException ignored) {}
        return list;
    }

    private List<IdName> loadSuppliers() {
        List<IdName> list = new ArrayList<>();
        String sql = "SELECT id, name FROM suppliers ORDER BY name";
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new IdName(rs.getLong(1), rs.getString(2)));
        } catch (SQLException ignored) {}
        return list;
    }

    // Helper nhỏ (id + tên)
    public static class IdName {
        public final Long id;
        public final String name;
        public IdName(Long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name + " (#" + id + ")"; }
    }
}
