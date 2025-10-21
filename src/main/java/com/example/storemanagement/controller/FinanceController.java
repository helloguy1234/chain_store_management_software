package com.example.storemanagement.controller;

/*
 * FinanceController – Gắn với finance.fxml
 * Chức năng: Filter theo Store/Date/Type, Pagination, Add/Edit/Delete, Export CSV,
 * tính tổng Income/Expense/Balance.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.storemanagement.dao.DBConnection;
import com.example.storemanagement.dao.FinanceDAO;
import com.example.storemanagement.model.FinanceReport;
import com.example.storemanagement.service.FinanceService;
import com.example.storemanagement.util.AlertUtils;
import com.example.storemanagement.util.DateUtils;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class FinanceController {

    // ====== FXML refs ======
    @FXML private ComboBox<IdName> storeFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<String> typeFilter; // All / INCOME / EXPENSE

    @FXML private Button applyFilterBtn;
    @FXML private Button refreshFinanceBtn;
    @FXML private Button addReportBtn;
    @FXML private Button editReportBtn;
    @FXML private Button deleteReportBtn;
    @FXML private Button exportReportBtn;

    @FXML private TableView<FinanceReport> financeTable;
    @FXML private TableColumn<FinanceReport, Long> colId;
    @FXML private TableColumn<FinanceReport, String> colStore;
    @FXML private TableColumn<FinanceReport, String> colDate;
    @FXML private TableColumn<FinanceReport, String> colType;
    @FXML private TableColumn<FinanceReport, BigDecimal> colAmount;
    @FXML private TableColumn<FinanceReport, String> colNote;

    @FXML private Label totalIncomeLbl;
    @FXML private Label totalExpenseLbl;
    @FXML private Label balanceLbl;
    @FXML private Pagination financePagination;

    // ====== Business ======
    private final FinanceService financeService = new FinanceService();
    private final int pageSize = 20;
    private int currentPage = 1;

    
    @FXML
    public void initialize() {
        // ComboBox chuẩn bị sẵn
        storeFilter.setItems(FXCollections.observableArrayList(loadStores()));
        typeFilter.setItems(FXCollections.observableArrayList("All", "INCOME", "EXPENSE"));
        typeFilter.getSelectionModel().selectFirst();

        // Cột bảng
        colId.setCellValueFactory(new ReadOnlyObjectWrapperFactory<>(FinanceReport::getId));
        colStore.setCellValueFactory(cd -> new ReadOnlyStringWrapper(storeName(cd.getValue().getStoreId())));
        colDate.setCellValueFactory(cd -> new ReadOnlyStringWrapper(DateUtils.format(cd.getValue().getReportDate())));
        colType.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getType() == null ? "" : cd.getValue().getType().name()));
        colAmount.setCellValueFactory(new ReadOnlyObjectWrapperFactory<>(FinanceReport::getAmount));
        colNote.setCellValueFactory(new ReadOnlyStringWrapperFactory<>(FinanceReport::getNote));

        // Events
        applyFilterBtn.setOnAction(e -> doSearch(1));
        refreshFinanceBtn.setOnAction(e -> doSearch(currentPage));
        addReportBtn.setOnAction(e -> onAdd());
        editReportBtn.setOnAction(e -> onEdit());
        deleteReportBtn.setOnAction(e -> onDelete());
        exportReportBtn.setOnAction(e -> onExportCsv());

        financePagination.currentPageIndexProperty().addListener((obs, ov, nv) -> {
            currentPage = nv.intValue() + 1;
            doSearch(currentPage);
        });

        // Load lần đầu
        doSearch(1);
    }

    private void doSearch(int page) {
        try {
            Long storeId = storeFilter.getValue() == null ? null : storeFilter.getValue().id;
            LocalDate from = fromDate.getValue();
            LocalDate to   = toDate.getValue();
            FinanceReport.Type type = null;
            String t = typeFilter.getValue();
            if ("INCOME".equalsIgnoreCase(t)) type = FinanceReport.Type.INCOME; else if ("EXPENSE".equalsIgnoreCase(t)) type = FinanceReport.Type.EXPENSE;

            List<FinanceReport> list = financeService.filter(storeId, from, to, type, page, pageSize);
            financeTable.getItems().setAll(list);

            // Summary
            FinanceDAO.Summary sum = financeService.summarize(storeId, from, to, type);
            totalIncomeLbl.setText("Income: " + sum.income);
            totalExpenseLbl.setText("Expense: " + sum.expense);
            balanceLbl.setText("Balance: " + sum.balance);

            // Ước lượng pageCount
            int pageCount = list.size() < pageSize && page == 1 ? 1 : Math.max(1, page);
            financePagination.setPageCount(pageCount);
            financePagination.setCurrentPageIndex(page - 1);
        } catch (SQLException ex) {
            AlertUtils.error("Load finance failed", ex.getMessage());
        }
    }

    private void onAdd() {
        Dialog<FinanceReport> d = buildFinanceDialog(null);
        d.showAndWait().ifPresent(fr -> {
            try {
                if (fr.getType() == FinanceReport.Type.INCOME)
                    financeService.addIncome(fr.getStoreId(), fr.getReportDate(), fr.getAmount(), fr.getCategory(), fr.getNote());
                else
                    financeService.addExpense(fr.getStoreId(), fr.getReportDate(), fr.getAmount(), fr.getCategory(), fr.getNote());
                doSearch(1);
            } catch (SQLException ex) { AlertUtils.error("Create failed", ex.getMessage()); }
        });
    }

    private void onEdit() {
        FinanceReport sel = financeTable.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtils.warn("No selection", "Chọn một bản ghi để sửa"); return; }
        Dialog<FinanceReport> d = buildFinanceDialog(sel);
        d.showAndWait().ifPresent(fr -> {
            try {
                fr.setId(sel.getId());
                financeService.updateReport(fr);
                doSearch(currentPage);
            } catch (SQLException ex) { AlertUtils.error("Update failed", ex.getMessage()); }
        });
    }

    private void onDelete() {
        FinanceReport sel = financeTable.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtils.warn("No selection", "Chọn một bản ghi để xoá"); return; }
        if (!AlertUtils.confirm("Xác nhận", "Xoá bản ghi #" + sel.getId() + "?")) return;
        try {
            financeService.deleteReport(sel.getId());
            doSearch(currentPage);
        } catch (SQLException ex) { AlertUtils.error("Delete failed", ex.getMessage()); }
    }

    private void onExportCsv() {
        // Xuất CSV của trang hiện tại vào file tạm trong thư mục project
        try {
            File out = new File("finance_export_page" + currentPage + ".csv");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
                bw.write("ID,Store,Date,Type,Amount,Category,Note\n");
                for (FinanceReport r : financeTable.getItems()) {
                    String line = r.getId() + "," + quote(storeName(r.getStoreId())) + "," + DateUtils.format(r.getReportDate()) + "," +
                                  (r.getType()==null?"":r.getType().name()) + "," + r.getAmount() + "," + quote(r.getCategory()) + "," + quote(r.getNote());
                    bw.write(line); bw.write("\n");
                }
            }
            AlertUtils.info("Exported", "Đã xuất: " + out.getAbsolutePath());
        } catch (Exception ex) { AlertUtils.error("Export failed", ex.getMessage()); }
    }

    private String quote(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        return '"' + t + '"';
    }

    private Dialog<FinanceReport> buildFinanceDialog(FinanceReport init) {
        Dialog<FinanceReport> d = new Dialog<>();
        d.setTitle(init == null ? "Thêm báo cáo" : "Sửa báo cáo");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<IdName> cbStore = new ComboBox<>(FXCollections.observableArrayList(loadStores()));
        DatePicker dpDate = new DatePicker(init != null ? init.getReportDate() : LocalDate.now());
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("INCOME", "EXPENSE"));
        TextField tfAmount = new TextField(init != null && init.getAmount()!=null ? init.getAmount().toPlainString() : "0");
        TextField tfCategory = new TextField(init != null ? (init.getCategory()==null?"":init.getCategory()) : "");
        TextArea taNote = new TextArea(init != null ? (init.getNote()==null?"":init.getNote()) : "");
        taNote.setPrefRowCount(3);

        if (init != null) {
            // chọn store hiện tại
            for (IdName s : cbStore.getItems()) if (s.id == init.getStoreId()) { cbStore.getSelectionModel().select(s); break; }
            if (init.getType()!=null) cbType.getSelectionModel().select(init.getType().name()); else cbType.getSelectionModel().selectFirst();
        } else {
            cbStore.getSelectionModel().selectFirst();
            cbType.getSelectionModel().selectFirst();
        }

        GridPane gp = new GridPane(); gp.setHgap(8); gp.setVgap(8);
        gp.addRow(0, new Label("Store"), cbStore);
        gp.addRow(1, new Label("Date"), dpDate);
        gp.addRow(2, new Label("Type"), cbType);
        gp.addRow(3, new Label("Amount"), tfAmount);
        gp.addRow(4, new Label("Category"), tfCategory);
        gp.addRow(5, new Label("Note"), taNote);
        d.getDialogPane().setContent(gp);

        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    FinanceReport r = new FinanceReport();
                    IdName st = cbStore.getValue();
                    r.setStoreId(st == null ? null : st.id);
                    r.setReportDate(dpDate.getValue());
                    r.setType(FinanceReport.Type.valueOf(cbType.getValue()));
                    r.setAmount(new java.math.BigDecimal(tfAmount.getText().trim()));
                    r.setCategory(tfCategory.getText());
                    r.setNote(taNote.getText());
                    return r;
                } catch (Exception ex) {
                    AlertUtils.warn("Giá trị không hợp lệ", "Amount phải là số hợp lệ");
                    return null;
                }
            }
            return null;
        });
        return d;
    }

    // ====== Store helpers ======
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

    private String storeName(Long id) {
        if (id == null) return "";
        String sql = "SELECT name FROM stores WHERE id=?";
        try (Connection cn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString(1); }
        } catch (SQLException ignored) {}
        return "#" + id;
    }

    // Helper: cặp id-tên
    public static class IdName { public final long id; public final String name; public IdName(long id, String name){this.id=id;this.name=name;} @Override public String toString(){return name;} }

    // Factories nhỏ cho ReadOnly wrappers (đỡ phải viết lambda dài)
    private static class ReadOnlyObjectWrapperFactory<T, R> extends javafx.scene.control.cell.PropertyValueFactory<T, R> {
        private final java.util.function.Function<T, R> fn;
        public ReadOnlyObjectWrapperFactory(java.util.function.Function<T, R> fn){ super(""); this.fn = fn; }
        @Override public javafx.beans.value.ObservableValue<R> call(TableColumn.CellDataFeatures<T, R> c){ return new ReadOnlyObjectWrapper<>(fn.apply(c.getValue())); }
    }
    private static class ReadOnlyStringWrapperFactory<T> extends javafx.scene.control.cell.PropertyValueFactory<T, String> {
        private final java.util.function.Function<T, String> fn;
        public ReadOnlyStringWrapperFactory(java.util.function.Function<T, String> fn){ super(""); this.fn = fn; }
        @Override public javafx.beans.value.ObservableValue<String> call(TableColumn.CellDataFeatures<T, String> c){ return new ReadOnlyStringWrapper(fn.apply(c.getValue())); }
    }
}