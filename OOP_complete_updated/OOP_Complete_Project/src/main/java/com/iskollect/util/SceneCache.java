package com.iskollect.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class SceneCache {

    public static Scene getScene(String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(SceneCache.class.getResource(fxmlPath));
        Parent root = loader.load();
        return new Scene(root);
    }

    public static void clear() {
        // Scenes are loaded fresh so old controllers can be garbage-collected.
    }
}
