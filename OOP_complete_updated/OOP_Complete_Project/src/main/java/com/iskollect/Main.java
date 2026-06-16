package com.iskollect;

import com.iskollect.util.RedirectUtil;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Iskollect");
        RedirectUtil.redirectToLogin(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
