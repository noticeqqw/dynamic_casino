module dynamic.casino {
    requires javafx.controls;
    requires javafx.fxml;

    opens dynamic.casino to javafx.fxml;
    exports dynamic.casino;
}
