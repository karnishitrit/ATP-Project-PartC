package View;

import ViewModel.MyViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
public class MyViewController implements IView, Observer {

    @FXML private MazeDisplayer mazeDisplayer;
    @FXML private StackPane mazePane;
    @FXML private StackPane welcomePane;
    private MyViewModel viewModel;

    private boolean gameFinished = false;
    private double zoom = 1.0;

    public void setViewModel(MyViewModel viewModel) {
        this.viewModel = viewModel;
        this.viewModel.addObserver(this);
    }

    @FXML
    public void initialize() {
        mazeDisplayer.widthProperty().bind(mazePane.widthProperty());
        mazeDisplayer.heightProperty().bind(mazePane.heightProperty());

        mazeDisplayer.widthProperty().addListener((obs, oldVal, newVal) -> mazeDisplayer.redraw());
        mazeDisplayer.heightProperty().addListener((obs, oldVal, newVal) -> mazeDisplayer.redraw());

        mazeDisplayer.setVisible(false);
        mazeDisplayer.setManaged(false);

        welcomePane.setVisible(true);
        welcomePane.setManaged(true);

        mazeDisplayer.setOnMouseDragged(event -> {
            int targetRow = mazeDisplayer.getClickedRow(event.getY());
            int targetColumn = mazeDisplayer.getClickedColumn(event.getX());

            int currentRow = viewModel.characterRow.get();
            int currentColumn = viewModel.characterColumn.get();

            int rowChange = Integer.compare(targetRow, currentRow);
            int columnChange = Integer.compare(targetColumn, currentColumn);

            viewModel.moveCharacter(rowChange, columnChange);
        });
    }

    @FXML
    public void solveMaze() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Solve Maze");
        alert.setHeaderText("Return to Start?");
        alert.setContentText("""
                The robot must return to the charging station
                before calculating the optimal route.
                
                Return to the starting position?
            """);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            viewModel.solveMaze();
        }
    }

    @FXML
    public void generateMaze() {
        try {
            gameFinished = false;

            int rows = askForNumber("Rows", "Enter number of rows:");
            int columns = askForNumber("Columns", "Enter number of columns:");

            viewModel.generateMaze(rows, columns);

            welcomePane.setVisible(false);
            welcomePane.setManaged(false);

            mazeDisplayer.setVisible(true);
            mazeDisplayer.setManaged(true);
            mazeDisplayer.toFront();
            mazeDisplayer.redraw();

        } catch (Exception e) {
            displayError("Invalid maze size.");
        }
    }

    private int askForNumber(String title, String message) {
        TextInputDialog dialog = new TextInputDialog("15");
        dialog.setTitle(title);
        dialog.setHeaderText(message);
        dialog.setContentText("Value:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) {
            throw new IllegalArgumentException();
        }

        int value = Integer.parseInt(result.get());

        if (value < 2) {
            throw new IllegalArgumentException();
        }

        return value;
    }


    @FXML
    public void saveMaze() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Maze");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Maze files", "*.maze")
        );

        File file = fileChooser.showSaveDialog(mazeDisplayer.getScene().getWindow());

        if (file != null) {
            viewModel.saveMaze(file);
        }
    }

    @FXML
    public void loadMaze() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Maze");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Maze files", "*.maze")
        );

        File file = fileChooser.showOpenDialog(mazeDisplayer.getScene().getWindow());

        if (file != null) {
            viewModel.loadMaze(file);
        }
    }

    public void keyPressed(KeyEvent keyEvent) {
        System.out.println("Pressed: " + keyEvent.getCode());

        switch (keyEvent.getCode()) {

            // למעלה
            case NUMPAD8, DIGIT8 ->
                    viewModel.moveCharacter(-1, 0);

            // למטה
            case NUMPAD2, DIGIT2 ->
                    viewModel.moveCharacter(1, 0);

            // ימינה
            case NUMPAD6, DIGIT6 -> {
                mazeDisplayer.setFacingRight(true);
                viewModel.moveCharacter(0, 1);
            }

            // שמאלה
            case NUMPAD4, DIGIT4 -> {
                mazeDisplayer.setFacingRight(false);
                viewModel.moveCharacter(0, -1);
            }

            // אלכסון ימין-למעלה
            case NUMPAD9, DIGIT9 -> {
                mazeDisplayer.setFacingRight(true);
                viewModel.moveCharacter(-1, 1);
            }

            // אלכסון ימין-למטה
            case NUMPAD3, DIGIT3 -> {
                mazeDisplayer.setFacingRight(true);
                viewModel.moveCharacter(1, 1);
            }

            // אלכסון שמאל-למעלה
            case NUMPAD7, DIGIT7 -> {
                mazeDisplayer.setFacingRight(false);
                viewModel.moveCharacter(-1, -1);
            }

            // אלכסון שמאל-למטה
            case NUMPAD1, DIGIT1 -> {
                mazeDisplayer.setFacingRight(false);
                viewModel.moveCharacter(1, -1);
            }
        }

        keyEvent.consume();
    }

    @Override
    public void displayMaze() {
        mazeDisplayer.setMaze(viewModel.getMaze());
        mazeDisplayer.setCharacterPosition(
                viewModel.characterRow.get(),
                viewModel.characterColumn.get()
        );
    }

    @Override
    public void displaySolution() {
        mazeDisplayer.setSolution(viewModel.getSolution());
    }

    @Override
    public void displayError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void displayWinMessage() {
        Stage stage = new Stage();

        Image image = new Image(getClass().getResourceAsStream("/Images/success.png"));
        ImageView imageView = new ImageView(image);

        imageView.setPreserveRatio(true);
        imageView.setFitWidth(700);

        StackPane root = new StackPane(imageView);

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("Congratulations!");
        stage.show();
    }

    @FXML
    public void exit() {
        Platform.exit();
    }

    @FXML
    public void showProperties() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Properties");
        alert.setHeaderText("Application Properties");
        alert.setContentText("""
            Maze generating algorithm: MyMazeGenerator
            Maze searching algorithm: BestFirstSearch
            Theme: Robot Vacuum - Apartment Escape
            Movement: NumPad only
            """);
        alert.showAndWait();
    }

    @FXML
    public void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("How to play");
        alert.setContentText("""
            Generate a maze from File -> New.
            
            Move the robot using NumPad:
            8 = up
            2 = down
            4 = left
            6 = right
            7, 9, 1, 3 = diagonals
            
            Use Maze -> Solve to show wheel marks that help you reach the goal.
            """);
        alert.showAndWait();
    }

    @Override
    public void update(Observable o, Object arg) {
        displayMaze();
        displaySolution();

        if (!gameFinished && viewModel.isGoalReached()) {
            gameFinished = true;
            displayWinMessage();
        }
    }

    @FXML
    public void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Maze Game");
        alert.setContentText("""
            ATP Project - Part C
            
            Authors:
            Karni & Ziv
            ...
        """);

        alert.showAndWait();
    }

    public void mouseScrolled(javafx.scene.input.ScrollEvent event) {
        if (event.isControlDown()) {
            if (event.getDeltaY() > 0) {
                zoom *= 1.1;
            } else {
                zoom /= 1.1;
            }

            mazeDisplayer.setScaleX(zoom);
            mazeDisplayer.setScaleY(zoom);

            event.consume();
        }
    }
}