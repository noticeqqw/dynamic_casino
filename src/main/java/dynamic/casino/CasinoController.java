package dynamic.casino;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

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
    private TextField rowsField;

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

    private List<List<ImageView>> reels;
    private List<List<Image>> reelImages;
    private int columns = 3;
    private int rows = 1;
    private boolean isSpinning = false;

    // Список изображений для использования
    private List<Image> gameImages = new ArrayList<>();
    private String imagesDirectory = "images"; // Директория с изображениями

    @FXML
    public void initialize() {
        setupSettingsPanel();
        createGameArea();
        setupEventHandlers();
        loadDefaultImages(); // Загружаем изображения при запуске
    }

    private void setupSettingsPanel() {
        columnsField.setText(String.valueOf(columns));
        rowsField.setText(String.valueOf(rows));
    }

    private void setupEventHandlers() {
        applySettingsButton.setOnAction(e -> applySettings());
        startButton.setOnAction(e -> startSpin());
        loadImagesButton.setOnAction(e -> loadImagesFromDirectory());
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
            statusLabel.setText("Загружено изображений: " + gameImages.size());
            createGameArea(); // Пересоздаем игровую область с новыми изображениями
        }
    }

    private void applySettings() {
        try {
            int newColumns = Integer.parseInt(columnsField.getText());
            int newRows = Integer.parseInt(rowsField.getText());

            if (newColumns > 0 && newColumns <= 10 && newRows > 0 && newRows <= 5) {
                columns = newColumns;
                rows = newRows;
                updateProbability();
                createGameArea();
            } else {
                showAlert("Ошибка", "Количество колонок должно быть от 1 до 10, строк от 1 до 5");
            }
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Введите корректные числовые значения");
        }
    }

    private void createGameArea() {
        gameArea.getChildren().clear();
        reels = new ArrayList<>();
        reelImages = new ArrayList<>();

        if (gameImages.isEmpty()) {
            Label noImagesLabel = new Label("Нет изображений для отображения.\nПоместите изображения в папку 'images' и нажмите 'Загрузить изображения'");
            noImagesLabel.setAlignment(Pos.CENTER);
            noImagesLabel.setStyle("-fx-text-alignment: center;");
            gameArea.getChildren().add(noImagesLabel);
            return;
        }

        // Создаем контейнер для колонок
        HBox reelsContainer = new HBox(10);
        reelsContainer.setAlignment(Pos.CENTER);
        reelsContainer.setPadding(new Insets(20));

        // Создаем колонки
        for (int col = 0; col < columns; col++) {
            VBox column = new VBox(5);
            column.setAlignment(Pos.CENTER);
            column.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-padding: 10; -fx-background-color: #ffffff;");

            List<ImageView> columnReels = new ArrayList<>();
            List<Image> columnImages = new ArrayList<>();

            // Создаем ячейки в колонке
            for (int row = 0; row < rows; row++) {
                ImageView imageView = new ImageView();
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(false);
                imageView.setSmooth(true);

                // Устанавливаем случайное изображение
                Random random = new Random();
                Image randomImage = gameImages.get(random.nextInt(gameImages.size()));
                imageView.setImage(randomImage);
                columnImages.add(randomImage);

                column.getChildren().add(imageView);
                columnReels.add(imageView);
            }

            reelsContainer.getChildren().add(column);
            reels.add(columnReels);
            reelImages.add(columnImages);
        }

        gameArea.getChildren().add(reelsContainer);
        updateProbability();
    }

    private void startSpin() {
        if (isSpinning || gameImages.isEmpty()) return;

        isSpinning = true;
        startButton.setDisable(true);
        resultLabel.setText("");

        // Запускаем анимацию для каждой колонки с разной задержкой
        for (int col = 0; col < columns; col++) {
            int finalCol = col;
            PauseTransition delay = new PauseTransition(Duration.millis(col * 300));
            delay.setOnFinished(e -> spinColumn(finalCol));
            delay.play();
        }

        // После завершения всех анимаций проверяем результат
        Timeline checkResult = new Timeline(new KeyFrame(Duration.millis(columns * 300 + 2000), e -> {
            checkWin();
            isSpinning = false;
            startButton.setDisable(false);
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
                    Image newImage = gameImages.get(random.nextInt(gameImages.size()));
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
                    Image newImage = gameImages.get(random.nextInt(gameImages.size()));
                    columnReels.get(row).setImage(newImage);
                    columnImages.set(row, newImage);
                }
            });
            spinAnimation.getKeyFrames().add(kf);
        }

        spinAnimation.play();
    }

    private void checkWin() {
        if (columns == 0 || rows == 0 || gameImages.isEmpty()) return;

        boolean isWin = true;
        Image firstImage = reelImages.get(0).get(0);

        // Проверяем все изображения на совпадение
        for (int col = 0; col < columns; col++) {
            for (int row = 0; row < rows; row++) {
                if (!reelImages.get(col).get(row).getUrl().equals(firstImage.getUrl())) {
                    isWin = false;
                    break;
                }
            }
            if (!isWin) break;
        }

        if (isWin) {
            resultLabel.setText("ПОБЕДА! Все изображения совпадают!");
            resultLabel.setStyle("-fx-text-fill: green; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else {
            resultLabel.setText("Попробуйте еще раз!");
            resultLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
        }
    }

    private void updateProbability() {
        if (gameImages.isEmpty()) {
            probabilityLabel.setText("Вероятность выигрыша: 0% (нет изображений)");
            return;
        }

        double probability = ProbabilityCalculator.calculateWinProbability(columns, rows, gameImages.size());
        probabilityLabel.setText(String.format("Вероятность выигрыша: %.4f%%", probability * 100));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
