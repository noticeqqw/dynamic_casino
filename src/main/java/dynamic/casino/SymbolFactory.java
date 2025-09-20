package dynamic.casino;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;

public class SymbolFactory {

    public static ImageView createSymbol(List<Image> gameImages, RandomNumberGenerator rng) {
        ImageView imageView = new ImageView();
        if (!gameImages.isEmpty()) {
            Image randomImage = gameImages.get(rng.nextInt(gameImages.size()));
            imageView.setImage(randomImage);
        }
        return imageView;
    }

    public static ImageView createSymbol(List<Image> gameImages, RandomNumberGenerator rng,
                                       double fitWidth, double fitHeight) {
        ImageView imageView = createSymbol(gameImages, rng);
        imageView.setFitWidth(fitWidth);
        imageView.setFitHeight(fitHeight);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        return imageView;
    }
}
