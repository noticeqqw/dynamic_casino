package dynamic.casino;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class App extends Application {

    private final Spinner<Integer> colsSpinner = new Spinner<>(1, 12, 3);
    private final Spinner<Integer> itemsSpinner = new Spinner<>(3, 20, 6);
    private final Spinner<Integer> speedSpinner = new Spinner<>(200, 2000, 900, 50); // мс на колонку
    private final Label probLabel = new Label();
    private final Label imagesLabel = new Label("Папка: ресурсы /images");
    private File imagesDir = null;

    private final SlotMachine slotMachine = new SlotMachine();

    @Override
    public void start(Stage stage) {
        // Левая панель параметров
        VBox left = new VBox(8);
        left.setPadding(new Insets(10));

        Button chooseDir = new Button("Выбрать папку с фото");
        chooseDir.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Папка с фото (jpg/png)");
            File dir = dc.showDialog(stage);
            if (dir != null && dir.isDirectory()) {
                imagesDir = dir;
                imagesLabel.setText("Папка: " + dir.getAbsolutePath());
                reloadImages();
            }
        });

        Button reloadBtn = new Button("Применить разметку");
        reloadBtn.setOnAction(e -> applyLayout());

        Button spinBtn = new Button("SPIN");
        spinBtn.setMaxWidth(Double.MAX_VALUE);
        spinBtn.setOnAction(e -> spin());

        Button stopBtn = new Button("STOP");
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setOnAction(e -> slotMachine.stopAll());

        Label colsL = new Label("Кол-во колонок:");
        colsSpinner.valueProperty().addListener((obs, o, n) -> updateProbability());
        Label itemsL = new Label("Элементов в колонке:");
        itemsSpinner.valueProperty().addListener((obs, o, n) -> updateProbability());
        Label speedL = new Label("Скорость (мс/колонку):");

        left.getChildren().addAll(
                colsL, colsSpinner,
                itemsL, itemsSpinner,
                speedL, speedSpinner,
                chooseDir, imagesLabel,
                reloadBtn, spinBtn, stopBtn,
                new Separator(),
                new Label("Вероятность одной линии (все одинаковые):"),
                probLabel
        );

        // Центр: поле с колонками
        BorderPane root = new BorderPane();
        root.setLeft(left);
        root.setCenter(slotMachine.getRoot());
        BorderPane.setAlignment(slotMachine.getRoot(), Pos.CENTER);
        BorderPane.setMargin(slotMachine.getRoot(), new Insets(10));

        Scene scene = new Scene(root, 1100, 650);
        stage.setTitle("dynamic.casino — ЛР1 (JavaFX)");
        stage.setScene(scene);
        stage.show();

        // Инициализация
        applyLayout();
        reloadImages();
        updateProbability();
    }

    private void applyLayout() {
        int cols = colsSpinner.getValue();
        int perCol = itemsSpinner.getValue();
        // Разметка: до 5 колонок — 1 строка, иначе 2 строки (пример простого правила)
        int rows = (cols <= 5) ? 1 : 2;
        slotMachine.configureGrid(cols, rows, perCol);
        updateProbability();
    }

    private void reloadImages() {
        List<Image> imgs = ImageLoader.loadImages(imagesDir);
        if (imgs.isEmpty()) {
            // Попытка загрузить из ресурсов /images
            imgs = ImageLoader.loadFromResources("/images");
        }
        slotMachine.setImages(imgs);
    }

    private void spin() {
        int cols = colsSpinner.getValue();
        int perCol = itemsSpinner.getValue();
        int baseDurationMs = speedSpinner.getValue();

        slotMachine.spinAll(cols, perCol, baseDurationMs);
    }

    private void updateProbability() {
        int k = colsSpinner.getValue();     // количество колонок
        int n = itemsSpinner.getValue();    // уникальных элементов/символов в колонке
        // Модель выигрыша: одна центральная линия, все символы одинаковые
        // Вероятность = (1/n)^(k-1)
        double p = Math.pow(1.0 / n, Math.max(0, k - 1));
        probLabel.setText(String.format("P(win) = (1/%d)^(%d-1) = %.6f (%.4f%%)",
                n, k, p, 100 * p));
    }

    public static void main(String[] args) {
        launch();
    }

    // --- Утилита загрузки изображений (ваши фото) ---
    static class ImageLoader {
        static List<Image> loadImages(File dir) {
            if (dir == null || !dir.isDirectory()) return List.of();
            File[] files = dir.listFiles((d, name) -> {
                String s = name.toLowerCase();
                return s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg");
            });
            if (files == null || files.length == 0) return List.of();
            return java.util.Arrays.stream(files)
                    .map(f -> new Image(f.toURI().toString(), 160, 160, true, true))
                    .collect(Collectors.toList());
        }

        static List<Image> loadFromResources(String root) {
            try {
                // Пробуем несколько стандартных имён
                String[] defaults = {
                        root + "/1.png", root + "/2.png", root + "/3.png",
                        root + "/4.png", root + "/5.png", root + "/6.png"
                };
                List<Image> imgs = java.util.Arrays.stream(defaults)
                        .map(p -> {
                            try {
                                return new Image(App.class.getResourceAsStream(p), 160, 160, true, true);
                            } catch (Exception ex) {
                                return null;
                            }
                        })
                        .filter(i -> i != null && !i.isError())
                        .collect(Collectors.toList());
                return imgs;
            } catch (Exception e) {
                return List.of();
            }
        }
    }
}
