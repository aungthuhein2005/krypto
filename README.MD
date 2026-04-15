# Krypto

Krypto is a JavaFX desktop app for secure file encryption and decryption.
It provides a modern UI with dashboard stats, encryption history, and configurable settings.

## Features

- Encrypt files using `AES-256` or `ChaCha20` (stored as `.enc`).
- Decrypt `.enc` files back to original format.
- Drag-and-drop or browse file selection.
- Password strength indicator and progress tracking.
- Operation history with search, filter, and CSV export.
- Settings for default algorithm, theme, save location, max file size, and clipboard auto-clear.

## Tech Stack

- Java 11+ (project is compiled with release 11)
- JavaFX (`javafx-controls`, `javafx-fxml`)
- Maven

## Project Structure

- `src/main/java/com/example/App.java` - JavaFX application entry
- `src/main/java/com/example/Launcher.java` - launcher main class for packaged app
- `src/main/java/com/example/DashboardController.java` - main app logic (encrypt/decrypt/settings/history)
- `src/main/resources/com/example/` - FXML pages and styles
- `pom.xml` - Maven build config

## Run Locally (Development)

From project root:

```powershell
mvn clean javafx:run
```

## Build JAR

```powershell
mvn clean package
```

Output JAR:

- `target/demo-1.0-SNAPSHOT.jar`

## Build Windows Installer (.exe)

### Prerequisites

- JDK with `jpackage` available
- WiX Toolset (required by `jpackage --type exe`)

Install WiX via winget:

```powershell
winget install --id WiXToolset.WiXToolset -e
```

### Package command

```powershell
jpackage --type exe --input target --dest installer --main-jar demo-1.0-SNAPSHOT.jar --main-class com.example.Launcher --name "Krypto" --app-version 1.0.0 --icon "assets\krypto.ico" --win-shortcut --win-menu
```

Installer output:

- `installer/Krypto-1.0.exe`

## Notes

- Do not use `--win-console` unless you want a terminal window to open with the app.
- App settings are saved to:
  - `%USERPROFILE%\.secure-file-manager.properties`
- Default encrypted/decrypted output folder:
  - `%USERPROFILE%\Documents\SecureFiles`

## Troubleshooting

- **`mvn` not recognized**
  - Install Maven and make sure `mvn -v` works.
- **`Invalid or unsupported type: [exe]`**
  - Install WiX and reopen terminal.
- **JavaFX runtime missing error on launch**
  - Ensure packaging uses `com.example.Launcher` as `--main-class`.

## License

This project is for educational/assignment use. Add your preferred license if you plan to distribute publicly.
