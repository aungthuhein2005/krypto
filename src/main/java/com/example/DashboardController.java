package com.example;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {
    private static final String[] ALGORITHMS = {"AES-256", "ChaCha20", "RSA-4096"};
    private static final byte[] MAGIC = new byte[]{'S', 'F', 'M', '1'};
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path SETTINGS_PATH = Paths.get(System.getProperty("user.home"), ".secure-file-manager.properties");
    private static final SecureRandom RANDOM = new SecureRandom();

    @FXML
    private BorderPane rootPane;
    @FXML
    private StackPane contentArea;
    @FXML
    private Button dashboardNavButton;
    @FXML
    private Button encryptNavButton;
    @FXML
    private Button decryptNavButton;
    @FXML
    private Button historyNavButton;
    @FXML
    private Button settingsNavButton;

    @FXML
    private ComboBox<String> algorithmComboBox;
    @FXML
    private PasswordField encryptPasswordField;
    @FXML
    private TextField encryptPasswordVisibleField;
    @FXML
    private CheckBox showEncryptPasswordCheck;
    @FXML
    private ProgressBar passwordStrengthBar;
    @FXML
    private Label encryptStrengthLevelLabel;
    @FXML
    private Label encryptStrengthBlocksLabel;
    @FXML
    private ProgressBar encryptionProgressBar;
    @FXML
    private Label encryptProgressLabel;
    @FXML
    private Label encryptStatusLabel;
    @FXML
    private Label encryptTimeLabel;
    @FXML
    private Label encryptFileTypeLabel;
    @FXML
    private Label encryptFileNameLabel;
    @FXML
    private Label encryptFileMetaLabel;
    @FXML
    private Label fileLimitLabel;

    @FXML
    private PasswordField decryptPasswordField;
    @FXML
    private TextField decryptPasswordVisibleField;
    @FXML
    private CheckBox showDecryptPasswordCheck;
    @FXML
    private ProgressBar decryptionProgressBar;
    @FXML
    private Label decryptProgressLabel;
    @FXML
    private Label decryptStatusLabel;
    @FXML
    private Label decryptTimeLabel;
    @FXML
    private Label decryptFileTypeLabel;
    @FXML
    private Label decryptFileNameLabel;
    @FXML
    private Label decryptFileMetaLabel;

    @FXML
    private TableView<HistoryEntry> historyTable;
    @FXML
    private TableColumn<HistoryEntry, String> historyFileNameCol;
    @FXML
    private TableColumn<HistoryEntry, String> historyOperationCol;
    @FXML
    private TableColumn<HistoryEntry, String> historyAlgorithmCol;
    @FXML
    private TableColumn<HistoryEntry, String> historyDateCol;
    @FXML
    private TableColumn<HistoryEntry, String> historyStatusCol;
    @FXML
    private ComboBox<String> historyFilterComboBox;
    @FXML
    private TextField historySearchField;

    @FXML
    private ComboBox<String> defaultAlgorithmComboBox;
    @FXML
    private CheckBox darkModeCheckBox;
    @FXML
    private CheckBox lightModeCheckBox;
    @FXML
    private TextField saveLocationField;
    @FXML
    private CheckBox autoClearClipboardCheckBox;
    @FXML
    private TextField maxFileSizeField;
    @FXML
    private Label generatedKeyLabel;

    @FXML
    private Label totalEncryptedLabel;
    @FXML
    private Label totalDecryptedLabel;
    @FXML
    private Label lastActivityLabel;
    @FXML
    private Label currentAlgorithmLabel;

    private final ObservableList<HistoryEntry> historyItems = FXCollections.observableArrayList();
    private FilteredList<HistoryEntry> filteredHistory;

    private File selectedEncryptFile;
    private File selectedDecryptFile;

    private String saveLocation;
    private String defaultAlgorithm = "AES-256";
    private boolean darkMode = true;
    private long maxFileSizeBytes = 100L * 1024 * 1024;
    private boolean autoClearClipboard;

    private String encryptStatusMessage = "Ready";
    private boolean encryptStatusSuccess;
    private String decryptStatusMessage = "Ready";
    private boolean decryptStatusSuccess;
    private String encryptProgressMessage = "Encrypting... 0%";
    private double encryptProgressValue;
    private String decryptProgressMessage = "Decrypting... 0%";
    private double decryptProgressValue;
    private String encryptTimeMessage = "Encryption completed • Time: --";
    private String decryptTimeMessage = "Decryption completed • Time: --";
    private boolean bootstrapComplete;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (bootstrapComplete) {
            return;
        }
        bootstrapComplete = true;
        loadSettings();
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        setActiveNavButton(dashboardNavButton);
        loadPage("pages/dashboard-page.fxml");
        refreshDashboard();
    }

    @FXML
    private void showEncrypt() {
        setActiveNavButton(encryptNavButton);
        loadPage("pages/encrypt-page.fxml");
        configureEncryptPage();
    }

    @FXML
    private void showDecrypt() {
        setActiveNavButton(decryptNavButton);
        loadPage("pages/decrypt-page.fxml");
        configureDecryptPage();
    }

    @FXML
    private void showHistory() {
        setActiveNavButton(historyNavButton);
        loadPage("pages/history-page.fxml");
        configureHistoryPage();
    }

    @FXML
    private void showSettings() {
        setActiveNavButton(settingsNavButton);
        loadPage("pages/settings-page.fxml");
        configureSettingsPage();
    }

    private void setActiveNavButton(Button activeButton) {
        Button[] buttons = {dashboardNavButton, encryptNavButton, decryptNavButton, historyNavButton, settingsNavButton};
        for (Button button : buttons) {
            if (button == null) {
                continue;
            }
            button.getStyleClass().remove("nav-button-active");
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    private void loadPage(String pagePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/" + pagePath));
            loader.setController(this);
            Parent page = loader.load();
            contentArea.getChildren().setAll(page);
            applyTheme();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureEncryptPage() {
        ObservableList<String> algorithms = FXCollections.observableArrayList(ALGORITHMS);
        algorithmComboBox.setItems(algorithms);
        if (algorithmComboBox.getValue() == null) {
            algorithmComboBox.setValue(defaultAlgorithm);
        }

        encryptPasswordVisibleField.textProperty().bindBidirectional(encryptPasswordField.textProperty());
        showEncryptPasswordCheck.selectedProperty().addListener((obs, oldValue, newValue) ->
                togglePasswordVisibility(encryptPasswordField, encryptPasswordVisibleField, newValue));
        encryptPasswordField.textProperty().addListener((obs, oldValue, newValue) -> updatePasswordStrength(newValue));
        encryptPasswordVisibleField.textProperty().addListener((obs, oldValue, newValue) -> updatePasswordStrength(newValue));

        fileLimitLabel.setText("Maximum file size: " + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        encryptProgressLabel.setText(encryptProgressMessage);
        encryptionProgressBar.setProgress(encryptProgressValue);
        encryptTimeLabel.setText(encryptTimeMessage);
        setEncryptStatus(encryptStatusMessage, encryptStatusSuccess);

        if (selectedEncryptFile != null) {
            selectEncryptFile(selectedEncryptFile);
        } else {
            updatePasswordStrength(encryptPasswordField.getText());
        }
    }

    private void configureDecryptPage() {
        decryptPasswordVisibleField.textProperty().bindBidirectional(decryptPasswordField.textProperty());
        showDecryptPasswordCheck.selectedProperty().addListener((obs, oldValue, newValue) ->
                togglePasswordVisibility(decryptPasswordField, decryptPasswordVisibleField, newValue));

        decryptProgressLabel.setText(decryptProgressMessage);
        decryptionProgressBar.setProgress(decryptProgressValue);
        decryptTimeLabel.setText(decryptTimeMessage);
        setDecryptStatus(decryptStatusMessage, decryptStatusSuccess);

        if (selectedDecryptFile != null) {
            selectDecryptFile(selectedDecryptFile);
        }
    }

    private void configureHistoryPage() {
        historyFileNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFileName()));
        historyOperationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOperation()));
        historyAlgorithmCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAlgorithm()));
        historyDateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate()));
        historyStatusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        if (filteredHistory == null) {
            filteredHistory = new FilteredList<>(historyItems, entry -> true);
        }
        SortedList<HistoryEntry> sorted = new SortedList<>(filteredHistory);
        sorted.comparatorProperty().bind(historyTable.comparatorProperty());
        historyTable.setItems(sorted);

        historyFilterComboBox.setItems(FXCollections.observableArrayList("All", "Encrypt", "Decrypt"));
        if (historyFilterComboBox.getValue() == null) {
            historyFilterComboBox.setValue("All");
        }

        historyFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyHistoryFilters());
        historySearchField.textProperty().addListener((obs, oldValue, newValue) -> applyHistoryFilters());

        applyHistoryFilters();
    }

    private void configureSettingsPage() {
        defaultAlgorithmComboBox.setItems(FXCollections.observableArrayList(ALGORITHMS));
        defaultAlgorithmComboBox.setValue(defaultAlgorithm);

        saveLocationField.setText(saveLocation);
        darkModeCheckBox.setSelected(darkMode);
        lightModeCheckBox.setSelected(!darkMode);
        autoClearClipboardCheckBox.setSelected(autoClearClipboard);
        maxFileSizeField.setText(String.valueOf(maxFileSizeBytes / (1024 * 1024)));

        darkModeCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                lightModeCheckBox.setSelected(false);
            }
        });
        lightModeCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                darkModeCheckBox.setSelected(false);
            }
        });
    }

    @FXML
    private void handleBrowseEncryptFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Encrypt");
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            selectEncryptFile(file);
        }
    }

    @FXML
    private void handleBrowseDecryptFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Encrypted File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Encrypted Files", "*.enc"));
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            selectDecryptFile(file);
        }
    }

    @FXML
    private void handleEncrypt() {
        if (selectedEncryptFile == null) {
            setEncryptStatus("Please choose a file before encryption", false);
            return;
        }
        if (selectedEncryptFile.length() > maxFileSizeBytes) {
            setEncryptStatus("File too large. Limit: " + (maxFileSizeBytes / (1024 * 1024)) + " MB", false);
            return;
        }
        String password = encryptPasswordField.getText();
        if (password == null || password.isEmpty()) {
            setEncryptStatus("Please enter a password", false);
            return;
        }

        String selectedAlgorithm = Objects.requireNonNullElse(algorithmComboBox.getValue(), "AES-256");
        String effectiveAlgorithm = "RSA-4096".equals(selectedAlgorithm) ? "AES-256" : selectedAlgorithm;
        if ("RSA-4096".equals(selectedAlgorithm)) {
            setEncryptStatus("RSA-4096 is optional. Using AES-256 for file encryption.", false);
        }

        Task<CryptoResult> task = new Task<CryptoResult>() {
            @Override
            protected CryptoResult call() throws Exception {
                long start = System.nanoTime();
                Path outputPath = encryptFile(
                        selectedEncryptFile.toPath(),
                        password.toCharArray(),
                        effectiveAlgorithm,
                        (done, total, message) -> {
                            updateProgress(done, total);
                            updateMessage(message);
                        });
                double elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
                return new CryptoResult(outputPath, elapsedSeconds, selectedAlgorithm);
            }
        };

        if (encryptionProgressBar != null) {
            encryptionProgressBar.progressProperty().unbind();
            encryptionProgressBar.progressProperty().bind(task.progressProperty());
        }
        task.messageProperty().addListener((obs, oldValue, newValue) -> {
            encryptProgressMessage = newValue;
            if (encryptProgressLabel != null) {
                encryptProgressLabel.setText(newValue);
            }
        });
        encryptProgressMessage = "Encrypting... 0%";
        if (encryptProgressLabel != null) {
            encryptProgressLabel.setText(encryptProgressMessage);
        }

        task.setOnSucceeded(event -> {
            if (encryptionProgressBar != null) {
                encryptionProgressBar.progressProperty().unbind();
            }
            CryptoResult result = task.getValue();
            encryptTimeMessage = String.format(Locale.US, "Encryption completed • Time: %.2f seconds", result.elapsedSeconds);
            if (encryptTimeLabel != null) {
                encryptTimeLabel.setText(encryptTimeMessage);
            }
            setEncryptStatus("✔ File encrypted successfully! Saved to: " + result.outputPath, true);
            addHistory(selectedEncryptFile.getName(), "Encrypt", result.algorithm, "Success");
            refreshDashboard();
            if (autoClearClipboard) {
                clearClipboard();
            }
        });

        task.setOnFailed(event -> {
            if (encryptionProgressBar != null) {
                encryptionProgressBar.progressProperty().unbind();
            }
            Throwable error = task.getException();
            setEncryptStatus("Encryption failed: " + getRootMessage(error), false);
            addHistory(selectedEncryptFile.getName(), "Encrypt", selectedAlgorithm, "Failed");
            refreshDashboard();
        });

        new Thread(task, "encrypt-task").start();
    }

    @FXML
    private void handleDecrypt() {
        if (selectedDecryptFile == null) {
            setDecryptStatus("Please choose an encrypted .enc file", false);
            return;
        }
        String password = decryptPasswordField.getText();
        if (password == null || password.isEmpty()) {
            setDecryptStatus("Please enter the password used for encryption", false);
            return;
        }

        Task<CryptoResult> task = new Task<CryptoResult>() {
            @Override
            protected CryptoResult call() throws Exception {
                long start = System.nanoTime();
                DecryptionOutput output = decryptFile(
                        selectedDecryptFile.toPath(),
                        password.toCharArray(),
                        (done, total, message) -> {
                            updateProgress(done, total);
                            updateMessage(message);
                        });
                double elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
                return new CryptoResult(output.outputPath, elapsedSeconds, output.algorithm);
            }
        };

        if (decryptionProgressBar != null) {
            decryptionProgressBar.progressProperty().unbind();
            decryptionProgressBar.progressProperty().bind(task.progressProperty());
        }
        task.messageProperty().addListener((obs, oldValue, newValue) -> {
            decryptProgressMessage = newValue;
            if (decryptProgressLabel != null) {
                decryptProgressLabel.setText(newValue);
            }
        });
        decryptProgressMessage = "Decrypting... 0%";
        if (decryptProgressLabel != null) {
            decryptProgressLabel.setText(decryptProgressMessage);
        }

        task.setOnSucceeded(event -> {
            if (decryptionProgressBar != null) {
                decryptionProgressBar.progressProperty().unbind();
            }
            CryptoResult result = task.getValue();
            decryptTimeMessage = String.format(Locale.US, "Decryption completed • Time: %.2f seconds", result.elapsedSeconds);
            if (decryptTimeLabel != null) {
                decryptTimeLabel.setText(decryptTimeMessage);
            }
            setDecryptStatus("✔ File decrypted successfully: " + result.outputPath, true);
            addHistory(selectedDecryptFile.getName(), "Decrypt", result.algorithm, "Success");
            refreshDashboard();
            if (autoClearClipboard) {
                clearClipboard();
            }
        });

        task.setOnFailed(event -> {
            if (decryptionProgressBar != null) {
                decryptionProgressBar.progressProperty().unbind();
            }
            Throwable error = task.getException();
            String rootMessage = getRootMessage(error);
            if (rootMessage.toLowerCase(Locale.ROOT).contains("tag mismatch")) {
                rootMessage = "Incorrect password. Decryption failed";
            }
            setDecryptStatus(rootMessage, false);
            addHistory(selectedDecryptFile.getName(), "Decrypt", "-", "Failed");
            refreshDashboard();
        });

        new Thread(task, "decrypt-task").start();
    }

    @FXML
    private void handleEncryptDragOver(DragEvent event) {
        if (event.getGestureSource() != rootPane && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @FXML
    private void handleEncryptDrop(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;
        if (dragboard.hasFiles()) {
            selectEncryptFile(dragboard.getFiles().get(0));
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void handleDecryptDragOver(DragEvent event) {
        if (event.getGestureSource() != rootPane && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @FXML
    private void handleDecryptDrop(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;
        if (dragboard.hasFiles()) {
            selectDecryptFile(dragboard.getFiles().get(0));
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void handleClearHistory() {
        historyItems.clear();
        refreshDashboard();
        applyHistoryFilters();
    }

    @FXML
    private void handleExportHistory() {
        if (historyItems.isEmpty()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export History");
        chooser.setInitialFileName("history.csv");
        File outFile = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (outFile == null) {
            return;
        }
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
            out.write("File Name,Operation,Algorithm,Date,Status\n".getBytes(StandardCharsets.UTF_8));
            for (HistoryEntry entry : historyItems) {
                String line = escapeCsv(entry.getFileName()) + "," +
                        escapeCsv(entry.getOperation()) + "," +
                        escapeCsv(entry.getAlgorithm()) + "," +
                        escapeCsv(entry.getDate()) + "," +
                        escapeCsv(entry.getStatus()) + "\n";
                out.write(line.getBytes(StandardCharsets.UTF_8));
            }
            setEncryptStatus("History exported: " + outFile.getAbsolutePath(), true);
        } catch (IOException e) {
            setEncryptStatus("Failed to export history: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleChooseSaveLocation() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Save Location");
        File selected = chooser.showDialog(rootPane.getScene().getWindow());
        if (selected != null) {
            saveLocation = selected.getAbsolutePath();
            saveLocationField.setText(saveLocation);
        }
    }

    @FXML
    private void handleGenerateSecureKey() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            key.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        generatedKeyLabel.setText(key.toString());
    }

    @FXML
    private void handleResetSettings() {
        defaultAlgorithmComboBox.setValue("AES-256");
        defaultAlgorithm = "AES-256";
        darkModeCheckBox.setSelected(true);
        lightModeCheckBox.setSelected(false);
        autoClearClipboardCheckBox.setSelected(false);
        maxFileSizeField.setText("100");
        saveLocation = getDefaultSaveDir().toString();
        saveLocationField.setText(saveLocation);
        applyTheme();
        if (fileLimitLabel != null) {
            fileLimitLabel.setText("Maximum file size: 100 MB");
        }
    }

    @FXML
    private void handleSaveSettings() {
        if (!validateAndApplySettingsFromForm()) {
            return;
        }
        saveSettings();
        applyTheme();
    }

    private void selectEncryptFile(File file) {
        selectedEncryptFile = file;
        if (encryptFileTypeLabel != null) {
            encryptFileTypeLabel.setText(fileTypeTag(file.getName()));
        }
        if (encryptFileNameLabel != null) {
            encryptFileNameLabel.setText(file.getName());
        }
        if (encryptFileMetaLabel != null) {
            encryptFileMetaLabel.setText(String.format(Locale.US, "%.2f MB • %s File", file.length() / (1024.0 * 1024.0), fileTypeTag(file.getName())));
        }
        setEncryptStatus("File selected: " + file.getName(), true);
    }

    private void selectDecryptFile(File file) {
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".enc")) {
            setDecryptStatus("Invalid file type. Please select a .enc file", false);
            return;
        }
        selectedDecryptFile = file;
        if (decryptFileTypeLabel != null) {
            decryptFileTypeLabel.setText("ENC");
        }
        if (decryptFileNameLabel != null) {
            decryptFileNameLabel.setText(file.getName());
        }
        if (decryptFileMetaLabel != null) {
            decryptFileMetaLabel.setText(String.format(Locale.US, "%.2f MB • .enc", file.length() / (1024.0 * 1024.0)));
        }
        setDecryptStatus("Encrypted file selected: " + file.getName(), true);
    }

    private void togglePasswordVisibility(PasswordField hiddenField, TextField visibleField, boolean visible) {
        hiddenField.setVisible(!visible);
        hiddenField.setManaged(!visible);
        visibleField.setVisible(visible);
        visibleField.setManaged(visible);
    }

    private void updatePasswordStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;

        if (passwordStrengthBar == null || encryptStrengthLevelLabel == null || encryptStrengthBlocksLabel == null) {
            return;
        }
        passwordStrengthBar.setProgress(score / 5.0);
        if (score <= 2) {
            encryptStrengthLevelLabel.setText("Weak");
            encryptStrengthLevelLabel.getStyleClass().setAll("label-weak");
            encryptStrengthBlocksLabel.setText("██░░░░░░░░");
            passwordStrengthBar.setStyle("-fx-accent: #ff6a6a;");
        } else if (score <= 4) {
            encryptStrengthLevelLabel.setText("Medium");
            encryptStrengthLevelLabel.getStyleClass().setAll("label-medium");
            encryptStrengthBlocksLabel.setText("█████░░░░░");
            passwordStrengthBar.setStyle("-fx-accent: #f6cf58;");
        } else {
            encryptStrengthLevelLabel.setText("Strong");
            encryptStrengthLevelLabel.getStyleClass().setAll("label-strong");
            encryptStrengthBlocksLabel.setText("██████████");
            passwordStrengthBar.setStyle("-fx-accent: #5ee190;");
        }
    }

    private Path encryptFile(Path inputPath, char[] password, String algorithm, ProgressCallback callback) throws Exception {
        Path outputDir = getSaveDirectory();
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve(inputPath.getFileName().toString() + ".enc");

        byte algorithmId = "ChaCha20".equals(algorithm) ? (byte) 2 : (byte) 1;
        String transformation = "ChaCha20".equals(algorithm) ? "ChaCha20-Poly1305" : "AES/GCM/NoPadding";

        byte[] salt = randomBytes(16);
        byte[] iv = randomBytes(12);
        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance(transformation);
        if (algorithmId == 1) {
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        }

        long totalBytes = Files.size(inputPath);
        callback.update(0, Math.max(totalBytes, 1), "Encrypting... 0%");
        byte[] nameBytes = inputPath.getFileName().toString().getBytes(StandardCharsets.UTF_8);

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputPath.toFile())));
             BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputPath.toFile()))) {
            out.write(MAGIC);
            out.writeByte(1);
            out.writeByte(algorithmId);
            out.writeByte(salt.length);
            out.writeByte(iv.length);
            out.writeShort(nameBytes.length);
            out.write(salt);
            out.write(iv);
            out.write(nameBytes);

            processCipher(callback, in, out, cipher, totalBytes, "Encrypting");
        }

        return outputPath;
    }

    private DecryptionOutput decryptFile(Path encryptedFile, char[] password, ProgressCallback callback) throws Exception {
        Path outputDir = getSaveDirectory();
        Files.createDirectories(outputDir);

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(encryptedFile.toFile())))) {
            byte[] magic = new byte[4];
            in.readFully(magic);
            if (!java.util.Arrays.equals(MAGIC, magic)) {
                throw new IllegalArgumentException("Invalid encrypted file format");
            }

            in.readByte(); // version
            byte algorithmId = in.readByte();
            int saltLen = in.readUnsignedByte();
            int ivLen = in.readUnsignedByte();
            int fileNameLen = in.readUnsignedShort();

            byte[] salt = new byte[saltLen];
            byte[] iv = new byte[ivLen];
            byte[] nameBytes = new byte[fileNameLen];
            in.readFully(salt);
            in.readFully(iv);
            in.readFully(nameBytes);

            String originalName = new String(nameBytes, StandardCharsets.UTF_8);
            Path outputPath = resolveUniqueOutput(outputDir.resolve(originalName));

            String algorithm = algorithmId == 2 ? "ChaCha20" : "AES-256";
            String transformation = algorithmId == 2 ? "ChaCha20-Poly1305" : "AES/GCM/NoPadding";

            SecretKey key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance(transformation);
            if (algorithmId == 1) {
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            }

            long total = Math.max(Files.size(encryptedFile) - (4 + 1 + 1 + 1 + 1 + 2 + saltLen + ivLen + fileNameLen), 1);
            callback.update(0, total, "Decrypting... 0%");
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputPath.toFile())))) {
                processCipher(callback, in, out, cipher, total, "Decrypting");
            } catch (Exception e) {
                Files.deleteIfExists(outputPath);
                throw e;
            }

            return new DecryptionOutput(outputPath, algorithm);
        }
    }

    private void processCipher(ProgressCallback callback, InputStream in, DataOutputStream out, Cipher cipher, long totalBytes, String phase)
            throws IOException, GeneralSecurityException {
        byte[] buffer = new byte[8192];
        long processed = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            byte[] update = cipher.update(buffer, 0, read);
            if (update != null) {
                out.write(update);
            }
            processed += read;
            int percent = (int) Math.round((processed * 100.0) / Math.max(totalBytes, 1));
            callback.update(processed, totalBytes, phase + "... " + Math.min(percent, 100) + "%");
        }
        byte[] finalBytes = cipher.doFinal();
        if (finalBytes != null) {
            out.write(finalBytes);
        }
        callback.update(totalBytes, totalBytes, phase + "... 100%");
    }

    private SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] encoded = factory.generateSecret(keySpec).getEncoded();
        return new SecretKeySpec(encoded, "AES");
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private Path resolveUniqueOutput(Path desiredPath) {
        if (!Files.exists(desiredPath)) {
            return desiredPath;
        }
        String fileName = desiredPath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        for (int i = 1; i < 1000; i++) {
            Path candidate = desiredPath.getParent().resolve(base + "_" + i + ext);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        return desiredPath.getParent().resolve(base + "_" + System.currentTimeMillis() + ext);
    }

    private String fileTypeTag(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp")) return "IMG";
        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z")) return "ZIP";
        if (lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt")) return "DOC";
        if (lower.endsWith(".enc")) return "ENC";
        return "FILE";
    }

    private void setEncryptStatus(String message, boolean success) {
        encryptStatusMessage = message;
        encryptStatusSuccess = success;
        if (encryptStatusLabel != null) {
            encryptStatusLabel.setText(message);
            encryptStatusLabel.getStyleClass().removeAll("toast-success", "toast-error", "toast-info");
            encryptStatusLabel.getStyleClass().add(success ? "toast-success" : "toast-error");
        }
    }

    private void setDecryptStatus(String message, boolean success) {
        decryptStatusMessage = message;
        decryptStatusSuccess = success;
        if (decryptStatusLabel != null) {
            decryptStatusLabel.setText(message);
            decryptStatusLabel.getStyleClass().removeAll("toast-success", "toast-error", "toast-info");
            decryptStatusLabel.getStyleClass().add(success ? "toast-success" : "toast-error");
        }
    }

    private void applyHistoryFilters() {
        if (filteredHistory == null || historyFilterComboBox == null || historySearchField == null) {
            return;
        }
        String filter = historyFilterComboBox.getValue();
        String keyword = historySearchField.getText() == null ? "" : historySearchField.getText().toLowerCase(Locale.ROOT);

        filteredHistory.setPredicate(entry -> {
            boolean operationMatches = "All".equals(filter) || entry.getOperation().equalsIgnoreCase(filter);
            boolean textMatches = keyword.isBlank()
                    || entry.getFileName().toLowerCase(Locale.ROOT).contains(keyword)
                    || entry.getDate().toLowerCase(Locale.ROOT).contains(keyword);
            return operationMatches && textMatches;
        });
    }

    private void addHistory(String fileName, String operation, String algorithm, String status) {
        historyItems.add(0, new HistoryEntry(fileName, operation, algorithm, LOG_TIME_FORMAT.format(LocalDateTime.now()), status));
    }

    private void refreshDashboard() {
        long encrypted = historyItems.stream().filter(item -> "Encrypt".equalsIgnoreCase(item.getOperation()) && "Success".equalsIgnoreCase(item.getStatus())).count();
        long decrypted = historyItems.stream().filter(item -> "Decrypt".equalsIgnoreCase(item.getOperation()) && "Success".equalsIgnoreCase(item.getStatus())).count();
        if (totalEncryptedLabel != null) {
            totalEncryptedLabel.setText(String.valueOf(encrypted));
        }
        if (totalDecryptedLabel != null) {
            totalDecryptedLabel.setText(String.valueOf(decrypted));
        }
        if (lastActivityLabel != null) {
            lastActivityLabel.setText(historyItems.isEmpty() ? "No activity yet"
                    : historyItems.get(0).getOperation() + " • " + historyItems.get(0).getFileName() + " • " + historyItems.get(0).getStatus());
        }
        if (currentAlgorithmLabel != null) {
            String current = defaultAlgorithmComboBox == null || defaultAlgorithmComboBox.getValue() == null
                    ? defaultAlgorithm : defaultAlgorithmComboBox.getValue();
            currentAlgorithmLabel.setText(current);
        }
    }

    private void loadSettings() {
        Properties props = new Properties();
        saveLocation = getDefaultSaveDir().toString();
        defaultAlgorithm = "AES-256";
        darkMode = true;
        autoClearClipboard = false;
        maxFileSizeBytes = 100L * 1024 * 1024;

        if (!Files.exists(SETTINGS_PATH)) {
            return;
        }
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(SETTINGS_PATH))) {
            props.load(in);
            String configured = props.getProperty("defaultAlgorithm", "AES-256");
            defaultAlgorithm = configured;
            saveLocation = props.getProperty("saveLocation", saveLocation);
            darkMode = Boolean.parseBoolean(props.getProperty("darkMode", "true"));
            autoClearClipboard = Boolean.parseBoolean(props.getProperty("autoClearClipboard", "false"));
            long maxMb = Long.parseLong(props.getProperty("maxFileSizeMb", "100"));
            maxFileSizeBytes = Math.max(1, maxMb) * 1024 * 1024;
        } catch (Exception ignored) {
            // Defaults will be used when loading fails.
        }
    }

    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("defaultAlgorithm", defaultAlgorithm);
        props.setProperty("saveLocation", saveLocation);
        props.setProperty("darkMode", String.valueOf(darkMode));
        props.setProperty("autoClearClipboard", String.valueOf(autoClearClipboard));
        props.setProperty("maxFileSizeMb", String.valueOf(maxFileSizeBytes / (1024 * 1024)));
        try {
            Files.createDirectories(SETTINGS_PATH.getParent());
            try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(SETTINGS_PATH))) {
                props.store(out, "Secure File Manager Settings");
            }
            setEncryptStatus("Settings saved", true);
        } catch (IOException e) {
            setEncryptStatus("Failed to save settings: " + e.getMessage(), false);
        }
    }

    private boolean validateAndApplySettingsFromForm() {
        String maxMbText = maxFileSizeField.getText();
        long maxMb;
        try {
            maxMb = Long.parseLong(maxMbText);
        } catch (NumberFormatException e) {
            setEncryptStatus("Maximum file size must be a number", false);
            return false;
        }
        if (maxMb < 1) {
            setEncryptStatus("Maximum file size must be at least 1 MB", false);
            return false;
        }

        maxFileSizeBytes = maxMb * 1024 * 1024;
        fileLimitLabel.setText("Maximum file size: " + maxMb + " MB");
        autoClearClipboard = autoClearClipboardCheckBox.isSelected();
        darkMode = darkModeCheckBox.isSelected() || !lightModeCheckBox.isSelected();
        saveLocation = saveLocationField.getText();
        if (saveLocation == null || saveLocation.isBlank()) {
            saveLocation = getDefaultSaveDir().toString();
            saveLocationField.setText(saveLocation);
        }
        if (defaultAlgorithmComboBox.getValue() == null) {
            defaultAlgorithmComboBox.setValue("AES-256");
        }
        defaultAlgorithm = defaultAlgorithmComboBox.getValue();
        if (algorithmComboBox != null) {
            algorithmComboBox.setValue(defaultAlgorithmComboBox.getValue());
        }
        applyTheme();
        refreshDashboard();
        return true;
    }

    private void applyTheme() {
        rootPane.getStyleClass().remove("theme-light");
        if (!darkMode) {
            rootPane.getStyleClass().add("theme-light");
        }
    }

    private Path getSaveDirectory() {
        Path dir = Paths.get(saveLocation == null || saveLocation.isBlank() ? getDefaultSaveDir().toString() : saveLocation);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException ignored) {
                return getDefaultSaveDir();
            }
        }
        return dir;
    }

    private Path getDefaultSaveDir() {
        return Paths.get(System.getProperty("user.home"), "Documents", "SecureFiles");
    }

    private String getRootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "Unknown error" : current.getMessage();
    }

    private String escapeCsv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private void clearClipboard() {
        Platform.runLater(() -> {
            ClipboardContent content = new ClipboardContent();
            content.putString("");
            Clipboard.getSystemClipboard().setContent(content);
        });
    }

    private static class CryptoResult {
        private final Path outputPath;
        private final double elapsedSeconds;
        private final String algorithm;

        private CryptoResult(Path outputPath, double elapsedSeconds, String algorithm) {
            this.outputPath = outputPath;
            this.elapsedSeconds = elapsedSeconds;
            this.algorithm = algorithm;
        }
    }

    @FunctionalInterface
    private interface ProgressCallback {
        void update(long done, long total, String message);
    }

    private static class DecryptionOutput {
        private final Path outputPath;
        private final String algorithm;

        private DecryptionOutput(Path outputPath, String algorithm) {
            this.outputPath = outputPath;
            this.algorithm = algorithm;
        }
    }

    public static class HistoryEntry {
        private final SimpleStringProperty fileName;
        private final SimpleStringProperty operation;
        private final SimpleStringProperty algorithm;
        private final SimpleStringProperty date;
        private final SimpleStringProperty status;

        public HistoryEntry(String fileName, String operation, String algorithm, String date, String status) {
            this.fileName = new SimpleStringProperty(fileName);
            this.operation = new SimpleStringProperty(operation);
            this.algorithm = new SimpleStringProperty(algorithm);
            this.date = new SimpleStringProperty(date);
            this.status = new SimpleStringProperty(status);
        }

        public String getFileName() {
            return fileName.get();
        }

        public String getOperation() {
            return operation.get();
        }

        public String getAlgorithm() {
            return algorithm.get();
        }

        public String getDate() {
            return date.get();
        }

        public String getStatus() {
            return status.get();
        }

    }
}
