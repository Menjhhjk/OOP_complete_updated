package com.iskollect.controller;

import com.iskollect.model.RedeemResult;
import com.iskollect.model.User;
import com.iskollect.service.BadgeService;
import com.iskollect.service.CouponService;
import com.iskollect.util.ClockUtil;
import com.iskollect.util.SceneCache;
import com.iskollect.util.SessionManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.IOException;

public class RewardsController {

    @FXML private Label  currentPointsLabel;
    @FXML private Label  dateTimeLabel;
    @FXML private Button redeemSuppliesBtn;
    @FXML private Button redeemSnack1Btn;
    @FXML private Button redeemSnack2Btn;
    @FXML private Button redeemLunchBtn;
    @FXML private Label  statusLabel;
    @FXML private ImageView currentBadgeImageView;

    private final CouponService couponService = new CouponService();
    private final BadgeService badgeService = new BadgeService();
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

    private static final int    SUPPLIES_ID   = 1;
    private static final int    SNACK_V1_ID   = 2;
    private static final int    SNACK_V2_ID   = 3;
    private static final int    LUNCH_ID      = 4;
    private static final double SUPPLIES_COST = 10;
    private static final double SNACK_V1_COST = 30;
    private static final double SNACK_V2_COST = 50;
    private static final double LUNCH_COST    = 100;

    @FXML
    public void initialize() {
        SessionManager.addPointUpdateListener(this::refreshPoints);
        refreshPoints();
        ClockUtil.startClock(dateTimeLabel);
    }

    private void setButtonStates(double points) {
        if (redeemSuppliesBtn != null) redeemSuppliesBtn.setDisable(points < SUPPLIES_COST);
        if (redeemSnack1Btn  != null) redeemSnack1Btn.setDisable(points < SNACK_V1_COST);
        if (redeemSnack2Btn  != null) redeemSnack2Btn.setDisable(points < SNACK_V2_COST);
        if (redeemLunchBtn   != null) redeemLunchBtn.setDisable(points < LUNCH_COST);
    }

    @FXML private void handleRedeemSupplies() { redeem(SUPPLIES_ID); }
    @FXML private void handleRedeemSnack1()   { redeem(SNACK_V1_ID); }
    @FXML private void handleRedeemSnack2()   { redeem(SNACK_V2_ID); }
    @FXML private void handleRedeemLunch()    { redeem(LUNCH_ID); }

    private void redeem(int rewardId) {
        User user = SessionManager.getSession();
        if (user == null) { statusLabel.setText("Please log in first."); return; }

        setButtonStates(-1);
        statusLabel.setText("Processing...");

        int userId = user.getUserId();
        Thread t = new Thread(() -> {
            try {
                RedeemResult result = couponService.redeem(userId, rewardId);
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        statusLabel.setText("Success! Coupon code: " + result.getCouponCode());
                        user.setTotalPoints(result.getRemainingPoints());
                        SessionManager.notifyPointUpdate();
                    } else {
                        statusLabel.setText(result.getMessage());
                        refreshPoints();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Redemption failed: " + e.getMessage());
                    refreshPoints();
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void refreshPoints() {
        SessionManager.refreshUserSession();
        User user = SessionManager.getSession();

        if (user == null) return;

        Platform.runLater(() -> {
            double points = user.getTotalPoints();
            currentPointsLabel.setText(formatPoints(points) + " points");
            setButtonStates(points);
            updateCurrentBadgeImage(user.getUserId());
        });
    }

    private void updateCurrentBadgeImage(int userId) {
        if (currentBadgeImageView == null) return;

        String badge = badgeService.getCurrentBadge(userId).getTierName();
        String imagePath = badgeToImagePath(badge);
        try {
            Image img = IMAGE_CACHE.computeIfAbsent(imagePath, path ->
                    new Image(getClass().getResourceAsStream(path), 66, 62, true, true));
            currentBadgeImageView.setImage(img);
        } catch (Exception e) {
            System.err.println("Could not load rewards badge image: " + imagePath);
        }
    }

    private String badgeToImagePath(String tierName) {
        switch (tierName) {
            case "Silver":        return "/com/iskollect/assets/6.png";
            case "Emerald":       return "/com/iskollect/assets/emerald.png";
            case "Gold":          return "/com/iskollect/assets/5.png";
            case "Constellation": return "/com/iskollect/assets/4.png";
            default:              return "/com/iskollect/assets/7.png";
        }
    }

    private String formatPoints(double points) {
        return String.format(Locale.US, "%.2f", points);
    }

    // ── Navigation ────────────────────────────────────────────────────────
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

    @FXML private void goToDashboard()          { loadScreen("dashboard.fxml"); }
    @FXML private void goToBottleRecords()      { loadScreen("bottlerecords.fxml"); }
    @FXML private void goToTransactionHistory() { loadScreen("transactionhistory.fxml"); }
    @FXML private void goToProfile()            { loadScreen("profile.fxml"); }

    @FXML
    private void handleLogout() {
        SceneCache.clear();
        try { new com.iskollect.service.AuthService().logout(); }
        catch (com.iskollect.exception.DatabaseException e) {
            System.err.println("Logout error: " + e.getMessage());
        }
        Stage stage = (Stage) currentPointsLabel.getScene().getWindow();
        com.iskollect.util.RedirectUtil.redirectToLogin(stage);
    }

    private void loadScreen(String fxmlFile) {
        try {
            Stage stage = (Stage) currentPointsLabel.getScene().getWindow();
            stage.setScene(SceneCache.getScene("/com/iskollect/fxml/" + fxmlFile));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Navigation error [" + fxmlFile + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
