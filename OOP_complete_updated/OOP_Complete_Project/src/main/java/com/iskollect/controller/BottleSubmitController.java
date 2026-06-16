package com.iskollect.controller;

import com.iskollect.model.User;
import com.iskollect.model.SubmitResult;
import com.iskollect.service.BottleService;
import com.iskollect.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.regex.Pattern;

public class BottleSubmitController {
    @FXML private TextField bottleCountField;
    @FXML private Label     statusLabel;

    private final BottleService bottleService = new BottleService();
    private static final Pattern WHOLE_NUMBER_WITH_COMMAS =
            Pattern.compile("\\d{1,3}(,\\d{3})*|\\d+");

    @FXML
    public void submitBottles() {
        User user = SessionManager.getSession();
        if (user == null) {
            setStatus("Please log in first.");
            return;
        }
        String input = bottleCountField.getText() == null ? "" : bottleCountField.getText().trim();
        if (!WHOLE_NUMBER_WITH_COMMAS.matcher(input).matches()) {
            setStatus("Bottle count must be a whole number.");
            return;
        }
        int bottles;
        try {
            long parsed = Long.parseLong(input.replace(",", ""));
            if (parsed > BottleService.MAX_BOTTLES_PER_SUBMISSION) {
                setStatus("Number of bottles submitted exceeds the limit of "
                        + BottleService.MAX_BOTTLES_PER_SUBMISSION + " per submission.");
                return;
            }
            bottles = (int) parsed;
        } catch (NumberFormatException e) {
            setStatus("Number of bottles submitted exceeds the limit of "
                    + BottleService.MAX_BOTTLES_PER_SUBMISSION + " per submission.");
            return;
        }
        if (bottles <= 0) {
            setStatus("Please enter a valid number of bottles.");
            return;
        }

        SubmitResult result = bottleService.submitBottles(user.getUserId(), bottles);

        if (!result.isSuccess()) {
            setStatus(result.getMessage());
            return;
        }

        user.setStreak(result.getCurrentStreak());

        bottleCountField.clear();

        StringBuilder msg = new StringBuilder();
        msg.append("Earned ").append(formatPoints(result.getBasePoints())).append(" base pts");
        if (result.getStreakBonus() > 0) {
            msg.append(" + ").append(formatPoints(result.getStreakBonus())).append(" streak bonus");
        }
        if (result.getBadgeBonus() > 0) {
            msg.append(" + ").append(formatPoints(result.getBadgeBonus())).append(" badge bonus");
        }
        msg.append(" = ").append(formatPoints(result.getTotalPoints())).append(" pts total.");
        msg.append(" Badge: ").append(result.getNewBadgeTier());

        SessionManager.notifyPointUpdate();

        setStatus(msg.toString());
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) bottleCountField.getScene().getWindow();
        stage.close();
    }

    private void setStatus(String message) {
        if (statusLabel != null) statusLabel.setText(message);
    }

    private String formatPoints(double points) {
        return String.format(Locale.US, "%.2f", points);
    }
}
