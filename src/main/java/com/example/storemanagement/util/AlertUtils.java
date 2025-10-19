// ==========================
// package: com.example.storemanagement.util
// Files: AlertUtils.java, DateUtils.java, ValidationUtils.java
// ==========================

// ─────────────────────────────────────────────────────────────────────────────
// File: AlertUtils.java
// Mục đích: Hiển thị thông báo JavaFX (info/warn/error/confirm)
// - Tự xử lý chạy trên JavaFX Application Thread (Platform.runLater)
// - Có confirm() dạng blocking (trả về boolean) nhờ CountDownLatch
// ─────────────────────────────────────────────────────────────────────────────
package com.example.storemanagement.util;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class AlertUtils {
    private AlertUtils() {}

    /** Hiển thị thông báo kiểu Information (không chặn UI). */
    public static void info(String title, String message) {
        runOnFxThread(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title == null ? "Information" : title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.show();
        });
    }

    /** Hiển thị cảnh báo (không chặn UI). */
    public static void warn(String title, String message) {
        runOnFxThread(() -> {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle(title == null ? "Warning" : title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.show();
        });
    }

    /** Hiển thị lỗi (không chặn UI). */
    public static void error(String title, String message) {
        runOnFxThread(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title == null ? "Error" : title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.show();
        });
    }

    /** Hộp thoại Xác nhận (OK/Cancel) – blocking và trả về true nếu OK. */
    public static boolean confirm(String title, String message) {
        AtomicBoolean result = new AtomicBoolean(false);
        runOnFxThreadAndWait(() -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle(title == null ? "Confirm" : title);
            a.setHeaderText(null);
            a.setContentText(message);
            Optional<ButtonType> bt = a.showAndWait();
            result.set(bt.isPresent() && bt.get() == ButtonType.OK);
        });
        return result.get();
    }

    // ── Helpers đảm bảo chạy đúng Thread ─────────────────────────────────────
    private static void runOnFxThread(Runnable task) {
        if (Platform.isFxApplicationThread()) task.run();
        else Platform.runLater(task);
    }

    private static void runOnFxThreadAndWait(Runnable task) {
        if (Platform.isFxApplicationThread()) { task.run(); return; }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> { try { task.run(); } finally { latch.countDown(); } });
        try { latch.await(); } catch (InterruptedException ignored) {}
    }
}