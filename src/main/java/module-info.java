module dynamic.casino {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens dynamic.casino to javafx.fxml;
    exports dynamic.casino;
}
