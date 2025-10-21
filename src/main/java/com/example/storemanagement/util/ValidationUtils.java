package com.example.storemanagement.util;

// ─────────────────────────────────────────────────────────────────────────────
// File: ValidationUtils.java
// Mục đích: Các hàm kiểm tra dữ liệu nhập phổ biến (email/phone/SKU/giá trị dương...)
// NOTE (fix lỗi `illegal escape character`):
// - Trong literal String của Java, ký tự '\\' dùng để escape. Vì vậy, nếu muốn
//   dùng dấu gạch ngang '-' trong character class của regex, bạn có 2 cách an toàn:
//   (1) Đặt '-' ở đầu HOẶC CUỐI character class để nó là ký tự thường, hoặc
//   (2) Escape bằng \\-, nhưng nhớ phải DOUBLE ESCAPE trong Java literal ⇒ "\\\\-".
//   Bên dưới mình chọn CÁCH (1) cho `PHONE_RE` và `SKU_RE` để code gọn, dễ đọc.
// ─────────────────────────────────────────────────────────────────────────────

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class ValidationUtils {
    private ValidationUtils() {}

    // Regex cơ bản – không quá nghiêm ngặt để thuận tiện cho người dùng VN
    // Dùng \\n thay vì \n để nhất quán và tránh nhầm lẫn khi đọc mã.
    private static final Pattern EMAIL_RE = Pattern.compile("^[^@\\n]+@[^@\\n]+\\.[^@\\n]+$");

    // Đặt '-' ở CUỐI character class để KHÔNG cần escape (tránh "illegal escape character").
    private static final Pattern PHONE_RE = Pattern.compile("^[0-9 +()\\-]{7,20}$");
    // Bạn cũng có thể viết: "^[0-9 +()\\-]{7,20}$" (double-escape) – đều hợp lệ.

    // Với SKU: cho phép chữ, số, dấu chấm, gạch dưới, gạch nối; đặt '-' ở cuối.
    private static final Pattern SKU_RE   = Pattern.compile("^[A-Za-z0-9._-]{3,40}$");

    // ── Chuỗi ────────────────────────────────────────────────────────────────
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String requireNonBlank(String s, String fieldName) {
        if (isBlank(s)) throw new IllegalArgumentException(fieldName + " không được để trống");
        return s.trim();
    }

    // ── Email/Phone/SKU ─────────────────────────────────────────────────────
    public static boolean isEmail(String s) { return !isBlank(s) && EMAIL_RE.matcher(s.trim()).matches(); }
    public static boolean isPhone(String s) { return !isBlank(s) && PHONE_RE.matcher(s.trim()).matches(); }
    public static boolean isSku(String s)   { return !isBlank(s) && SKU_RE.matcher(s.trim()).matches(); }

    // ── Số học ──────────────────────────────────────────────────────────────
    public static int requirePositive(int v, String fieldName) {
        if (v <= 0) throw new IllegalArgumentException(fieldName + " phải > 0");
        return v;
    }

    public static long requirePositive(long v, String fieldName) {
        if (v <= 0) throw new IllegalArgumentException(fieldName + " phải > 0");
        return v;
    }

    public static BigDecimal requirePositive(BigDecimal v, String fieldName) {
        if (v == null || v.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException(fieldName + " phải > 0");
        return v;
    }

    public static int clampNonNegative(int v) { return Math.max(0, v); }

    // ── Phân trang ───────────────────────────────────────────────────────────
    public static int normalizePage(int page) { return page < 1 ? 1 : page; }
    public static int normalizePageSize(int size, int def) { return size <= 0 ? def : size; }
}
