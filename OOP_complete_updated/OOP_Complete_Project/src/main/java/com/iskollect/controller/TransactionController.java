package com.iskollect.controller;

import com.iskollect.dao.RedemptionDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.Redemption;
import com.iskollect.model.User;
import com.iskollect.util.ClockUtil;
import com.iskollect.util.SessionManager;

import java.io.IOException;
import java.time.LocalDateTime;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TransactionController {

    @FXML private Label pointsLabel;
    @FXML private Label totalCouponsLabel;
    @FXML private Label dateTimeLabel;

    @FXML private ComboBox<String> couponTypeFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> fromDateFilter;
    @FXML private ComboBox<String> toDateFilter;

    @FXML private TableView<Redemption>       transactionTable;
    @FXML private TableColumn<Redemption, Number>  colIndex;
    @FXML private TableColumn<Redemption, String>  colDateTime;
    @FXML private TableColumn<Redemption, String>  colCouponType;
    @FXML private TableColumn<Redemption, String>  colPointsUsed;
    @FXML private TableColumn<Redemption, String>  colUniqueCode;
    @FXML private TableColumn<Redemption, String>  colStatus;

    private final RedemptionDAO redemptionDAO = new RedemptionDAO();
    private final ObservableList<Redemption> allData = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    @FXML
    public void initialize() {
        SessionManager.addPointUpdateListener(this::loadDataAsync);
        setupColumns();
        loadDataAsync();
        ClockUtil.startClock(dateTimeLabel);
        if (pointsLabel != null) {
            pointsLabel.setMinWidth(100); // Force a larger minimum width
            pointsLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
    }

    @FXML
    private void openAddBottle() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iskollect/fxml/submitbottlepopup.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(root));
            popupStage.setTitle("Submit Bottles");
            popupStage.show();
        } catch (IOException e) {
            System.err.println("Could not open bottle popup: " + e.getMessage());
        }
    }

    private void setupColumns() {
        if (colIndex != null)
            colIndex.setCellValueFactory(cell ->
                    new SimpleIntegerProperty(transactionTable.getItems().indexOf(cell.getValue()) + 1));
        if (colDateTime != null)
            colDateTime.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getRedemptionDate() != null
                            ? cell.getValue().getRedemptionDate().format(DATE_FMT) : ""));
        if (colCouponType != null)
            colCouponType.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getCouponName() != null
                            ? cell.getValue().getCouponName() : ""));
        if (colPointsUsed != null)
            colPointsUsed.setCellValueFactory(cell ->
                    new SimpleStringProperty(formatPoints(cell.getValue().getPointsDeducted())));
        if (colUniqueCode != null)
            colUniqueCode.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getCouponCode() != null
                            ? cell.getValue().getCouponCode() : ""));
        if (colStatus != null)
            colStatus.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().isFulfilled() ? "Fulfilled" : "Pending"));
    }

    private void loadDataAsync() {
        User user = SessionManager.getSession();
        if (user == null) return;
        int userId = user.getUserId();
        double points = user.getTotalPoints();

        Thread t = new Thread(() -> {
            try {
                List<Redemption> records = redemptionDAO.getByUserId(userId);
                Platform.runLater(() -> {
                    pointsLabel.setText(formatPoints(points));
                    allData.setAll(records);
                    if (transactionTable != null) transactionTable.setItems(allData);
                    if (totalCouponsLabel != null)
                        totalCouponsLabel.setText(String.valueOf(records.size()));
                    setupFilters(records);
                });
            } catch (DatabaseException e) {
                System.err.println("Failed to load transactions: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void setupFilters(List<Redemption> records) {
        if (couponTypeFilter != null) {
            List<String> types = records.stream()
                    .map(Redemption::getCouponName)
                    .filter(t -> t != null && !t.isEmpty())
                    .distinct().sorted().collect(Collectors.toList());
            types.add(0, "All");
            couponTypeFilter.setItems(FXCollections.observableArrayList(types));
            couponTypeFilter.setValue("All");
            couponTypeFilter.setOnAction(e -> applyFilters());
        }
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList("All", "Fulfilled", "Pending"));
            statusFilter.setValue("All");
            statusFilter.setOnAction(e -> applyFilters());
        }
        List<String> dates = records.stream()
                .map(Redemption::getRedemptionDate)
                .filter(d -> d != null)
                .distinct()
                .sorted()
                .map(d -> d.format(DATE_FMT))
                .collect(Collectors.toList());
        dates.add(0, "All");
        if (fromDateFilter != null) {
            fromDateFilter.setItems(FXCollections.observableArrayList(dates));
            fromDateFilter.setValue("All");
            fromDateFilter.setOnAction(e -> applyFilters());
        }
        if (toDateFilter != null) {
            toDateFilter.setItems(FXCollections.observableArrayList(dates));
            toDateFilter.setValue("All");
            toDateFilter.setOnAction(e -> applyFilters());
        }
    }

    @FXML
    private void applyFilters() {
        String type   = safeFilterValue(couponTypeFilter, "All");
        String status = safeFilterValue(statusFilter,     "All");
        String fromText = safeFilterValue(fromDateFilter, "All");
        String toText = safeFilterValue(toDateFilter, "All");
        java.time.LocalDate from = parseDateFilter(fromText);
        java.time.LocalDate to = parseDateFilter(toText);
        List<Redemption> filtered = allData.stream()
                .filter(r -> "All".equals(type) || type.equals(r.getCouponName()))
                .filter(r -> "All".equals(status) ||
                        ("Fulfilled".equals(status) ? r.isFulfilled() : !r.isFulfilled()))
                .filter(r -> from == null || (r.getRedemptionDate() != null && !r.getRedemptionDate().isBefore(from)))
                .filter(r -> to == null || (r.getRedemptionDate() != null && !r.getRedemptionDate().isAfter(to)))
                .collect(Collectors.toList());
        if (transactionTable != null)
            transactionTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private String safeFilterValue(ComboBox<String> comboBox, String defaultValue) {
        if (comboBox == null) {
            return defaultValue;
        }
        String value = comboBox.getValue();
        return value == null ? defaultValue : value;
    }

    @FXML
    private void handleClearFilters() {
        if (couponTypeFilter != null) couponTypeFilter.setValue("All");
        if (statusFilter     != null) statusFilter.setValue("All");
        if (fromDateFilter   != null) fromDateFilter.setValue("All");
        if (toDateFilter     != null) toDateFilter.setValue("All");
        if (transactionTable != null) transactionTable.setItems(allData);
    }

    private java.time.LocalDate parseDateFilter(String text) {
        if (text == null || "All".equals(text)) return null;
        return java.time.LocalDate.parse(text, DATE_FMT);
    }

    private String formatPoints(double points) {
        return String.format(Locale.US, "%.2f", points);
    }

    // ── Navigation ────────────────────────────────────────────────────────
    @FXML private void goToDashboard()          { loadScreen("dashboard.fxml"); }
    @FXML private void goToBottleRecords()      { loadScreen("bottlerecords.fxml"); }
    @FXML private void goToRewardsCatalog()     { loadScreen("rewardsCatalog.fxml"); }
    @FXML private void goToProfile()            { loadScreen("profile.fxml"); }

    @FXML
    private void handleLogout() {
        com.iskollect.util.SceneCache.clear();
        try { new com.iskollect.service.AuthService().logout(); }
        catch (com.iskollect.exception.DatabaseException e) {
            System.err.println("Logout error: " + e.getMessage());
        }
        Stage stage = (Stage) transactionTable.getScene().getWindow();
        com.iskollect.util.RedirectUtil.redirectToLogin(stage);
    }

    private void loadScreen(String fxmlFile) {
        try {
            Stage stage = (Stage) transactionTable.getScene().getWindow();
            stage.setScene(com.iskollect.util.SceneCache.getScene("/com/iskollect/fxml/" + fxmlFile));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Navigation error [" + fxmlFile + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
