package com.iskollect.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

public final class ClockUtil {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy - hh:mma");

    private ClockUtil() {}

    public static void startClock(Label label) {
        if (label == null) return;

        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                label.setText(LocalDateTime.now().format(FORMATTER))));
        clock.setCycleCount(Timeline.INDEFINITE);
        label.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                clock.stop();
            }
        });
        label.setText(LocalDateTime.now().format(FORMATTER));
        clock.play();
    }
}
