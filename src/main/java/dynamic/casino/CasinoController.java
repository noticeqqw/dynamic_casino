package dynamic.casino;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import java.util.concurrent.atomic.AtomicInteger;

import java.io.File;
import java.util.*;

public class CasinoController {

    @FXML
    private VBox mainContainer;

    @FXML
    private VBox settingsPanel;

    @FXML
    private TextField columnsField;

    @FXML
    private TextField symbolsToUseField;

    @FXML
    private TextField spinSpeedField; // Скорость прокрутки (интенсивность)

    @FXML
    private TextField simulationSpeedField; // Длительность симуляции в секундах

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

    private final IntegerProperty columnsProperty = new SimpleIntegerProperty(3);
    private final IntegerProperty rowsProperty = new SimpleIntegerProperty(1);
    private final IntegerProperty symbolCountProperty = new SimpleIntegerProperty(0);
    private final IntegerProperty symbolsToUseProperty = new SimpleIntegerProperty(0);
    private final DoubleProperty spinSpeedProperty = new SimpleDoubleProperty(1.0);   // Интенсивность вращения
    private final DoubleProperty simulationSpeedProperty = new SimpleDoubleProperty(3.0); // Длительность в секундах
    private final BooleanProperty isSpinningProperty = new SimpleBooleanProperty(false);

    private final AtomicInteger activeAnimations = new AtomicInteger(0);

    private List<VBox> reels;
    private List<List<Integer>> reelSymbolIndices; // Храним индексы, а не Image

    private RandomNumberGenerator rng = new DefaultRandomGenerator();

    private ProbabilityCalculator probabilityCalculator = new DefaultProbabilityCalculator();

    private List<Image> gameImages = new ArrayList<>();
    private List<Image> usedImages = new ArrayList<>();
    private String imagesDirectory = "images";

    private GameState currentState = GameState.IDLE;

    @FXML
    public void initialize() {
        setupSettingsPanel();
        setupBindings();
        createGameArea();
        setupEventHandlers();
        loadDefaultImages();
        applyCasinoStyle();
    }

    private void setupSettingsPanel() {
        columnsField.setText(String.valueOf(columnsProperty.get()));
        if (symbolsToUseField != null) {
            symbolsToUseField.setText("0");
        }
        if (spinSpeedField != null) {
            spinSpeedField.setText(String.valueOf(spinSpeedProperty.get()));
        }
        if (simulationSpeedField != null) {
            simulationSpeedField.setText(String.valueOf(simulationSpeedProperty.get()));
        }
    }

    private void setupBindings() {
        columnsProperty.addListener((obs, oldVal, newVal) -> {
            if (columnsField != null) {
                columnsField.setText(String.valueOf(newVal.intValue()));
            }
            updateRowsBasedOnColumns(newVal.intValue());
            updateProbability();
        });

        symbolCountProperty.addListener((obs, oldVal, newVal) -> updateProbability());
        rowsProperty.addListener((obs, oldVal, newVal) -> updateProbability());
        if (startButton != null) {
            BooleanBinding noImagesOrNoColumns = Bindings.createBooleanBinding(
                () -> symbolCountProperty.get() <= 0 || columnsProperty.get() <= 0,
                symbolCountProperty, columnsProperty
            );
            startButton.disableProperty().bind(isSpinningProperty.or(noImagesOrNoColumns));
        }

        symbolsToUseProperty.addListener((obs, oldVal, newVal) -> updateUsedImages());

        spinSpeedProperty.addListener((obs, oldVal, newVal) -> {
            if (spinSpeedField != null) {
                spinSpeedField.setText(String.valueOf(newVal.doubleValue()));
            }
        });

        simulationSpeedProperty.addListener((obs, oldVal, newVal) -> {
            if (simulationSpeedField != null) {
                simulationSpeedField.setText(String.valueOf(newVal.doubleValue()));
            }
        });
    }

    private void setupEventHandlers() {
        if (applySettingsButton != null) {
            applySettingsButton.setOnAction(e -> applySettings());
        }
        if (startButton != null) {
            startButton.setOnAction(e -> executeCommand(new StartSpinCommand()));
        }
        if (loadImagesButton != null) {
            loadImagesButton.setOnAction(e -> loadImagesFromDirectory());
        }
    }

