package com.iskollect.controller;

import com.iskollect.exception.DatabaseException;
import com.iskollect.exception.InvalidInputException;
import com.iskollect.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import java.util.Optional;
import java.util.regex.Pattern;

public class LoginController {
    private static final Pattern WEBMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{1,63}@iskolarngbayan\\.pup\\.edu\\.ph$");

    @FXML private TextField     webmailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setText("");
        String email = webmailField.getText();
        String pass  = passwordField.getText();

        if (email.isBlank() || pass.isBlank()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }
        if (!WEBMAIL_PATTERN.matcher(email.trim()).matches()) {
            errorLabel.setText("Please use a valid PUP webmail address.");
            return;
        }

        if (loginButton != null) loginButton.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                boolean success = authService.login(email, pass);
                Platform.runLater(() -> {
                    if (loginButton != null) loginButton.setDisable(false);
                    if (success) {
                        loadScreen("dashboard.fxml");
                    } else {
                        errorLabel.setText("Incorrect webmail or password.");
                    }
                });
            } catch (InvalidInputException | DatabaseException e) {
                Platform.runLater(() -> {
                    if (loginButton != null) loginButton.setDisable(false);
                    errorLabel.setText(e.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void goToRegister() {
        loadScreen("signup.fxml");
    }

    @FXML
    private void goToForgotPassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Password reset request");
        dialog.setContentText("Enter your PUP webmail:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String email = result.get().trim();
        if (!WEBMAIL_PATTERN.matcher(email).matches()) {
            errorLabel.setText("Please use a valid PUP webmail address.");
            return;
        }

        errorLabel.setStyle("-fx-text-fill: #1b6b1b;");
        errorLabel.setText("Password reset request received for " + email + ". Please contact support for the verification code.");
    }

    private void loadScreen(String fxmlFile) {
        try {
            Stage stage = (Stage) webmailField.getScene().getWindow();
            stage.setScene(com.iskollect.util.SceneCache.getScene("/com/iskollect/fxml/" + fxmlFile));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Navigation error [" + fxmlFile + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
