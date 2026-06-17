package com.iskollect.controller;

import com.iskollect.model.User;
import com.iskollect.service.BadgeService;
import com.iskollect.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BadgeHistoryController {

    @FXML private TableView<BadgeService.BadgeHistoryEntry> badgeTable;
    @FXML private TableColumn<BadgeService.BadgeHistoryEntry, String>  colWeek;
    @FXML private TableColumn<BadgeService.BadgeHistoryEntry, String>  colDateRange;
    @FXML private TableColumn<BadgeService.BadgeHistoryEntry, String>  colBadge;
    @FXML private TableColumn<BadgeService.BadgeHistoryEntry, Integer> colBottles;

    private final BadgeService badgeService = new BadgeService();
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter WEEK_START_FMT = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter WEEK_END_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {

        colWeek.setCellValueFactory(cell -> {
            LocalDate ws = cell.getValue().getWeekStartDate();
            return new SimpleStringProperty(formatWeekRange(ws));
        });
        colDateRange.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDateAwarded().format(DISPLAY_FMT)));
        colBadge.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getBadgeName() + " Badge"));
        colBottles.setCellValueFactory(cell ->
                new SimpleIntegerProperty(cell.getValue().getTotalBottles()).asObject());
    }

    private String formatWeekRange(LocalDate weekStart) {
        if (weekStart == null) return "";
        LocalDate weekEnd = weekStart.plusDays(6);
        return weekStart.format(WEEK_START_FMT) + " - " + weekEnd.format(WEEK_END_FMT);
    }

    private void loadData() {
        User user = SessionManager.getSession();
        if (user == null) return;
        int userId = user.getUserId();

        Thread t = new Thread(() -> {
            List<BadgeService.BadgeHistoryEntry> history = badgeService.getAllBadgeHistory(userId);
            Platform.runLater(() -> {
                if (badgeTable != null) {
                    badgeTable.setItems(FXCollections.observableArrayList(history));
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void closePopup() {
        if (badgeTable != null && badgeTable.getScene() != null) {
            Stage stage = (Stage) badgeTable.getScene().getWindow();
            stage.close();
        }
    }
}
