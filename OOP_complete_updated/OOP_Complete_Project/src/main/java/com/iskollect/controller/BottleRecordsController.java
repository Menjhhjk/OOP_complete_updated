package com.iskollect.controller;

import com.iskollect.model.User;
import com.iskollect.model.BottleRecord;
import com.iskollect.service.BadgeService;
import com.iskollect.service.BottleService;
import com.iskollect.dao.UserDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.util.ClockUtil;
import com.iskollect.util.SessionManager;
import java.time.LocalDateTime;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BottleRecordsController {

    @FXML private TableView<BottleRecord> submissionTable;
    @FXML private TableColumn<BottleRecord, String>  colDate;
    @FXML private TableColumn<BottleRecord, Integer> colBottles;
    @FXML private TableColumn<BottleRecord, String> colPoints;

    @FXML private Label dateTimeLabel;

    @FXML private Label       bottleCountLabel;
    @FXML private Label       bottlesNeededLabel;
    @FXML private Label       nextBadgeLabel;
    @FXML private ProgressBar badgeProgressBar;
    @FXML private Label       progressPctLabel;

    @FXML private ImageView badgeHistoryImage1;
    @FXML private Label     badgeHistoryName1;
    @FXML private Label     badgeHistoryDate1;
    @FXML private ImageView badgeHistoryImage2;
    @FXML private Label     badgeHistoryName2;
    @FXML private Label     badgeHistoryDate2;
    @FXML private ImageView badgeHistoryImage3;
    @FXML private Label     badgeHistoryName3;
    @FXML private Label     badgeHistoryDate3;

    private final BottleService bottleService = new BottleService();
    private final BadgeService  badgeService  = new BadgeService();
    private final UserDAO       userDAO       = new UserDAO();

    private ObservableList<BottleRecord> allData = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT    = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

    private static final int[]    TIER_THRESHOLDS = { 0, 6, 11, 21, 31 };
    private static final String[] TIER_NAMES      = { "Bronze", "Silver", "Emerald", "Gold", "Constellation" };

    @FXML
    public void initialize() {
        SessionManager.addPointUpdateListener(this::loadDataAsync);
        setupColumns();
        loadDataAsync();
        ClockUtil.startClock(dateTimeLabel);
    }

    private void setupColumns() {
        if (submissionTable == null) return;

        if (colDate != null) {
            colDate.setCellValueFactory(cell ->
                new SimpleStringProperty(
                    cell.getValue().getDate() != null
                        ? cell.getValue().getDate().format(DATE_FMT) : ""));
        }
        if (colBottles != null) {
            colBottles.setCellValueFactory(cell ->
                new SimpleIntegerProperty(cell.getValue().getBottles()).asObject());
        }
        if (colPoints != null) {
            colPoints.setCellValueFactory(cell ->
                new SimpleStringProperty(formatPoints(cell.getValue().getPoints())));
        }
    }

    private String formatPoints(double points) {
        return String.format(Locale.US, "%.2f", points);
    }

    private void loadDataAsync() {
        User user = SessionManager.getSession();
        if (user == null) return;
        int userId = user.getUserId();

        Thread t = new Thread(() -> {
            List<BottleRecord> list = bottleService.getBottleHistory(userId);

            int weeklyBottles = 0;
            try {
                User fresh = userDAO.findById(userId);
                if (fresh != null) weeklyBottles = fresh.getWeeklyBottles();
            } catch (DatabaseException e) {
                System.err.println("Could not fetch weekly bottles: " + e.getMessage());
            }

            List<BadgeService.BadgeHistoryEntry> history = badgeService.getBadgeHistory(userId, 3);

            final int weekly = weeklyBottles;
            final List<BadgeService.BadgeHistoryEntry> bHistory = history;

            Platform.runLater(() -> {
                allData.setAll(list);
                if (submissionTable != null) {
                    submissionTable.setItems(allData);
                }
                updateStatsBanner(weekly);
                updateBadgeHistory(bHistory);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void updateStatsBanner(int weeklyBottles) {

        BadgeService.BadgeResult current = badgeService.evaluateBadge(weeklyBottles);
        String currentTier = current.getTierName();

        int nextThreshold = -1;
        String nextTierName = null;
        for (int i = 0; i < TIER_NAMES.length - 1; i++) {
            if (TIER_NAMES[i].equals(currentTier)) {
                nextThreshold = TIER_THRESHOLDS[i + 1];
                nextTierName  = TIER_NAMES[i + 1];
                break;
            }
        }

        if (bottleCountLabel != null) {
            bottleCountLabel.setText(weeklyBottles + " of bottles");
        }

        if (nextTierName != null) {
            int bottlesNeeded = nextThreshold - weeklyBottles;
            double progress = (double) weeklyBottles / nextThreshold;
            int pct = (int) Math.round(progress * 100);

            if (bottlesNeededLabel != null)
                bottlesNeededLabel.setText(bottlesNeeded + " more bottle(s) needed for next badge level.");
            if (nextBadgeLabel != null)
                nextBadgeLabel.setText("Next Level: " + nextTierName + " Badge");
            if (badgeProgressBar != null)
                badgeProgressBar.setProgress(progress);
            if (progressPctLabel != null)
                progressPctLabel.setText(pct + "%");
        } else {
            if (bottlesNeededLabel != null)
                bottlesNeededLabel.setText("You've reached the highest badge level!");
            if (nextBadgeLabel != null)
                nextBadgeLabel.setText("Current: Constellation Badge ★");
            if (badgeProgressBar != null)
                badgeProgressBar.setProgress(1.0);
            if (progressPctLabel != null)
                progressPctLabel.setText("100%");
        }
    }

    private void updateBadgeHistory(List<BadgeService.BadgeHistoryEntry> history) {
        ImageView[] images = { badgeHistoryImage1, badgeHistoryImage2, badgeHistoryImage3 };
        Label[]    names   = { badgeHistoryName1,  badgeHistoryName2,  badgeHistoryName3  };
        Label[]    dates   = { badgeHistoryDate1,  badgeHistoryDate2,  badgeHistoryDate3  };

        for (int i = 0; i < images.length; i++) {
            if (i < history.size()) {
                BadgeService.BadgeHistoryEntry entry = history.get(i);
                String imgPath = badgeToImagePath(entry.getBadgeName());
                if (images[i] != null) {
                    Image img = IMAGE_CACHE.get(imgPath);
                    if (img == null) {
                        try (java.io.InputStream stream = getClass().getResourceAsStream(imgPath)) {
                            if (stream != null) {
                                img = new Image(stream, 81, 74, true, true);
                                IMAGE_CACHE.put(imgPath, img);
                            } else {
                                System.err.println("Badge image resource not found: " + imgPath);
                            }
                        } catch (Exception ex) {
                            System.err.println("Could not load badge image: " + imgPath + " — " + ex.getMessage());
                        }
                    }
                    if (img != null) images[i].setImage(img);
                    images[i].setVisible(true);
                }
                if (names[i] != null) { names[i].setText(entry.getBadgeName() + " Badge"); names[i].setVisible(true); }
                if (dates[i] != null) { dates[i].setText(entry.getDateAwarded().format(DISPLAY_FMT)); dates[i].setVisible(true); }
            } else {
                if (images[i] != null) images[i].setVisible(false);
                if (names[i]  != null) names[i].setVisible(false);
                if (dates[i]  != null) dates[i].setVisible(false);
            }
        }
    }

    private String badgeToImagePath(String tierName) {
        switch (tierName) {
            case "Bronze":        return "/com/iskollect/assets/7.png";
            case "Silver":        return "/com/iskollect/assets/6.png";
            case "Emerald":       return "/com/iskollect/assets/emerald.png";
            case "Gold":          return "/com/iskollect/assets/5.png";
            case "Constellation": return "/com/iskollect/assets/4.png";
            default:              return "/com/iskollect/assets/7.png";
        }
    }

    // ── Filter buttons ─────────────────────────────────────────────────────
    @FXML private void filterAll() {
        if (submissionTable != null) {
            submissionTable.setItems(allData);
        }
    }

    @FXML private void filterDay()   { filterByDate(LocalDate.now(), LocalDate.now()); }

    @FXML private void filterWeek() {
        LocalDate today = LocalDate.now();
        filterByDate(today.minusDays(today.getDayOfWeek().getValue() - 1L), today);
    }

    @FXML private void filterMonth() {
        LocalDate today = LocalDate.now();
        filterByDate(today.withDayOfMonth(1), today);
    }

    @FXML private void filterYear() {
        LocalDate today = LocalDate.now();
        filterByDate(today.withDayOfYear(1), today);
    }

    private void filterByDate(LocalDate from, LocalDate to) {
        if (submissionTable == null) return;
        List<BottleRecord> filtered = allData.stream()
                .filter(r -> r.getDate() != null
                        && !r.getDate().isBefore(from)
                        && !r.getDate().isAfter(to))
                .collect(Collectors.toList());
        submissionTable.setItems(FXCollections.observableArrayList(filtered));
    }

    // ── Popups ─────────────────────────────────────────────────────────────
    @FXML
    private void openBadgeHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iskollect/fxml/badgehistorypopup.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(root));
            popupStage.setTitle("Badge History");
            popupStage.show();
        } catch (Exception e) {
            System.err.println("Could not open badge history: " + e.getMessage());
            e.printStackTrace();
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

    // ── Navigation ─────────────────────────────────────────────────────────
    @FXML private void goToDashboard()          { loadScreen("dashboard.fxml"); }
    @FXML private void goToRewardsCatalog()     { loadScreen("rewardsCatalog.fxml"); }
    @FXML private void goToTransactionHistory() { loadScreen("transactionhistory.fxml"); }
    @FXML private void goToProfile()            { loadScreen("profile.fxml"); }

    @FXML
    private void handleLogout() {
        com.iskollect.util.SceneCache.clear();
        try { new com.iskollect.service.AuthService().logout(); }
        catch (com.iskollect.exception.DatabaseException e) {
            System.err.println("Logout error: " + e.getMessage());
        }
        Stage stage = (Stage) submissionTable.getScene().getWindow();
        com.iskollect.util.RedirectUtil.redirectToLogin(stage);
    }

    private void loadScreen(String fxmlFile) {
        try {
            Stage stage = (Stage) submissionTable.getScene().getWindow();
            stage.setScene(com.iskollect.util.SceneCache.getScene("/com/iskollect/fxml/" + fxmlFile));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Navigation error [" + fxmlFile + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
