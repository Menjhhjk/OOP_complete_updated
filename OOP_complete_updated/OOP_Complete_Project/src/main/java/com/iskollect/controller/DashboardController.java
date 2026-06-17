package com.iskollect.controller;

import com.iskollect.dao.BottleRecordDAO;
import com.iskollect.dao.RedemptionDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.Redemption;
import com.iskollect.model.User;
import com.iskollect.service.BadgeService;
import com.iskollect.service.PointsService;
import com.iskollect.service.StreakService;
import com.iskollect.util.ClockUtil;
import com.iskollect.util.SessionManager;
import java.time.LocalDateTime;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    @FXML private Label nameLabel;
    @FXML private Label pointsLabel;
    @FXML private Label bottlesLabel;
    @FXML private Label badgeLabel;
    @FXML private Label levelLabel;
    @FXML private Label streakLabel;
    @FXML private Label dateTimeLabel;
    @FXML private ImageView badgeImageView;

    private final BottleRecordDAO bottleRecordDAO = new BottleRecordDAO();
    private final PointsService pointsService = new PointsService();
    private final BadgeService  badgeService  = new BadgeService();
    private final StreakService streakService = new StreakService();
    private final RedemptionDAO redemptionDAO = new RedemptionDAO();

    @FXML private TableView<Redemption> transactionTable;
    @FXML private TableColumn<Redemption, Number> colIndex;
    @FXML private TableColumn<Redemption, String>  colDateTime;
    @FXML private TableColumn<Redemption, String>  colCouponType;
    @FXML private TableColumn<Redemption, String>  colPointsUsed;
    @FXML private TableColumn<Redemption, String>  colUniqueCode;
    @FXML private TableColumn<Redemption, String>  colStatus;


    private final ObservableList<Redemption> allData = FXCollections.observableArrayList();
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    @FXML
    public void initialize() {
        SessionManager.addPointUpdateListener(this::refresh);
        setupColumns();
        refresh();
        ClockUtil.startClock(dateTimeLabel);
    }

    private void setupColumns() {
        if (colIndex != null)
            colIndex.setCellValueFactory(cell -> new SimpleIntegerProperty(transactionTable.getItems().indexOf(cell.getValue()) + 1));
        if (colDateTime != null)
            colDateTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRedemptionDate() != null ? cell.getValue().getRedemptionDate().format(DATE_FMT) : ""));
        if (colCouponType != null)
            colCouponType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCouponName()));
        if (colPointsUsed != null)
            colPointsUsed.setCellValueFactory(cell -> new SimpleStringProperty(formatPoints(cell.getValue().getPointsDeducted())));
        if (colUniqueCode != null)
            colUniqueCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCouponCode()));
        if (colStatus != null)
            colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isFulfilled() ? "Fulfilled" : "Pending"));
    }

    private int badgeToLevel(String tierName) {
        switch (tierName) {
            case "Silver":        return 2;
            case "Emerald":       return 3;
            case "Gold":          return 4;
            case "Constellation": return 5;
            default:              return 1; // Bronze
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

    @FXML
    public void refresh() {
        SessionManager.refreshUserSession();
        User user = SessionManager.getSession();
        if (user == null) {
            setText(nameLabel, "No active session");
            return;
        }

        String name = user.getUsername();
        double points = user.getTotalPoints();
        int bottles = user.getRawBottleCount();

        Thread t = new Thread(() -> {
            try {
                List<Redemption> records = redemptionDAO.getByUserId(user.getUserId());
                String badge = badgeService.getCurrentBadge(user.getUserId()).getTierName();
                int streak = streakService.getStreakCount(user.getUserId());
                int level = badgeToLevel(badge);
                String imagePath = badgeToImagePath(badge);

                Platform.runLater(() -> {
                    allData.setAll(records);
                    if (transactionTable != null) {
                        transactionTable.setItems(allData);
                        transactionTable.refresh();
                    }
                    setText(nameLabel, name);
                    setText(bottlesLabel, String.valueOf(bottles));
                    setText(pointsLabel, formatPoints(points));
                    setText(badgeLabel, badge);
                    setText(levelLabel, "Lvl. " + level);
                    setText(streakLabel, String.valueOf(streak));
                    // Update badge image
                    if (badgeImageView != null) {
                        try {
                            Image img = IMAGE_CACHE.computeIfAbsent(imagePath, path -> {
                                try {
                                    return new Image(getClass().getResourceAsStream(path), 135, 129, true, true);
                                } catch (Exception ex) {
                                    System.err.println("Could not load badge image: " + path);
                                    return null;
                                }
                            });
                            if (img != null) badgeImageView.setImage(img);
                        } catch (Exception ex) {
                            System.err.println("Could not load badge image: " + imagePath);
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Error refreshing extra stats: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }

    private String formatPoints(double points) {
        return String.format(Locale.US, "%.2f", points);
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

    @FXML private void goToBottleRecords()      { loadScreen("bottlerecords.fxml"); }
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
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        com.iskollect.util.RedirectUtil.redirectToLogin(stage);
    }

    private void loadScreen(String fxmlFile) {
        try {
            Stage stage = (Stage) nameLabel.getScene().getWindow();
            stage.setScene(com.iskollect.util.SceneCache.getScene("/com/iskollect/fxml/" + fxmlFile));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Navigation error [" + fxmlFile + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
