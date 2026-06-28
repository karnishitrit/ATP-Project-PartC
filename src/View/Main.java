package View;

import Model.IModel;
import Model.MyModel;
import ViewModel.MyViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point of the JavaFX application.
 * Initializes the MVVM components, loads the main view,
 * and manages the application lifecycle.
 */
public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class);

    /**
     * Initializes the application, creates the model,
     * view model and view, and displays the main window.
     * Also starts the maze servers and registers
     * application event handlers.
     *
     * @param primaryStage the primary application window
     * @throws Exception if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Application started");

        // Load the main application view.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/View/MyView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 700);

        scene.getStylesheets().add(getClass().getResource("MyStyle.css").toExternalForm());

        // Create the model and view model.
        IModel model = new MyModel();
        model.startServers();
        MyViewModel viewModel = new MyViewModel(model);

        // Connect the view with its view model.
        MyViewController controller = fxmlLoader.getController();
        controller.setViewModel(viewModel);

        primaryStage.setTitle("Maze Game");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(400);
        primaryStage.show();

        logger.info("Main window displayed");

        scene.setOnKeyPressed(controller::keyPressed);
        scene.setOnScroll(controller::mouseScrolled);
        scene.getRoot().requestFocus();


        // Stop the servers before closing the application.
        primaryStage.setOnCloseRequest(event -> {
            model.stopServers();
            logger.info("Application closed");
            primaryStage.close();
        });
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}