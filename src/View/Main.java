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

public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Application started");

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/View/MyView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 700);

        scene.getStylesheets().add(getClass().getResource("MyStyle.css").toExternalForm());

        IModel model = new MyModel();
        model.startServers();
        MyViewModel viewModel = new MyViewModel(model);

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


        primaryStage.setOnCloseRequest(event -> {

            model.stopServers();

            logger.info("Application closed");

            primaryStage.close();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}