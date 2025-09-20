package dynamic.casino;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/casino.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Игровой автомат казино 777");
        primaryStage.setScene(new Scene(root, 1200, 700));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
