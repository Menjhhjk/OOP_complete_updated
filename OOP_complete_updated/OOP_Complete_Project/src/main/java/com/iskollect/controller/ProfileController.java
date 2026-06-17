package com.iskollect.controller;

import com.iskollect.dao.UserDAO;
import com.iskollect.dao.BottleRecordDAO;
import com.iskollect.dao.RedemptionDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.User;
import com.iskollect.service.BadgeService;
import com.iskollect.util.PasswordUtil;
import com.iskollect.util.SessionManager;
import com.iskollect.util.ClockUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class ProfileController {

    @FXML private Label webmailLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Label totalBottlesLabel;
    @FXML private Label couponsRedeemedLabel;
    @FXML private Label displayNameLabel;
    @FXML private Label badgeLevelLabel;
    @FXML private Label usernameDisplayLabel;
    @FXML private Label dateTimeLabel;
    @FXML private Label profileStatusLabel;
    @FXML private Label passwordStatusLabel;

    @FXML private TextField    displayNameField;
    @FXML private TextField    ageField;
    @FXML private TextField    usernameField;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private ImageView profilePicture;

    private final UserDAO       userDAO       = new UserDAO();
    private final BottleRecordDAO   bottlerecordDAO   = new BottleRecordDAO();
    private final RedemptionDAO redemptionDAO = new RedemptionDAO();
    private final BadgeService     badgeService     = new BadgeService();
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{2,29}$");
    private static final Path PROFILE_PICTURE_DIR =
            Paths.get("data", "profile_pics").toAbsolutePath().normalize();

    @FXML
    public void initialize() {
        SessionManager.addPointUpdateListener(this::refresh);
        User user = SessionManager.getSession();
        if (user == null) return;

        configureCircularProfilePicture();

        setLabel(webmailLabel,       user.getWebmail());
        setLabel(displayNameLabel,   user.getName());
        setLabel(usernameDisplayLabel, "@" + safeUsername(user.getUsername()));

        if (user.getCreatedAt() != null) {
            setLabel(memberSinceLabel,
                user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        }
        setField(displayNameField, user.getName());
        setField(ageField, String.valueOf(user.getAge()));
        setField(usernameField, user.getUsername());

        loadSavedProfilePicture(user);

        int userId = user.getUserId();
        Thread t = new Thread(() -> {
            String badge;
            int    bottles = 0;
            int    coupons = 0;
            try { badge = badgeService.getCurrentBadge(userId).getTierName(); }
            catch (Exception e) { badge = "N/A"; }
            try { bottles = bottlerecordDAO.getTotalBottles(userId); }
            catch (DatabaseException ignored) {}
            try { coupons = redemptionDAO.getByUserId(userId).size(); }
            catch (DatabaseException ignored) {}

            final String b = badge;
            final int    bo = bottles;
            final int    co = coupons;
            Platform.runLater(() -> {
                setLabel(badgeLevelLabel,      b);
                setLabel(totalBottlesLabel,    String.valueOf(bo));
                setLabel(couponsRedeemedLabel, String.valueOf(co));
            });
        });
        t.setDaemon(true);
        t.start();
        ClockUtil.startClock(dateTimeLabel);
    }

    @FXML
    public void refresh() {
        User user = SessionManager.getSession();
        if (user == null) return;

        int userId = user.getUserId();

        Thread t = new Thread(() -> {
            String badge;
            int bottles = 0;
            int coupons = 0;

            try {
                badge = badgeService.getCurrentBadge(userId).getTierName();
            } catch (Exception e) {
                badge = "N/A";
            }
            try {
                bottles = bottlerecordDAO.getTotalBottles(userId);
            } catch (DatabaseException ignored) {}
            try {
                coupons = redemptionDAO.getByUserId(userId).size();
            } catch (DatabaseException ignored) {}

            final String b = badge;
            final int bo = bottles;
            final int co = coupons;

            Platform.runLater(() -> {
                setLabel(badgeLevelLabel, b);
                setLabel(totalBottlesLabel, String.valueOf(bo));
                setLabel(couponsRedeemedLabel, String.valueOf(co));
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleChangeProfile() {
        User user = SessionManager.getSession();
        if (user == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(displayNameLabel.getScene().getWindow());

        if (selectedFile == null) return; //

        try {
            Path destFolder = PROFILE_PICTURE_DIR;
            Files.createDirectories(destFolder);

            String fileName = "user_" + user.getUserId() + ".png";
            Path destPath = destFolder.resolve(fileName);
            Files.copy(selectedFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

            String savedPath = destPath.toString();
            userDAO.updateProfilePicture(user.getUserId(), savedPath);
            user.setProfilePhoto(savedPath);
            setProfileImage(new Image(destPath.toUri().toString()));

            System.out.println("Profile picture updated to: " + savedPath);

        } catch (Exception e) {
            System.err.println("Error saving profile picture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveChanges() {
        User user = SessionManager.getSession();
        if (user == null) return;

        String newDisplayName = displayNameField != null ? displayNameField.getText().trim() : "";
        String newUsername = usernameField != null ? usernameField.getText().trim() : "";

        clearProfileFeedback();
        if (newDisplayName.isEmpty() || newDisplayName.length() > 50) {
            setProfileStatus("Display name must be 1-50 characters.", true);
            return;
        }
        if (!USERNAME_PATTERN.matcher(newUsername).matches()) {
            setProfileStatus("Username must be 3-30 characters and use only letters, numbers, dots, underscores, or hyphens.", true);
            return;
        }

        int newAge;
        try {
            String ageText = ageField != null ? ageField.getText().trim() : "0";
            newAge = Integer.parseInt(ageText);
            if (newAge < 1 || newAge > 120) {
                setProfileStatus("Please enter a valid age from 1 to 120.", true);
                return;
            }
        } catch (NumberFormatException e) {
            setProfileStatus("Please enter a valid whole number for age.", true);
            return;
        }

        try {
            userDAO.updateProfile(user.getUserId(), newDisplayName, newUsername, newAge);
            user.setName(newDisplayName);
            user.setUsername(newUsername);
            user.setAge(newAge);
            setLabel(displayNameLabel,     newDisplayName);
            setLabel(usernameDisplayLabel, "@" + safeUsername(newUsername));
            setField(displayNameField, newDisplayName);
            setField(usernameField, newUsername);
            setProfileStatus("Profile updated successfully.", false);
        } catch (DatabaseException e) {
            setProfileStatus("Failed to update profile: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleUpdatePassword() {
        User user = SessionManager.getSession();
        if (user == null) return;
        if (currentPasswordField == null || newPasswordField == null || confirmPasswordField == null) return;
        clearPasswordFeedback();

        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();

        if (!PasswordUtil.checkPassword(current, user.getPassword())) {
            currentPasswordField.setStyle("-fx-border-color: red;");
            setPasswordStatus("Current password is incorrect.", true);
            return;
        }
        if (!newPass.equals(confirm)) {
            newPasswordField.setStyle("-fx-border-color: red;");
            confirmPasswordField.setStyle("-fx-border-color: red;");
            setPasswordStatus("New password and confirmation do not match.", true);
            return;
        }
        if (newPass.length() < 8) {
            newPasswordField.setStyle("-fx-border-color: red;");
            setPasswordStatus("New password must be at least 8 characters long.", true);
            return;
        }
        String regex = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};'\"\\\\|,.<>\\/?]).*$";
        if (!newPass.matches(regex)) {
            newPasswordField.setStyle("-fx-border-color: red;");
            setPasswordStatus("New password must include one number and one special character.", true);
            return;
        }
        try {
            String hashed = PasswordUtil.hashPassword(newPass);
            userDAO.updatePasswordHash(user.getUserId(), hashed);
            user.setPassword(hashed);
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            currentPasswordField.setStyle("");
            newPasswordField.setStyle("");
            confirmPasswordField.setStyle("");
            setPasswordStatus("Password updated successfully.", false);
        } catch (DatabaseException e) {
            setPasswordStatus("Password update failed: " + e.getMessage(), true);
        }
    }

    private void setLabel(Label label, String text) { if (label != null) label.setText(text); }
    private void setField(TextField field, String value) {
        if (field != null) field.setText(value != null ? value : "");
    }

    private String safeUsername(String username) {
        return username == null ? "" : username.replaceAll("\\s+", "").toLowerCase();
    }

    private void clearProfileFeedback() {
        if (displayNameField != null) displayNameField.setStyle("");
        if (usernameField != null) usernameField.setStyle("");
        if (ageField != null) ageField.setStyle("");
        setProfileStatus("", false);
    }

    private void clearPasswordFeedback() {
        currentPasswordField.setStyle("");
        newPasswordField.setStyle("");
        confirmPasswordField.setStyle("");
        setPasswordStatus("", false);
    }

    private void setProfileStatus(String message, boolean error) {
        if (profileStatusLabel != null) {
            profileStatusLabel.setText(message);
            profileStatusLabel.setStyle(error ? "-fx-text-fill: #b00020;" : "-fx-text-fill: #1b6b1b;");
        }
    }

    private void setPasswordStatus(String message, boolean error) {
        if (passwordStatusLabel != null) {
            passwordStatusLabel.setText(message);
            passwordStatusLabel.setStyle(error ? "-fx-text-fill: #b00020;" : "-fx-text-fill: #1b6b1b;");
        }
    }

    private void configureCircularProfilePicture() {
        if (profilePicture == null) return;

        double size = Math.min(profilePicture.getFitWidth(), profilePicture.getFitHeight());
        profilePicture.setFitWidth(size);
        profilePicture.setFitHeight(size);
        profilePicture.setPreserveRatio(true);
        profilePicture.setSmooth(true);
        profilePicture.setClip(new Circle(size / 2, size / 2, size / 2));

        Image currentImage = profilePicture.getImage();
        if (currentImage != null) {
            cropProfileImageToCenter(currentImage);
        }
    }

    private void setProfileImage(Image image) {
        if (profilePicture == null || image == null) return;

        profilePicture.setImage(image);
        if (image.getWidth() > 0 && image.getHeight() > 0) {
            cropProfileImageToCenter(image);
        } else {
            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() >= 1.0) {
                    cropProfileImageToCenter(image);
                }
            });
        }
    }

    private void cropProfileImageToCenter(Image image) {
        double width = image.getWidth();
        double height = image.getHeight();
        if (width <= 0 || height <= 0) return;

        double cropSize = Math.min(width, height);
        double x = (width - cropSize) / 2;
        double y = (height - cropSize) / 2;
        profilePicture.setViewport(new Rectangle2D(x, y, cropSize, cropSize));
    }

    private void loadSavedProfilePicture(User user) {
        if (profilePicture == null || user == null) return;

        String photoPath = user.getProfilePhoto();
        if (photoPath == null || photoPath.isBlank()) {
            try {
                User freshUser = userDAO.findById(user.getUserId());
                if (freshUser != null) {
                    photoPath = freshUser.getProfilePhoto();
                    user.setProfilePhoto(photoPath);
                }
            } catch (DatabaseException e) {
                System.err.println("Could not reload profile picture path: " + e.getMessage());
            }
        }

        Path resolvedPhoto = resolveProfilePhotoPath(photoPath);
        if (resolvedPhoto != null && Files.exists(resolvedPhoto)) {
            setProfileImage(new Image(resolvedPhoto.toUri().toString()));
        }
    }

    private Path resolveProfilePhotoPath(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) return null;

        Path rawPath = Paths.get(photoPath);
        if (rawPath.isAbsolute() && Files.exists(rawPath)) {
            return rawPath;
        }

        Path cwdPath = rawPath.toAbsolutePath().normalize();
        if (Files.exists(cwdPath)) {
            return cwdPath;
        }

        Path fileName = rawPath.getFileName();
        if (fileName != null) {
            Path profileDirPath = PROFILE_PICTURE_DIR.resolve(fileName).normalize();
            if (Files.exists(profileDirPath)) {
                return profileDirPath;
            }
        }

        return cwdPath;
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

    // ── Navigation ────────────────────────────────────────────────────────
    @FXML private void goToDashboard()          { loadScreen("dashboard.fxml"); }
    @FXML private void goToBottleRecords()      { loadScreen("bottlerecords.fxml"); }
    @FXML private void goToRewardsCatalog()     { loadScreen("rewardsCatalog.fxml"); }
    @FXML private void goToTransactionHistory() { loadScreen("transactionhistory.fxml"); }

    @FXML
    private void handleLogout() {
        com.iskollect.util.SceneCache.clear();
        try { new com.iskollect.service.AuthService().logout(); }
        catch (com.iskollect.exception.DatabaseException e) {
            System.err.println("Logout error: " + e.getMessage());
        }
        Stage stage = (Stage) webmailLabel.getScene().getWindow();
        com.iskollect.util.RedirectUtil.redirectToLogin(stage);
    }

    private void loadScreen(String fxmlFile) {
        try {
            Stage stage = (Stage) webmailLabel.getScene().getWindow();
            stage.setScene(com.iskollect.util.SceneCache.getScene("/com/iskollect/fxml/" + fxmlFile));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Navigation error [" + fxmlFile + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
