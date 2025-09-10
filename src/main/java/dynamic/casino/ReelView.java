package dynamic.casino;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ReelView extends StackPane {
    private final VBox strip = new VBox();              // вертикальная лента
    private final Group viewport = new Group(strip);    // чтобы анимировать translateY
    private final List<Image> sourceImages = new ArrayList<>();
    private final double cellSize = 160.0;
    private final double gap = 6.0;

    private int logicalItems = 6;       // сколько логических позиций в колонке
    private boolean spinning = false;
    private Timeline timeline;

    public ReelView() {
        setAlignment(Pos.TOP_CENTER);

        strip.setAlignment(Pos.CENTER);
        strip.setSpacing(gap);

        // Видимое «окно»
        Rectangle clip = new Rectangle();
        // Клип привязываем к фактическим размерам, чтобы не «обрезало» неправильно
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        // Минимальный размер (3 видимых элемента)
        setMinSize(cellSize + 10, cellSize * 3 + gap * 2);
        setPrefSize(cellSize + 10, cellSize * 3 + gap * 2);
        setMaxWidth(cellSize + 10);

        getChildren().add(viewport);

        // Небольшое ускорение рендеринга
        setCache(true);
        strip.setCache(true);
    }

    public void setImages(List<Image> imgs) {
        sourceImages.clear();
        if (imgs != null) {
            // ВАЖНО: грузим синхронно (без backgroundLoading), чтобы не «мигало»
            for (Image im : imgs) {
                Image fixed = new Image(im.getUrl(), 160, 160, true, true, false);
                sourceImages.add(fixed);
            }
        }
    }

    /** Собрать ленту: дублируем элементы сверху и снизу для бесшовного шага */
    public void build(int itemsPerColumn) {
        logicalItems = Math.max(1, itemsPerColumn);
        strip.getChildren().clear();
        if (sourceImages.isEmpty()) return;

        // Заполняем: логические элементы + 2 буфера (сверху/снизу)
        int total = logicalItems + 2;
        for (int i = 0; i < total; i++) {
            int idx = i % sourceImages.size();
            ImageView iv = new ImageView(sourceImages.get(idx));
            iv.setPreserveRatio(true);
            iv.setFitHeight(cellSize);
            strip.getChildren().add(iv);
        }

        // Стартовая позиция: лента стоит так, будто верхний «буфер» уже скрыт
        viewport.setTranslateY(-step());
    }

    /** Один шаг по высоте ячейки (включая gap) */
    private double step() {
        return cellSize + strip.getSpacing();
    }

    /**
     * Прокрутка: шагами. extraCycles — сколько кругов сделать,
     * затем остановиться на случайном индексе из [0..logicalItems-1].
     */
    public void spin(int itemsPerColumn, int baseDurationMs, int extraCycles) {
        if (sourceImages.isEmpty() || spinning) return;
        spinning = true;
        logicalItems = Math.max(1, itemsPerColumn);

        int totalSteps = extraCycles * logicalItems + ThreadLocalRandom.current().nextInt(logicalItems);
        animateSteps(totalSteps, baseDurationMs, () -> spinning = false);
    }

    /** Прокрутить на N шагов, без резких скачков: после каждого шага перекидываем первый узел в конец и ресетим translateY */
    private void animateSteps(int steps, int baseDurationMs, Runnable onEnd) {
        if (timeline != null) timeline.stop();

        final double oneStep = step();
        final Duration perStep = Duration.millis(Math.max(60, Math.min(220, baseDurationMs / Math.max(1, steps))));
        timeline = new Timeline();
        for (int i = 0; i < steps; i++) {
            // Кадр анимации одного шага вниз
            KeyFrame k1 = new KeyFrame(perStep.multiply(i + 1),
                    new KeyValue(viewport.translateYProperty(), -(oneStep * (i + 2)), Interpolator.EASE_IN));

            // В конце шага — «ротация» детей, чтобы создать бесконечную ленту
            final int atStep = i;
            KeyFrame k2 = new KeyFrame(perStep.multiply(i + 1).subtract(Duration.millis(1)), e -> {
                // ничего — просто гарантия порядка KeyFrame
            });

            timeline.getKeyFrames().addAll(k2, k1);
        }

        timeline.setOnFinished(e -> {
            // После последнего шага: аккуратно «пересобираем» ленту
            // Реальная ротация: каждый шаг — снимаем верхний, кидаем в конец
            // Мы не делали в реальном времени — сделаем пакетно:
            int rotations = steps % strip.getChildren().size(); // безопасно
            for (int r = 0; r < rotations; r++) {
                var first = strip.getChildren().remove(0);
                strip.getChildren().add(first);
            }
            // Ставим «окно» обратно ровно на -oneStep (буфер сверху скрыт)
            viewport.setTranslateY(-oneStep);

            if (onEnd != null) onEnd.run();
        });

        timeline.playFromStart();
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        spinning = false;
        // Выровнять на ближайшую «ячейку»
        double y = viewport.getTranslateY();
        double s = step();
        double nearest = -s * Math.round(Math.abs(y) / s);
        viewport.setTranslateY(nearest == 0 ? -s : nearest);
    }
}
