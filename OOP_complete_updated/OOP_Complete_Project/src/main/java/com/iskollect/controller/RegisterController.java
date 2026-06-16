package com.iskollect.controller;

import com.iskollect.exception.DatabaseException;
import com.iskollect.exception.InvalidInputException;
import com.iskollect.model.User;
import com.iskollect.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.regex.Pattern;

public class RegisterController {
    private static final Pattern WEBMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{1,63}@iskolarngbayan\\.pup\\.edu\\.ph$");

    @FXML private TextField     nameField;
    @FXML private TextField     webmailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        signUpButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleSignUp() {
        errorLabel.setText("");

        if (nameField.getText().isBlank() || webmailField.getText().isBlank() || passwordField.getText().isBlank()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }
        if (!WEBMAIL_PATTERN.matcher(webmailField.getText().trim()).matches()) {
            errorLabel.setText("Please use a valid PUP webmail address.");
            return;
        }

        if (signUpButton != null) signUpButton.setDisable(true);
        errorLabel.setText("Creating account...");

        String name  = nameField.getText().trim();
        String email = webmailField.getText().trim();
        String pass  = passwordField.getText();

        Thread t = new Thread(() -> {
            try {
                User user = new User(name, email, pass);
                boolean success = authService.register(user);
                Platform.runLater(() -> {
                    if (signUpButton != null) signUpButton.setDisable(false);
                    if (success) goToLogin();
                });
            } catch (InvalidInputException | DatabaseException e) {
                Platform.runLater(() -> {
                    if (signUpButton != null) signUpButton.setDisable(false);
                    errorLabel.setText(e.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void goToLogin() {
        loadScreen("login.fxml");
    }

    private void loadScreen(String fxmlFile) {
        try {
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(com.iskollect.util.SceneCache.getScene("/com/iskollect/fxml/" + fxmlFile));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Navigation error [" + fxmlFile + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
