package com.iskollect.controller;

import com.iskollect.model.InOutLog;
import com.iskollect.model.InOutLog.EventType;
import com.iskollect.model.LogResult;
import com.iskollect.service.InOutService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class InOutController {
    @FXML private TextField studentIdField;
    @FXML private Label statusLabel;
    @FXML private TableView<InOutLog> todayLogsTable;

    private final InOutService inOutService = new InOutService();

    @FXML
    public void initialize() {
        refreshTodayLogs();
    }

    @FXML
    public void logIngress() {
        log(EventType.INGRESS);
    }

    @FXML
    public void logEgress() {
        log(EventType.EGRESS);
    }

    private void log(EventType eventType) {
        try {
            int studentId = Integer.parseInt(studentIdField.getText().trim());
            LogResult result = inOutService.logEvent(studentId, eventType, null);
            setStatus(result.getMessage());
            if (result.isSuccess()) {
                refreshTodayLogs();
            }
        } catch (NumberFormatException e) {
            setStatus("Student ID must be a whole number.");
        }
    }

    private void refreshTodayLogs() {
        if (todayLogsTable != null) {
            todayLogsTable.setItems(FXCollections.observableArrayList(inOutService.getTodayLogs()));
        }
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
