package dynamic.casino;

import javafx.animation.*;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CasinoController {

    @FXML
    private VBox mainContainer;

    @FXML
    private VBox settingsPanel;

    @FXML
    private TextField columnsField;

    @FXML
    private Button startButton;

    @FXML
    private Button applySettingsButton;

    @FXML
    private Button loadImagesButton;

    @FXML
    private VBox gameArea;

    @FXML
    private Label probabilityLabel;

    @FXML
    private Label resultLabel;

    @FXML
    private Label statusLabel;

    // Свойства для паттерна Observer/Bindings
    private final IntegerProperty columnsProperty = new SimpleIntegerProperty(3);
    private final IntegerProperty rowsProperty = new SimpleIntegerProperty(1);
    private final IntegerProperty symbolCountProperty = new SimpleIntegerProperty(0);
    private final BooleanProperty isSpinningProperty = new SimpleBooleanProperty(false);

    private List<List<ImageView>> reels;
    private List<List<Image>> reelImages;

    // Стратегия генерации случайных чисел
    private RandomNumberGenerator rng = new DefaultRandomGenerator();

    // Стратегия расчета вероятности
    private ProbabilityCalculator probabilityCalculator = new DefaultProbabilityCalculator();

    // Список изображений для использования
    private List<Image> gameImages = new ArrayList<>();
    private String imagesDirectory = "images"; // Директория с изображениями

    // Состояние автомата
    private GameState currentState = GameState.IDLE;

    @FXML
    public void initialize() {
        setupSettingsPanel();
        setupBindings();
        createGameArea();
        setupEventHandlers();
        loadDefaultImages(); // Загружаем изображения при запуске
        applyCasinoStyle();
    }

    private void setupSettingsPanel() {
        columnsField.setText(String.valueOf(columnsProperty.get()));
    }

    private void setupBindings() {
        // Биндинги для автоматического обновления UI
        columnsProperty.addListener((obs, oldVal, newVal) -> {
            columnsField.setText(String.valueOf(newVal.intValue()));
            updateRowsBasedOnColumns(newVal.intValue());
            updateProbability();
        });

        symbolCountProperty.addListener((obs, oldVal, newVal) -> updateProbability());
        rowsProperty.addListener((obs, oldVal, newVal) -> updateProbability());
        isSpinningProperty.addListener((obs, oldVal, newVal) -> startButton.setDisable(newVal.booleanValue()));
    }

    private void setupEventHandlers() {
        applySettingsButton.setOnAction(e -> applySettings());
        startButton.setOnAction(e -> executeCommand(new StartSpinCommand()));
        loadImagesButton.setOnAction(e -> loadImagesFromDirectory());
    }

    private void applyCasinoStyle() {
        // Основной фон казино
        mainContainer.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #8B0000, #dc143c);");

        // Панель настроек в стиле казино
        settingsPanel.setStyle(
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #DAA520, #B8860B);" +
            "-fx-padding: 20;" +
            "-fx-border-color: #FFD700;" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;"
        );

        // Стиль для кнопок казино
        String buttonStyle =
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #32CD32, #228B22);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;" +
            "-fx-border-color: #006400;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 5;" +
            "-fx-background-radius: 5;";

        applySettingsButton.setStyle(buttonStyle);
        loadImagesButton.setStyle(buttonStyle);
        startButton.setStyle(
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #FF4500, #DC143C);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 16px;" +
            "-fx-border-color: #8B0000;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 5;" +
            "-fx-background-radius: 5;"
        );

        // Стиль для текстовых полей
        columnsField.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #DAA520;" +
            "-fx-border-width: 2;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;"
        );

        // Стиль для меток
        statusLabel.setStyle(
            "-fx-text-fill: #000080;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-background-color: rgba(255, 255, 255, 0.7);" +
            "-fx-padding: 5;" +
            "-fx-background-radius: 5;"
        );

        probabilityLabel.setStyle(
            "-fx-text-fill: #000080;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;" +
            "-fx-background-color: rgba(255, 255, 255, 0.7);" +
            "-fx-padding: 5;" +
            "-fx-background-radius: 5;"
        );

        resultLabel.setStyle(
            "-fx-text-fill: #FFD700;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 16px;" +
            "-fx-effect: dropshadow(gaussian, black, 3, 0, 1, 1);"
        );
    }

    private void loadDefaultImages() {
        gameImages.clear();

        // Проверяем существование директории images
        File imagesDir = new File(imagesDirectory);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
            statusLabel.setText("Создана директория 'images'. Поместите сюда изображения и нажмите 'Загрузить изображения'");
            return;
        }

        loadImagesFromDirectory();
    }

    private void loadImagesFromDirectory() {
        gameImages.clear();
        File imagesDir = new File(imagesDirectory);

        if (!imagesDir.exists() || !imagesDir.isDirectory()) {
            showAlert("Ошибка", "Директория 'images' не найдена");
            return;
        }

        File[] imageFiles = imagesDir.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".png") ||
            name.toLowerCase().endsWith(".jpg") ||
            name.toLowerCase().endsWith(".jpeg") ||
            name.toLowerCase().endsWith(".gif"));

        if (imageFiles == null || imageFiles.length == 0) {
            showAlert("Предупреждение", "В директории 'images' не найдено изображений");
            statusLabel.setText("Поместите изображения в папку 'images' и нажмите 'Загрузить изображения'");
            return;
        }

        // Загружаем изображения
        for (File imageFile : imageFiles) {
            try {
                Image image = new Image(imageFile.toURI().toString());
                if (!image.isError()) {
                    gameImages.add(image);
                }
            } catch (Exception e) {
                System.err.println("Ошибка загрузки изображения: " + imageFile.getName());
            }
        }

        if (gameImages.isEmpty()) {
            showAlert("Ошибка", "Не удалось загрузить ни одного изображения");
            statusLabel.setText("Ошибка загрузки изображений");
        } else {
            symbolCountProperty.set(gameImages.size());
            statusLabel.setText("Загружено изображений: " + gameImages.size());
            createGameArea(); // Пересоздаем игровую область с новыми изображениями
        }
    }

    private void applySettings() {
        try {
            int newColumns = Integer.parseInt(columnsField.getText());

            if (newColumns > 0 && newColumns <= 10) {
                columnsProperty.set(newColumns);
                createGameArea();
            } else {
                showAlert("Ошибка", "Количество колонок должно быть от 1 до 10");
            }
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Введите корректное числовое значение");
        }
    }

    private void updateRowsBasedOnColumns(int columns) {
        // При C ≤ 5 — 1 ряд, при C > 5 — 2 ряда
        int rows = (columns <= 5) ? 1 : 2;
        rowsProperty.set(rows);
    }

    private void createGameArea() {
        // Проверка состояния
        if (currentState == GameState.SPINNING) {
            return; // Нельзя изменять игровое поле во время спина
        }

        gameArea.getChildren().clear();
        reels = new ArrayList<>();
        reelImages = new ArrayList<>();

        if (gameImages.isEmpty()) {
            Label noImagesLabel = new Label("Нет изображений для отображения.\nПоместите изображения в папку 'images' и нажмите 'Загрузить изображения'");
            noImagesLabel.setAlignment(Pos.CENTER);
            noImagesLabel.setStyle(
                "-fx-text-alignment: center;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-color: rgba(0, 0, 0, 0.7);" +
                "-fx-padding: 20;" +
                "-fx-background-radius: 10;"
            );
            gameArea.getChildren().add(noImagesLabel);
            return;
        }

        // Создаем контейнер для барабанов с адаптивной сеткой
        GridPane reelsContainer = new GridPane();
        reelsContainer.setAlignment(Pos.CENTER);
        reelsContainer.setHgap(15);
        reelsContainer.setVgap(15);
        reelsContainer.setPadding(new Insets(30));

        // Стиль игрового поля казино
        reelsContainer.setStyle(
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #006400, #008000);" +
            "-fx-border-color: #FFD700;" +
            "-fx-border-width: 5;" +
            "-fx-border-radius: 15;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, black, 10, 0, 3, 3);"
        );

        // Определяем количество рядов в зависимости от количества колонок
        int totalColumns = columnsProperty.get();
        int topRowColumns, bottomRowColumns;

        if (totalColumns <= 5) {
            // Одна строка
            topRowColumns = totalColumns;
            bottomRowColumns = 0;
        } else {
            // Две строки
            topRowColumns = (int) Math.ceil(totalColumns / 2.0);
            bottomRowColumns = totalColumns - topRowColumns;
        }

        int columnIndex = 0;

        // Создаем верхний ряд
        for (int col = 0; col < topRowColumns; col++) {
            VBox column = createColumn(columnIndex);
            reelsContainer.add(column, col, 0);
            columnIndex++;
        }

        // Создаем нижний ряд (если нужно)
        if (bottomRowColumns > 0) {
            for (int col = 0; col < bottomRowColumns; col++) {
                VBox column = createColumn(columnIndex);
                reelsContainer.add(column, col, 1);
                columnIndex++;
            }
        }

        gameArea.getChildren().add(reelsContainer);
        updateProbability();
    }

    private VBox createColumn(int colIndex) {
        VBox column = new VBox(8);
        column.setAlignment(Pos.CENTER);
        column.setStyle(
            "-fx-border-color: #FFD700;" +
            "-fx-border-width: 3;" +
            "-fx-padding: 12;" +
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #C0C0C0, #A9A9A9);" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, black, 5, 0, 2, 2);"
        );

        List<ImageView> columnReels = new ArrayList<>();
        List<Image> columnImages = new ArrayList<>();

        // Создаем ячейки в колонке (только одна строка)
        for (int row = 0; row < 1; row++) { // Фиксировано 1 строка в колонке
            ImageView imageView = SymbolFactory.createSymbol(gameImages, rng);
            imageView.setFitWidth(110);
            imageView.setFitHeight(110);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);

            // Устанавливаем случайное изображение
            Image randomImage = gameImages.get(rng.nextInt(gameImages.size()));
            imageView.setImage(randomImage);
            columnImages.add(randomImage);

            column.getChildren().add(imageView);
            columnReels.add(imageView);
        }

        // Добавляем колонку в списки
        reels.add(columnReels);
        reelImages.add(columnImages);

        return column;
    }

    private void executeCommand(Command command) {
        // Проверка состояния перед выполнением команды
        if (command.canExecute(currentState)) {
            command.execute();
        }
    }

    // Команда запуска прокрутки
    private class StartSpinCommand implements Command {
        @Override
        public void execute() {
            startSpin();
        }

        @Override
        public boolean canExecute(GameState currentState) {
            return currentState == GameState.IDLE && !gameImages.isEmpty();
        }
    }

    private void startSpin() {
        if (currentState != GameState.IDLE || gameImages.isEmpty()) return;

        currentState = GameState.SPINNING;
        isSpinningProperty.set(true);
        resultLabel.setText("");

        // Добавляем эффект начала игры
        startButton.setStyle(
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #808080, #696969);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 16px;" +
            "-fx-border-color: #2F4F4F;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 5;" +
            "-fx-background-radius: 5;"
        );

        // Запускаем анимацию для каждой колонки с разной задержкой
        for (int col = 0; col < columnsProperty.get(); col++) {
            int finalCol = col;
            PauseTransition delay = new PauseTransition(Duration.millis(col * 300));
            delay.setOnFinished(e -> spinColumn(finalCol));
            delay.play();
        }

        // После завершения всех анимаций проверяем результат
        Timeline checkResult = new Timeline(new KeyFrame(Duration.millis(columnsProperty.get() * 300 + 2000), e -> {
            checkWin();
            currentState = GameState.IDLE;
            isSpinningProperty.set(false);
            // Возвращаем стиль кнопки
            startButton.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #FF4500, #DC143C);" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 16px;" +
                "-fx-border-color: #8B0000;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;"
            );
        }));
        checkResult.play();
    }

    private void spinColumn(int col) {
        List<ImageView> columnReels = reels.get(col);
        List<Image> columnImages = reelImages.get(col);

        // Создаем анимацию смены изображений
        Timeline spinAnimation = new Timeline();
        Random random = new Random();

        // Быстрая смена изображений
        for (int i = 0; i < 15; i++) {
            KeyFrame kf = new KeyFrame(Duration.millis(i * 80), e -> {
                for (int row = 0; row < columnReels.size(); row++) {
                    Image newImage = gameImages.get(rng.nextInt(gameImages.size()));
                    columnReels.get(row).setImage(newImage);
                    columnImages.set(row, newImage);
                }
            });
            spinAnimation.getKeyFrames().add(kf);
        }

        // Замедление в конце
        for (int i = 0; i < 5; i++) {
            KeyFrame kf = new KeyFrame(Duration.millis(1200 + i * 150), e -> {
                for (int row = 0; row < columnReels.size(); row++) {
                    Image newImage = gameImages.get(rng.nextInt(gameImages.size()));
                    columnReels.get(row).setImage(newImage);
                    columnImages.set(row, newImage);
                }
            });
            spinAnimation.getKeyFrames().add(kf);
        }

        spinAnimation.play();
    }

    private void checkWin() {
        if (columnsProperty.get() == 0 || gameImages.isEmpty()) return;

        boolean isWin = true;
        Image firstImage = reelImages.get(0).get(0);

        // Проверяем все изображения на совпадение
        for (int col = 0; col < columnsProperty.get(); col++) {
            for (int row = 0; row < 1; row++) { // Фиксировано 1 строка
                if (!reelImages.get(col).get(row).getUrl().equals(firstImage.getUrl())) {
                    isWin = false;
                    break;
                }
            }
            if (!isWin) break;
        }

        if (isWin) {
            resultLabel.setText("🎰 Выигрыш! 🎰");
            resultLabel.setStyle(
                "-fx-text-fill: #FFD700;" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-effect: dropshadow(gaussian, black, 3, 0, 1, 1);" +
                "-fx-background-color: rgba(0, 100, 0, 0.7);" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 10;"
            );
        } else {
            resultLabel.setText("💔 Попробуйте еще раз! 💔");
            resultLabel.setStyle(
                "-fx-text-fill: #FF6347;" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-effect: dropshadow(gaussian, black, 3, 0, 1, 1);" +
                "-fx-background-color: rgba(139, 0, 0, 0.7);" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 10;"
            );
        }
    }

    private void updateProbability() {
        if (gameImages.isEmpty()) {
            probabilityLabel.setText("Вероятность выигрыша: 0% (нет изображений)");
            return;
        }

        double probability = probabilityCalculator.calculateWinProbability(
            columnsProperty.get(),
            rowsProperty.get(),
            symbolCountProperty.get()
        );
        probabilityLabel.setText(String.format("🎲 Вероятность выигрыша: %.4f%% 🎲", probability * 100));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Стиль для диалогового окна
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #DAA520, #B8860B);" +
            "-fx-border-color: #FFD700;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 5;"
        );

        alert.showAndWait();
    }
}
