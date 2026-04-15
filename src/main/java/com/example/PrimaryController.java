package com.example;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class PrimaryController {

    @FXML
    private ProgressBar passwordStrengthBar;

    @FXML
    private ProgressBar encryptionProgressBar;

    @FXML
    private ComboBox<String> algorithmComboBox;

    @FXML
    private Label statusLabel;

    @FXML
    private void handleDashboard() {
        statusLabel.setText("Dashboard selected");
    }

    @FXML
    private void handleEncryptFile() {
        statusLabel.setText("Encrypt File selected");
    }

    @FXML
    private void handleDecryptFile() {
        statusLabel.setText("Decrypt File selected");
    }

    @FXML
    private void handleHistory() {
        statusLabel.setText("History selected");
    }

    @FXML
    private void handleSettings() {
        statusLabel.setText("Settings selected");
    }

    @FXML
    private void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Encrypt");
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            statusLabel.setText("Selected file: " + file.getName());
        } else {
            statusLabel.setText("No file selected");
        }
    }

    @FXML
    private void handleEncrypt() {
        String selectedAlgorithm = algorithmComboBox.getValue();
        if (selectedAlgorithm == null || selectedAlgorithm.isEmpty()) {
            statusLabel.setText("Please select an encryption algorithm");
            return;
        }

        // Simulate encryption process
        encryptionProgressBar.setProgress(0.65);
        statusLabel.setText("File encrypted successfully!");
    }

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }
}
