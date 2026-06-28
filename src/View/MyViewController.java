package View;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

/**
 * Controller for the main application view.
 * Handles user actions, updates the maze display,
 * and communicates with the ViewModel.
 */
public class MyViewController implements IView, Observer {

    @FXML private MazeDisplayer mazeDisplayer;
    @FXML private StackPane mazePane;
    @FXML private StackPane welcomePane;

    private MyViewModel viewModel;
    private static final Logger logger = LogManager.getLogger(MyViewController.class);

    private boolean gameFinished = false;
    private double zoom = 1.0;

    private static final List<String> WALL_SOUNDS = List.of(
            "Brother, I'm not allowed to drink glass, brother.m4a",
            "I'm on a dust diet.m4a"
    );

    private static final List<String> FINISH_SOUNDS = List.of(
            "The kitchen is sparkling and sparkling for you, sir.m4a",
            "The kitchen is dust free.m4a"
    );

    /**
     * Sets the ViewModel used by this controller
     * and registers the controller as an observer.
     *
     * @param viewModel the ViewModel connected to this view
     */
    public void setViewModel(MyViewModel viewModel) {
        this.viewModel = viewModel;
        this.viewModel.addObserver(this);
    }

    /**
     * Initializes the view after the FXML file is loaded.
     * Binds the maze display size to its pane, sets the welcome screen,
     * and registers mouse dragging behavior.
     */
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

            boolean moved = viewModel.moveCharacter(rowChange, columnChange);
            if (!moved) {
                SoundManager.playRandomEffect(WALL_SOUNDS);
            }
        });

        SoundManager.playEffect("start.m4a");
    }

    /**
     * Asks the user for confirmation and then requests
     * the maze solution from the ViewModel.
     */
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
            SoundManager.stopBackground();
            displayLoseMessage();
            SoundManager.playBackground("background.m4a");

            logger.info("User requested maze solution");

            viewModel.solveMaze();
        }
    }

    /**
     * Creates a new maze according to dimensions entered by the user.
     * Also switches the screen from the welcome pane to the maze display.
     */
    @FXML
    public void generateMaze() {
        try {
            gameFinished = false;

            int rows = askForNumber("Rows", "Enter number of rows:");
            int columns = askForNumber("Columns", "Enter number of columns:");

            SoundManager.playEffect("lets eat some dust.m4a");

            PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
            pause.setOnFinished(e -> SoundManager.playBackground("background.m4a"));
            pause.play();

            viewModel.generateMaze(rows, columns);

            logger.info("Generated maze: {} rows x {} columns", rows, columns);

            welcomePane.setVisible(false);
            welcomePane.setManaged(false);

            mazeDisplayer.setVisible(true);
            mazeDisplayer.setManaged(true);
            mazeDisplayer.toFront();
            mazeDisplayer.redraw();

        } catch (Exception e) {
            logger.warn("User entered invalid maze size", e);
            displayError("Invalid maze size.");
        }
    }

    /**
     * Opens a dialog and reads a valid maze size value.
     *
     * @param title dialog title
     * @param message message displayed to the user
     * @return the number entered by the user
     */
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

    /**
     * Opens a file chooser and saves the current maze.
     */
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
            logger.info("Maze saved to {}", file.getAbsolutePath());
        }
    }

    /**
     * Opens a file chooser and loads a maze from a file.
     */
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
            logger.info("Maze loaded from {}", file.getAbsolutePath());
        }
    }

    /**
     * Handles keyboard movement using NumPad and digit keys.
     *
     * @param keyEvent the keyboard event
     */
    public void keyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case NUMPAD8, DIGIT8 -> moveCharacterWithSound(-1, 0);
            case NUMPAD2, DIGIT2 -> moveCharacterWithSound(1, 0);

            case NUMPAD6, DIGIT6 -> {
                mazeDisplayer.setFacingRight(true);
                moveCharacterWithSound(0, 1);
            }

            case NUMPAD4, DIGIT4 -> {
                mazeDisplayer.setFacingRight(false);
                moveCharacterWithSound(0, -1);
            }

            case NUMPAD9, DIGIT9 -> {
                mazeDisplayer.setFacingRight(true);
                moveCharacterWithSound(-1, 1);
            }

            case NUMPAD3, DIGIT3 -> {
                mazeDisplayer.setFacingRight(true);
                moveCharacterWithSound(1, 1);
            }

            case NUMPAD7, DIGIT7 -> {
                mazeDisplayer.setFacingRight(false);
                moveCharacterWithSound(-1, -1);
            }

            case NUMPAD1, DIGIT1 -> {
                mazeDisplayer.setFacingRight(false);
                moveCharacterWithSound(1, -1);
            }
        }

        keyEvent.consume();
    }

    /**
     * Updates the maze and character position on the screen.
     */
    @Override
    public void displayMaze() {
        mazeDisplayer.setMaze(viewModel.getMaze());
        mazeDisplayer.setCharacterPosition(
                viewModel.characterRow.get(),
                viewModel.characterColumn.get()
        );
    }

    /**
     * Shows the solution path on the maze display.
     */
    @Override
    public void displaySolution() {
        mazeDisplayer.setSolution(viewModel.getSolution());
    }

    /**
     * Displays an error dialog.
     *
     * @param message error message to display
     */
    @Override
    public void displayError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows the winning screen and plays the success sound.
     */
    @Override
    public void displayWinMessage() {
        SoundManager.playEffect("totah.m4a");

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

    /**
     * Shows a losing message when the user asks to solve the maze.
     */
    public void displayLoseMessage() {
        SoundManager.playEffect("what a loser.m4a");

        Stage stage = new Stage();

        Image image = new Image(getClass().getResourceAsStream("/Images/loser.png"));
        ImageView imageView = new ImageView(image);

        imageView.setPreserveRatio(true);
        imageView.setFitWidth(700);

        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("You Lost!");
        stage.showAndWait();
    }

    /**
     * Exits the application.
     */
    @FXML
    public void exit() {
        Platform.exit();
    }

    /**
     * Displays general application properties.
     */
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

    /**
     * Displays the game instructions.
     */
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

    /**
     * Called when the ViewModel notifies about a change.
     * Refreshes the maze and checks whether the goal was reached.
     *
     * @param o observed object
     * @param arg optional update argument
     */
    @Override
    public void update(Observable o, Object arg) {
        displayMaze();
        displaySolution();

        if (!gameFinished && viewModel.isGoalReached()) {
            gameFinished = true;

            logger.info("Player reached the goal");

            SoundManager.stopBackground();
            SoundManager.playRandomEffectAndThen(FINISH_SOUNDS, this::displayWinMessage);
        }
    }

    /**
     * Displays information about the project and authors.
     */
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

    /**
     * Handles zooming in and out using Ctrl + mouse scroll.
     *
     * @param event mouse scroll event
     */
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

    /**
     * Attempts to move the character and plays a wall sound
     * if the movement is not allowed.
     *
     * @param rowChange row movement
     * @param colChange column movement
     */
    private void moveCharacterWithSound(int rowChange, int colChange) {
        if (!viewModel.moveCharacter(rowChange, colChange)) {
            SoundManager.playRandomEffectWithCooldown(WALL_SOUNDS, 900);
        }
    }
}