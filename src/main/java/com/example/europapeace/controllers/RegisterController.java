package com.example.europapeace.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private RadioButton rbSef;
    @FXML private RadioButton rbConsiliu;
    @FXML private VBox vboxTara;
    @FXML private ComboBox<String> comboState;

    @Autowired private ConfigurableApplicationContext springContext;

    @FXML
    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        rbSef.setToggleGroup(group);
        rbConsiliu.setToggleGroup(group);

        // Expression lambda (fără acolade dacă e o singură instrucțiune)
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> vboxTara.setVisible(newVal == rbSef));
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        // "Accessing" the fields to remove "never accessed" warning
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String tara = (rbSef.isSelected()) ? comboState.getValue() : "N/A";

        logger.info("Înregistrare: utilizator={}, email={}, rol={}, țară={}",
                username, email, (rbSef.isSelected() ? "Șef Stat" : "Consiliu"), tara);

        // Aici adaugi logica de salvare efectivă în baza de date

        backToLogin(event);
    }

    @FXML
    public void backToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login_view.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            logger.error("Eroare la navigarea către Login", e);
        }
    }
}