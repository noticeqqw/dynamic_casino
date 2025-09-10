package dynamic.casino;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SlotMachine {

    private final GridPane root = new GridPane();
    private final List<ReelView> reels = new ArrayList<>();
    private List<Image> images = new ArrayList<>();
    private int itemsPerColumn = 6;

    public SlotMachine() {
        root.setHgap(10);
        root.setVgap(10);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.CENTER);
    }

    public Pane getRoot() {
        return root;
    }

    public void setImages(List<Image> imgs) {
        this.images = imgs != null ? imgs : List.of();
        // обновить текущие барабаны, если уже есть
        for (ReelView r : reels) {
            r.setImages(images);
            r.build(itemsPerColumn);
        }
    }

    public void configureGrid(int cols, int rows, int perCol) {
        this.itemsPerColumn = perCol;
        root.getChildren().clear();
        reels.clear();

        int columnsPerRow = (rows == 1 ? cols : (int) Math.ceil(cols / 2.0));
        int total = cols;

        int colIndex = 0;
        int rowIndex = 0;

        for (int i = 0; i < total; i++) {
            ReelView reel = new ReelView();
            reel.setImages(images);
            reel.build(perCol);
            reels.add(reel);

            root.add(reel, colIndex, rowIndex);
            colIndex++;
            if (rows == 2 && colIndex >= columnsPerRow) {
                colIndex = 0;
                rowIndex++;
            }
        }
    }

    public void spinAll(int columns, int perCol, int baseDurationMs) {
        int i = 0;
        for (ReelView r : reels) {
            int jitter = ThreadLocalRandom.current().nextInt(0, 120);
            int extraCycles = 2 + (i % 3); // чуть дольше крутятся разные барабаны
            r.spin(perCol, Math.max(400, baseDurationMs / 2) + i * 80 + jitter, extraCycles);
            i++;
        }
    }

    public void stopAll() {
        for (ReelView r : reels) r.stop();
    }
}
