module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    opens com.example.app to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.example.app;
}
