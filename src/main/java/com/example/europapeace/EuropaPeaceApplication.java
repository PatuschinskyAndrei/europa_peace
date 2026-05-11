package com.example.europapeace;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class EuropaPeaceApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Pornim contextul Spring și îl salvăm
        this.springContext = SpringApplication.run(EuropaPeaceApplication.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login_view.fxml"));

            // Verificare de siguranță: dacă Spring nu a pornit, închidem aplicația
            if (springContext == null) {
                System.err.println("Spring Context nu a putut fi inițializat!");
                Platform.exit();
                return;
            }

            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();
            stage.setTitle("Europa Peace - Login");
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        Application.launch(EuropaPeaceApplication.class, args);
    }
}