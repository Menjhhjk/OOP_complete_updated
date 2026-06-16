package com.iskollect.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReportResult {
    private final String reportType;
    private final LocalDateTime generatedAt;
    private final List<Map<String, Object>> rows;
    private final Map<String, Object> totals;
    private final String errorMessage;
    private final boolean success;

    private ReportResult(String reportType, List<Map<String, Object>> rows,
                         Map<String, Object> totals, String errorMessage, boolean success) {
        this.reportType = reportType;
        this.generatedAt = LocalDateTime.now();
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        this.totals = Collections.unmodifiableMap(new LinkedHashMap<>(totals));
        this.errorMessage = errorMessage;
        this.success = success;
    }

    public static ReportResult success(String type, List<Map<String, Object>> rows,
                                       Map<String, Object> totals) {
        return new ReportResult(type, rows == null ? List.of() : rows,
                totals == null ? Map.of() : totals, null, true);
    }

    public static ReportResult failure(String type, String errorMessage) {
        return new ReportResult(type, List.of(), Map.of(), errorMessage, false);
    }

    public String getReportType() { return reportType; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public List<Map<String, Object>> getRows() { return rows; }
    public Map<String, Object> getTotals() { return totals; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isSuccess() { return success; }
}
