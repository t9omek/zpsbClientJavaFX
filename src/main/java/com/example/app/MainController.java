package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MainController {

    @FXML
    private TextField nameField;

    @FXML
    private Label resultLabel;

    @FXML
    private void onButtonClick() {
        String name = nameField.getText();

        if (name == null || name.isBlank()) {
            resultLabel.setText("Wpisz imię.");
            return;
        }

        resultLabel.setText("Cześć, " + name + "!");
    }
}
