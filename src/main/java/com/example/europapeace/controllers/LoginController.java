package com.example.europapeace.controllers;

import com.example.europapeace.entities.Utilizator;
import com.example.europapeace.repositories.UtilizatorRepository;
import com.example.europapeace.config.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;

@Component
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMessage;

    @Autowired private UtilizatorRepository utilizatorRepository;
    @Autowired private ConfigurableApplicationContext springContext;

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        Utilizator user = utilizatorRepository.findByUsername(username);

        if (user != null && user.getParola().equals(password)) {
            UserSession.getInstance().setUtilizatorLogat(user);
            logger.info("Utilizator logat: {}", username);
            incarcaScena("/main_view.fxml", event);
        } else {
            lblMessage.setText("Date incorecte!");
            lblMessage.setStyle("-fx-text-fill: red;");
        }
    }


    private void incarcaScena(String fxmlPath, ActionEvent event) {
        try {
            // REPARARE CALE: Folosim direct contextul clasei cu calea absoluta
            URL resource = getClass().getResource(fxmlPath);

            // Verificare de siguranta pentru a evita NullPointerException la linia 66
            if (resource == null) {
                logger.error("Fișierul FXML nu a fost găsit la calea: {}", fxmlPath);
                // Încercăm o cale relativă la rădăcina resurselor dacă prima a eșuat
                String caleaRelativa = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
                resource = Thread.currentThread().getContextClassLoader().getResource(caleaRelativa);
            }

            // Dacă și după a doua încercare este null, oprim execuția pentru a nu crapa aplicația
            if (resource == null) {
                lblMessage.setText("Eroare internă: Resursa grafică lipsește.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Preluăm Stage-ul de la sursa evenimentului (butonul apăsat)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            logger.error("Eroare critică la încărcarea scenei {}", fxmlPath, e);
            lblMessage.setText("Eroare la încărcarea paginii.");
        }
    }
}