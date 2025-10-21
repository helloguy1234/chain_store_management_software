// ─────────────────────────────────────────────────────────────────────────────
// File: DateUtils.java
// Mục đích: Định dạng/parse ngày giờ + chuyển đổi SQL/Java Time
// - Mặc định dùng hệ timezone của máy (ZoneId.systemDefault())
// - Cung cấp formatter phổ biến: yyyy-MM-dd, yyyy-MM-dd HH:mm:ss
// ─────────────────────────────────────────────────────────────────────────────
package com.example.storemanagement.util;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public final class DateUtils {
    private DateUtils() {}

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(DATE_PATTERN);
    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    // ── Format ───────────────────────────────────────────────────────────────
    public static String format(LocalDate d) {
        return d == null ? "" : DATE_FMT.format(d);
    }

    public static String format(LocalDateTime dt) {
        return dt == null ? "" : DATETIME_FMT.format(dt);
    }

    // ── Parse (trả về Optional để tránh ném lỗi) ─────────────────────────────
    public static Optional<LocalDate> parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return Optional.empty();
        try { return Optional.of(LocalDate.parse(s.trim(), DATE_FMT)); }
        catch (DateTimeParseException e) { return Optional.empty(); }
    }

    public static Optional<LocalDateTime> parseDateTime(String s) {
        if (s == null || s.trim().isEmpty()) return Optional.empty();
        try { return Optional.of(LocalDateTime.parse(s.trim(), DATETIME_FMT)); }
        catch (DateTimeParseException e) { return Optional.empty(); }
    }

    // ── SQL <-> Java Time ────────────────────────────────────────────────────
    public static java.sql.Date toSqlDate(LocalDate d) {
        return d == null ? null : java.sql.Date.valueOf(d);
    }

    public static LocalDate fromSqlDate(java.sql.Date d) {
        return d == null ? null : d.toLocalDate();
    }

    public static Timestamp toSqlTimestamp(LocalDateTime dt) {
        return dt == null ? null : Timestamp.valueOf(dt);
    }

    public static LocalDateTime fromSqlTimestamp(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    // ── Thời điểm hiện tại ───────────────────────────────────────────────────
    public static LocalDate today() { return LocalDate.now(); }
    public static LocalDateTime now() { return LocalDateTime.now(); }

    // ── Humanize đơn giản ("x phút trước"/"x giờ trước") ───────────────────
    public static String humanizeSince(LocalDateTime past) {
        if (past == null) return "";
        Duration d = Duration.between(past, LocalDateTime.now());
        long seconds = Math.max(0, d.getSeconds());
        if (seconds < 60) return seconds + " giây trước";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " phút trước";
        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ trước";
        long days = hours / 24;
        return days + " ngày trước";
    }
}