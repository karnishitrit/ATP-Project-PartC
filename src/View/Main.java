package View;

import Model.IModel;
import Model.MyModel;
import ViewModel.MyViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/View/MyView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 700);

        scene.getStylesheets().add(getClass().getResource("MyStyle.css").toExternalForm());

        IModel model = new MyModel();
        MyViewModel viewModel = new MyViewModel(model);

        MyViewController controller = fxmlLoader.getController();
        controller.setViewModel(viewModel);

        primaryStage.setTitle("Maze Game");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(400);
        primaryStage.show();

        scene.setOnKeyPressed(controller::keyPressed);
        scene.setOnScroll(controller::mouseScrolled);
        scene.getRoot().requestFocus();


        primaryStage.setOnCloseRequest(event -> {
            primaryStage.close();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}