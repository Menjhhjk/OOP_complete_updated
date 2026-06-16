package com.iskollect.util;

import com.iskollect.exception.NavigationException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class RedirectUtil {

    public static void redirectToLogin(Stage stage) {
        try {
            java.net.URL url = RedirectUtil.class.getResource("/com/iskollect/fxml/login.fxml");
            System.out.println("Checking path: " + url);
            FXMLLoader loader = new FXMLLoader(RedirectUtil.class.getResource("/com/iskollect/fxml/login.fxml"));
            Parent root = loader.load();

            Scene loginScene = new Scene(root);
            stage.setScene(loginScene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            NavigationException error = new NavigationException("Critical error: Could not load the UI.", e);
            AlertUtil.showAlert("Navigation Error", error);
        }
    }
}
