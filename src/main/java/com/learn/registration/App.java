package com.learn.registration;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * APPLICATION ENTRY POINT
 *
 * Security change: the first screen is now login.fxml, not registration.fxml.
 * No part of the app is accessible until credentials are verified.
 *
 * Flow:
 *   main() → launch() → JavaFX thread → start() → login.fxml
 *   LoginViewController → on success → userlist.fxml
 */
public class App extends Application {

    public static final String APP_TITLE = "JavaFX Registration — MVC Demo";

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxml = getClass().getResource(
                "/com/learn/registration/view/login.fxml");

        if (fxml == null) {
            throw new IllegalStateException(
                "Cannot find login.fxml. Check the resources path.");
        }

        FXMLLoader loader = new FXMLLoader(fxml);
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle(APP_TITLE + " — Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);   // login window is fixed size
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