    private void applyCasinoStyle() {
        if (mainContainer != null) {
            mainContainer.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #8B0000, #dc143c);");
        }

        if (settingsPanel != null) {
            settingsPanel.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #DAA520, #B8860B);" +
                "-fx-padding: 20;" +
                "-fx-border-color: #FFD700;" +
                "-fx-border-width: 3;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
            );
        }

        String buttonStyle =
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #32CD32, #228B22);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;" +
            "-fx-border-color: #006400;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 5;" +
            "-fx-background-radius: 5;";

        if (applySettingsButton != null) {
            applySettingsButton.setStyle(buttonStyle);
        }
        if (loadImagesButton != null) {
            loadImagesButton.setStyle(buttonStyle);
        }
        if (startButton != null) {
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
        }

        String textFieldStyle =
            "-fx-background-color: white;" +
            "-fx-border-color: #DAA520;" +
            "-fx-border-width: 2;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;";

        if (columnsField != null) {
            columnsField.setStyle(textFieldStyle);
        }
        if (symbolsToUseField != null) {
            symbolsToUseField.setStyle(textFieldStyle);
        }
        if (spinSpeedField != null) {
            spinSpeedField.setStyle(textFieldStyle);
        }
        if (simulationSpeedField != null) {
            simulationSpeedField.setStyle(textFieldStyle);
        }

        if (statusLabel != null) {
            statusLabel.setStyle(
                "-fx-text-fill: #000080;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 12px;" +
                "-fx-background-color: rgba(255, 255, 255, 0.7);" +
                "-fx-padding: 5;" +
                "-fx-background-radius: 5;"
            );
        }

        if (probabilityLabel != null) {
            probabilityLabel.setStyle(
                "-fx-text-fill: #000080;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-background-color: rgba(255, 255, 255, 0.7);" +
                "-fx-padding: 5;" +
                "-fx-background-radius: 5;"
            );
        }

