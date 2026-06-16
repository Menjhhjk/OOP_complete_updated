package com.iskollect;

import com.iskollect.util.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestDatabaseConnection {
    private static final int SAMPLE_ROW_LIMIT = 5;
    private static final int MAX_CELL_WIDTH = 40;

    private static final List<String> EXPECTED_TABLES = Arrays.asList(
            "badges",
            "bottle_records",
            "coupons",
            "inout_logs",
            "points_ledger",
            "redemptions",
            "streaks",
            "user_badges",
            "users"
    );

    public static void main(String[] args) {
        DBConnection db = null;
        try {
            db = DBConnection.getInstance();
            Connection conn = db.getConnection();

            System.out.println("DATABASE CONNECTION: SUCCESS");
            System.out.println("Connected to: " + maskCredentials(conn.getMetaData().getURL()));
            System.out.println();

            for (String tableName : EXPECTED_TABLES) {
                inspectTable(conn, tableName);
            }
        } catch (Exception e) {
            System.out.println("DATABASE CONNECTION: FAILED");
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.closeConnection();
            }
        }
    }

    private static void inspectTable(Connection conn, String tableName) {
        System.out.println("============================================================");
        System.out.println("TABLE: public." + tableName);

        try {
            if (!tableExists(conn, tableName)) {
                System.out.println("STATUS: NOT FOUND");
                System.out.println();
                return;
            }

            System.out.println("STATUS: CONNECTED");
            printColumns(conn, tableName);
            printSampleRows(conn, tableName);
        } catch (SQLException e) {
            System.out.println("STATUS: FAILED");
            System.out.println("ERROR: " + e.getMessage());
        }

        System.out.println();
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, "public", tableName, new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    private static void printColumns(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT column_name, data_type, is_nullable, column_default "
                + "FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = ? "
                + "ORDER BY ordinal_position";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("COLUMNS:");
                printResultSet(rs);
            }
        }
    }

    private static void printSampleRows(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT * FROM public." + tableName + " LIMIT " + SAMPLE_ROW_LIMIT;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("DATA SAMPLE:");
            printResultSet(rs);
        }
    }

    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        List<String> headers = new ArrayList<>();
        List<String[]> rows = new ArrayList<>();

        for (int i = 1; i <= columnCount; i++) {
            headers.add(meta.getColumnLabel(i));
        }

        while (rs.next()) {
            String[] row = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                row[i - 1] = value == null ? "NULL" : value.toString();
            }
            rows.add(row);
        }

        int[] widths = calculateWidths(headers, rows);
        printSeparator(widths);
        printRow(headers.toArray(new String[0]), widths);
        printSeparator(widths);
        for (String[] row : rows) {
            printRow(row, widths);
        }
        if (rows.isEmpty()) {
            System.out.println("| " + pad("(no rows)", totalInnerWidth(widths)) + " |");
        }
        printSeparator(widths);
    }

    private static int[] calculateWidths(List<String> headers, List<String[]> rows) {
        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = displayValue(headers.get(i)).length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], displayValue(row[i]).length());
            }
        }
        for (int i = 0; i < widths.length; i++) {
            widths[i] = Math.min(widths[i], MAX_CELL_WIDTH);
        }
        return widths;
    }

    private static void printSeparator(int[] widths) {
        StringBuilder line = new StringBuilder("+");
        for (int width : widths) {
            line.append("-".repeat(width + 2)).append("+");
        }
        System.out.println(line);
    }

    private static void printRow(String[] values, int[] widths) {
        StringBuilder line = new StringBuilder("|");
        for (int i = 0; i < values.length; i++) {
            line.append(' ')
                    .append(pad(displayValue(values[i]), widths[i]))
                    .append(" |");
        }
        System.out.println(line);
    }

    private static String displayValue(String value) {
        if (value == null) {
            return "NULL";
        }
        String singleLine = value.replace("\r", "\\r").replace("\n", "\\n");
        if (singleLine.length() <= MAX_CELL_WIDTH) {
            return singleLine;
        }
        return singleLine.substring(0, MAX_CELL_WIDTH - 3) + "...";
    }

    private static String pad(String value, int width) {
        return value + " ".repeat(Math.max(0, width - value.length()));
    }

    private static int totalInnerWidth(int[] widths) {
        int total = 0;
        for (int width : widths) {
            total += width + 3;
        }
        return Math.max(0, total - 1);
    }

    private static String maskCredentials(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("(?i)([?&](?:user|password)=)[^&]+", "$1****");
    }
}