        if (resultLabel != null) {
            resultLabel.setStyle(
                "-fx-text-fill: #FFD700;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 16px;" +
                "-fx-effect: dropshadow(gaussian, black, 3, 0, 1, 1);"
            );
        }
    }

    private void loadDefaultImages() {
        gameImages.clear();

        File imagesDir = new File(imagesDirectory);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
            if (statusLabel != null) {
                statusLabel.setText("Создана директория 'images'. Поместите сюда изображения и нажмите 'Загрузить изображения'");
            }
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
            if (statusLabel != null) {
                statusLabel.setText("Поместите изображения в папку 'images' и нажмите 'Загрузить изображения'");
            }
            return;
        }

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

        removeDuplicateImages();

        if (gameImages.isEmpty()) {
            showAlert("Ошибка", "Не удалось загрузить ни одного изображения");
            if (statusLabel != null) {
                statusLabel.setText("Ошибка загрузки изображений");
            }
        } else {
            symbolCountProperty.set(gameImages.size());
            updateUsedImages();
            if (statusLabel != null) {
                statusLabel.setText("Загружено изображений: " + gameImages.size());
            }
            createGameArea();
        }
    }

    private void updateUsedImages() {
        int symbolsToUse = symbolsToUseProperty.get();

        if (symbolsToUse <= 0 || symbolsToUse >= gameImages.size()) {
            usedImages = new ArrayList<>(gameImages);
        } else {
            usedImages = new ArrayList<>(gameImages.subList(0, Math.min(symbolsToUse, gameImages.size())));
        }
        symbolCountProperty.set(usedImages.size());
    }

    private void applySettings() {
        try {
            int newColumns = Integer.parseInt(columnsField.getText());
            int symbolsToUse = 0;
            double spinSpeed = 1.0;
            double simulationSpeed = 3.0;

            if (symbolsToUseField != null && !symbolsToUseField.getText().isEmpty()) {
                symbolsToUse = Integer.parseInt(symbolsToUseField.getText());
                if (symbolsToUse < 1 || symbolsToUse > 8) {
                    showAlert("Ошибка", "Количество фото должно быть от 1 до 8");
                    return;
                }
            }

            if (spinSpeedField != null && !spinSpeedField.getText().isEmpty()) {
                spinSpeed = Double.parseDouble(spinSpeedField.getText());
                if (spinSpeed <= 0.01 || spinSpeed > 10) {
                    showAlert("Ошибка", "Скорость прокрутки должна быть от 0.01 до 10\n(чем меньше — тем медленнее)");
                    return;
                }
            }

            if (simulationSpeedField != null && !simulationSpeedField.getText().isEmpty()) {
                simulationSpeed = Double.parseDouble(simulationSpeedField.getText());
                if (simulationSpeed < 0.5 || simulationSpeed > 30) {
                    showAlert("Ошибка", "Длительность симуляции должна быть от 0.5 до 30 секунд");
                    return;
                }
            }

            if (newColumns > 0 && newColumns <= 10) {
                columnsProperty.set(newColumns);
                symbolsToUseProperty.set(symbolsToUse);
                spinSpeedProperty.set(spinSpeed);
                simulationSpeedProperty.set(simulationSpeed);
                createGameArea();
            } else {
                showAlert("Ошибка", "Количество колонок должно быть от 1 до 10");
            }
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Введите корректные числовые значения");
        }
    }

    private void updateRowsBasedOnColumns(int columns) {
        int rows = (columns <= 5) ? 1 : 2;
        rowsProperty.set(rows);
    }

    private void createGameArea() {
        if (currentState == GameState.SPINNING) {
            return;
        }

        if (gameArea != null) {
            gameArea.getChildren().clear();
        }
        reels = new ArrayList<>();
        reelSymbolIndices = new ArrayList<>(); // Инициализируем новое поле

        if (usedImages.isEmpty()) {
            if (gameArea != null) {
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
            }
            return;
        }

        if (gameArea != null) {
            GridPane reelsContainer = new GridPane();
            reelsContainer.setAlignment(Pos.CENTER);
            reelsContainer.setHgap(15);
            reelsContainer.setVgap(15);
            reelsContainer.setPadding(new Insets(30));
            reelsContainer.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #006400, #008000);" +
                "-fx-border-color: #FFD700;" +
                "-fx-border-width: 5;" +
                "-fx-border-radius: 15;" +
                "-fx-background-radius: 15;" +
                "-fx-effect: dropshadow(gaussian, black, 10, 0, 3, 3);"
            );

            int totalColumns = columnsProperty.get();
            int topRowColumns, bottomRowColumns;

            if (totalColumns <= 5) {
                topRowColumns = totalColumns;
                bottomRowColumns = 0;
            } else {
                topRowColumns = (int) Math.ceil(totalColumns / 2.0);
                bottomRowColumns = totalColumns - topRowColumns;
            }

            int columnIndex = 0;
            for (int col = 0; col < topRowColumns; col++) {
                VBox column = createColumn(columnIndex);
                reelsContainer.add(column, col, 0);
                columnIndex++;
            }

            if (bottomRowColumns > 0) {
                for (int col = 0; col < bottomRowColumns; col++) {
                    VBox column = createColumn(columnIndex);
                    reelsContainer.add(column, col, 1);
                    columnIndex++;
                }
            }

            gameArea.getChildren().add(reelsContainer);
        }
        updateProbability();
    }

    private VBox createColumn(int colIndex) {
        VBox column = new VBox();
        column.setAlignment(Pos.CENTER);

        Pane slotCell = new Pane();
        slotCell.setPrefSize(110, 110);

        Rectangle clip = new Rectangle(110, 110);
        slotCell.setClip(clip);
        VBox reelBox = new VBox();
        reelBox.setSpacing(0);

        List<Integer> columnIndices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int randomIndex = rng.nextInt(usedImages.size());
            Image randomImage = usedImages.get(randomIndex);
            ImageView img = new ImageView(randomImage);
            img.setFitWidth(110);
            img.setFitHeight(110);
            img.setPreserveRatio(false);
            reelBox.getChildren().add(img);
            columnIndices.add(randomIndex);
        }

        slotCell.getChildren().add(reelBox);
        column.getChildren().add(slotCell);
        reels.add(reelBox);
        reelSymbolIndices.add(columnIndices);

        return column;
    }

    private void executeCommand(Command command) {
        if (command.canExecute(currentState)) {
            command.execute();
        }
    }

    private class StartSpinCommand implements Command {
        @Override
        public void execute() {
            startSpin();
        }

        @Override
        public boolean canExecute(GameState currentState) {
            return currentState == GameState.IDLE && !usedImages.isEmpty();
        }
    }


    private void startSpin() {
        if (currentState != GameState.IDLE || usedImages.isEmpty()) return;

        currentState = GameState.SPINNING;
        isSpinningProperty.set(true);
        if (resultLabel != null) {
            resultLabel.setText("");
        }
        if (startButton != null) {
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
        }

        double totalSimulationSeconds = simulationSpeedProperty.get();
        double spinIntensity = spinSpeedProperty.get();

        long totalSimulationMs = (long) (totalSimulationSeconds * 1000);
        long stepDurationMs = (long) (200 / spinIntensity);

        // Сбрасываем счётчик активных анимаций
        activeAnimations.set(0);

        // Запускаем все барабаны одновременно
        for (int col = 0; col < columnsProperty.get(); col++) {
            int finalCol = col;
            long delayMs = (long) (col * (totalSimulationMs / (double) columnsProperty.get() / 4.0));
PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
            delay.setOnFinished(e -> spinColumn(finalCol, stepDurationMs, totalSimulationMs));
            delay.play();
        }

        // Планируем остановку и проверку результата
        Timeline stopTimeline = new Timeline(new KeyFrame(
            Duration.millis(totalSimulationMs),
            e -> {
                // Останавливаем логику — новые анимации не запускаются
                currentState = GameState.IDLE;

                // Начинаем асинхронное ожидание завершения активных анимаций
                waitForAnimationsToFinish();
            }
        ));
        stopTimeline.play();
    }

    // 🔥 Асинхронное ожидание завершения анимаций — БЕЗ БЛОКИРОВКИ UI
    private void waitForAnimationsToFinish() {
        if (activeAnimations.get() == 0) {
            // Все анимации завершены — можно проверять результат
            Platform.runLater(this::showFinalResult);
        } else {
            // Ждём ещё 20 мс и проверяем снова
            PauseTransition wait = new PauseTransition(Duration.millis(20));
            wait.setOnFinished(e -> waitForAnimationsToFinish());
            wait.play();
        }
    }

    // Показываем результат, когда всё готово
    private void showFinalResult() {
        try {
            checkWin(); // ← Теперь данные стабильны!
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Ошибка", "Во время подсчёта результата произошла ошибка:\n" + ex.getMessage());
        } finally {
            isSpinningProperty.set(false);

            if (startButton != null) {
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
            }
        }
    }

    private void spinColumn(int col, long stepDurationMs, long totalSimulationMs) {
        if (col >= reels.size() || col >= reelSymbolIndices.size()) return;

        // 🔥 Увеличиваем счётчик активных анимаций
        activeAnimations.incrementAndGet();

        VBox reelBox = reels.get(col);
        List<Integer> columnIndices = reelSymbolIndices.get(col);

        TranslateTransition spin = new TranslateTransition(Duration.millis(stepDurationMs), reelBox);
        spin.setFromY(0);
        spin.setToY(-110);
        spin.setInterpolator(Interpolator.LINEAR);

        spin.setOnFinished(e -> {
            try {
                Node first = reelBox.getChildren().remove(0);

                if (first instanceof ImageView) {
                    int newIndex = rng.nextInt(usedImages.size());
                    Image newImage = usedImages.get(newIndex);
                    ((ImageView) first).setImage(newImage);

                    if (col < reelSymbolIndices.size()) {
                        List<Integer> indices = reelSymbolIndices.get(col);
                        if (!indices.isEmpty()) {
                            indices.remove(0);
                            indices.add(newIndex);
                            System.out.println("Обновлена колонка " + col + ": новый индекс = " + newIndex);
                        }
                    }
                }

                reelBox.getChildren().add(first);
                reelBox.setTranslateY(0);
            } finally {
                // 🔥 Уменьшаем счётчик — анимация этого шага завершена
                activeAnimations.decrementAndGet();
            }

            // Продолжаем только если игра ещё идёт
            if (currentState == GameState.SPINNING) {
                spinColumn(col, stepDurationMs, totalSimulationMs);
            } else {
                System.out.println("Анимация колонки " + col + " остановлена — игра завершена");
            }
        });

        spin.play();
    }


    private void checkWin() {
        if (columnsProperty.get() == 0 || usedImages.isEmpty() || reelSymbolIndices == null) {
            System.out.println("checkWin: нет данных для проверки");
            return;
        }

        System.out.println("\n=== НАЧАЛО ПРОВЕРКИ ВЫИГРЫША ===");
        System.out.println("Количество колонок: " + columnsProperty.get());

        boolean isWin = false;

        // Собираем реальные индексы верхних символов
        List<Integer> topIndices = new ArrayList<>();
        for (int col = 0; col < columnsProperty.get() && col < reelSymbolIndices.size(); col++) {
            List<Integer> colIndices = reelSymbolIndices.get(col);
            if (colIndices != null && !colIndices.isEmpty()) {
                int topIndex = colIndices.get(0); // первый в списке = верхний символ
                topIndices.add(topIndex);

                // Выводим URL изображения для проверки
                if (topIndex < usedImages.size()) {
                    String imgUrl = usedImages.get(topIndex).getUrl();
                    System.out.println("Колонка " + col + ": индекс=" + topIndex + " | URL=" + imgUrl);
                } else {
                    System.out.println("Колонка " + col + ": индекс=" + topIndex + " | URL=НЕДОПУСТИМЫЙ ИНДЕКС");
                }
            } else {
                System.out.println("Колонка " + col + ": данные отсутствуют");
            }
        }

        // Для 2 колонок: выигрыш, если индексы совпадают
        if (columnsProperty.get() == 2 && topIndices.size() == 2) {
            isWin = (topIndices.get(0).equals(topIndices.get(1)));
            System.out.println("Сравнение: " + topIndices.get(0) + " == " + topIndices.get(1) + " → " + isWin);
        }
        // Для 3+ колонок: все должны совпадать
        else if (topIndices.size() >= 2) {
            Integer first = topIndices.get(0);
            isWin = true;
            for (int i = 1; i < topIndices.size(); i++) {
                if (!topIndices.get(i).equals(first)) {
                    isWin = false;
                    break;
                }
            }
            System.out.println("Все совпадают с первым (" + first + ") → " + isWin);
        }

        System.out.println("ИТОГ: " + (isWin ? "ВЫИГРЫШ 🎰" : "проигрыш"));
        System.out.println("=== КОНЕЦ ПРОВЕРКИ ===\n");

        // Отображаем результат
        if (resultLabel != null) {
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
                resultLabel.setText("Попробуйте ещё раз!");
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
    }

    private void removeDuplicateImages() {
        List<Image> uniqueImages = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        for (Image img : gameImages) {
            String url = img.getUrl();
            if (!seenUrls.contains(url)) {
                seenUrls.add(url);
                uniqueImages.add(img);
                System.out.println("Загружено: " + url);
            } else {
                System.out.println("Пропущен дубликат: " + url);
            }
        }

        gameImages = uniqueImages;
    }

    private void updateProbability() {
        if (usedImages.isEmpty()) {
            if (probabilityLabel != null) {
                probabilityLabel.setText("Вероятность выигрыша: 0% (нет изображений)");
            }
            return;
        }

        double probability = probabilityCalculator.calculateWinProbability(
            columnsProperty.get(),
            rowsProperty.get(),
            symbolCountProperty.get()
        );
        if (probabilityLabel != null) {
            probabilityLabel.setText(String.format("🎲 Вероятность выигрыша: %.4f%% 🎲", probability * 100));
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
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
