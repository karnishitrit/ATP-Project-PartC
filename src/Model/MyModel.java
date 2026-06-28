package Model;

import Client.Client;
import Client.IClientStrategy;
import IO.MyDecompressorInputStream;
import Server.Server;
import Server.ServerStrategyGenerateMaze;
import Server.ServerStrategySolveSearchProblem;
import algorithms.mazeGenerators.Maze;
import algorithms.search.Solution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.util.Observable;

/**
 * Model implementation in the MVVM architecture.
 * Responsible for communicating with the maze servers,
 * managing the current maze and solution, handling
 * player movement, and notifying observers about changes.
 */
public class MyModel extends Observable implements IModel {
    private static final Logger logger = LogManager.getLogger(MyModel.class);

    private static final int MAZE_SERVER_PORT = 5400;
    private static final int SOLVER_SERVER_PORT = 5401;
    private static final int SERVER_TIMEOUT = 1000;

    private Maze maze;
    private Solution solution;
    private Server mazeGeneratingServer;
    private Server solveSearchProblemServer;
    private int characterRow;
    private int characterColumn;

    /**
     * Requests a new maze from the maze generation server.
     * After receiving and decompressing the maze, the character
     * is positioned at the start cell and observers are notified.
     *
     * @param rows number of maze rows
     * @param columns number of maze columns
     */
    @Override
    public void generateMaze(int rows, int columns) {
        try {
            Client client = new Client(InetAddress.getLocalHost(), MAZE_SERVER_PORT, createMazeGenerationStrategy(rows, columns));

            logger.info("Requesting maze generation: {}x{}", rows, columns);

            client.communicateWithServer();

            resetCharacterToStart();
            solution = null;

            updateView();

            logger.info("Maze generated successfully");

        } catch (Exception e) {
            logger.error("Could not generate maze", e);
        }
    }

    /**
     * Creates the client strategy used to communicate with
     * the maze generation server.
     * The strategy sends the requested maze dimensions and
     * receives the compressed maze from the server.
     *
     * @param rows requested maze rows
     * @param columns requested maze columns
     * @return client communication strategy
     */
    private IClientStrategy createMazeGenerationStrategy(int rows, int columns) {
        return (inFromServer, outToServer) -> {
            try {
                ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                ObjectInputStream fromServer = new ObjectInputStream(inFromServer);

                toServer.flush();
                toServer.writeObject(new int[]{rows, columns});
                toServer.flush();

                byte[] compressedMaze = (byte[]) fromServer.readObject();
                maze = decompressMaze(compressedMaze, rows, columns);

            } catch (Exception e) {
                logger.error("Error while receiving maze from server", e);
            }
        };
    }

    /**
     * Decompresses the maze data received from the server
     * and creates a Maze object.
     *
     * @param compressedMaze compressed maze bytes
     * @param rows maze rows
     * @param columns maze columns
     * @return reconstructed maze
     * @throws IOException if decompression fails
     */
    private Maze decompressMaze(byte[] compressedMaze, int rows, int columns) throws IOException {
        InputStream inputStream = new MyDecompressorInputStream(
                new ByteArrayInputStream(compressedMaze)
        );

        byte[] mazeBytes = new byte[rows * columns + 24];
        inputStream.read(mazeBytes);

        return new Maze(mazeBytes);
    }

    /**
     * Sends the current maze to the solving server and
     * receives its solution.
     * After receiving the solution, observers are notified.
     */
    @Override
    public void solveMaze() {
        if (maze == null) {
            return;
        }

        try {
            Client client = new Client(InetAddress.getLocalHost(), SOLVER_SERVER_PORT, createMazeSolvingStrategy());

            logger.info("Requesting maze solution");

            client.communicateWithServer();

            resetCharacterToStart();
            updateView();

            logger.info("Maze solution received");

        } catch (Exception e) {
            logger.error("Could not solve maze", e);
        }
    }

    /**
     * Creates the client strategy used to communicate with
     * the maze solving server.
     * The strategy sends the maze and receives its solution.
     *
     * @return client communication strategy
     */
    private IClientStrategy createMazeSolvingStrategy() {
        return (inFromServer, outToServer) -> {
            try {
                ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                ObjectInputStream fromServer = new ObjectInputStream(inFromServer);

                toServer.flush();
                toServer.writeObject(maze);
                toServer.flush();

                solution = (Solution) fromServer.readObject();

            } catch (Exception e) {
                logger.error("Error while receiving solution from server", e);
            }
        };
    }

    /**
     * Attempts to move the character according to the
     * requested row and column offsets.
     *
     * @param rowChange row movement
     * @param columnChange column movement
     * @return true if the movement was successful
     */
    @Override
    public boolean moveCharacter(int rowChange, int columnChange) {
        if (maze == null) {
            return false;
        }

        int nextRow = characterRow + rowChange;
        int nextColumn = characterColumn + columnChange;

        if (!isMoveAllowed(nextRow, nextColumn, rowChange, columnChange)) {
            return false;
        }

        characterRow = nextRow;
        characterColumn = nextColumn;

        updateView();
        return true;
    }

    /**
     * Checks whether the requested movement is legal.
     *
     * @param row destination row
     * @param column destination column
     * @param rowChange row offset
     * @param columnChange column offset
     * @return true if the destination cell can be reached
     */
    private boolean isMoveAllowed(int row, int column, int rowChange, int columnChange) {
        int[][] mazeMap = maze.getMaze();

        if (row < 0 || row >= mazeMap.length || column < 0 || column >= mazeMap[0].length) {
            return false;
        }

        if (mazeMap[row][column] == 1) {
            return false;
        }

        if ( rowChange != 0 && columnChange != 0) {
            return canMoveDiagonally(rowChange, columnChange, mazeMap);
        }

        return true;
    }

    /**
     * Checks whether diagonal movement is possible
     * without crossing maze walls.
     *
     * @param rowChange row offset
     * @param columnChange column offset
     * @param mazeMap maze grid
     * @return true if the diagonal movement is valid
     */
    private boolean canMoveDiagonally(int rowChange, int columnChange, int[][] mazeMap) {
        int verticalCell = mazeMap[characterRow + rowChange][characterColumn];
        int horizontalCell = mazeMap[characterRow][characterColumn + columnChange];

        return verticalCell == 0 || horizontalCell == 0;
    }

    /**
     * Saves the current maze to the given file.
     *
     * @param file destination file
     */
    @Override
    public void saveMaze(File file) {
        if (maze == null || file == null) {
            return;
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(maze.toByteArray());
            logger.info("Maze saved to {}", file.getAbsolutePath());

        } catch (IOException e) {
            logger.error("Could not save maze", e);
        }
    }

    /**
     * Loads a maze from a file and resets the
     * character position to the maze start.
     *
     * @param file source file
     */
    @Override
    public void loadMaze(File file) {
        if (file == null) {
            return;
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] mazeBytes = fileInputStream.readAllBytes();

            maze = new Maze(mazeBytes);
            solution = null;

            resetCharacterToStart();
            updateView();

            logger.info("Maze loaded from {}", file.getAbsolutePath());

        } catch (IOException e) {
            logger.error("Could not load maze", e);
        }
    }

    /**
     * Starts the maze generation and maze solving servers.
     */
    @Override
    public void startServers() {
        mazeGeneratingServer = new Server(MAZE_SERVER_PORT, SERVER_TIMEOUT, new ServerStrategyGenerateMaze());

        solveSearchProblemServer = new Server(SOLVER_SERVER_PORT, SERVER_TIMEOUT, new ServerStrategySolveSearchProblem());

        mazeGeneratingServer.start();
        solveSearchProblemServer.start();

        logger.info("Servers started");
    }

    /**
     * Stops all running maze servers.
     */
    @Override
    public void stopServers() {
        if (mazeGeneratingServer != null) {
            mazeGeneratingServer.stop();
        }

        if (solveSearchProblemServer != null) {
            solveSearchProblemServer.stop();
        }

        logger.info("Servers stopped");
    }

    /**
     * Moves the character to the maze starting position.
     */
    private void resetCharacterToStart() {
        if (maze == null) {
            return;
        }

        characterRow = maze.getStartPosition().getRowIndex();
        characterColumn = maze.getStartPosition().getColumnIndex();
    }

    /**
     * Marks the model as changed and notifies all observers.
     */
    private void updateView() {
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the current maze.
     *
     * @return current maze
     */
    @Override
    public Maze getMaze() {
        return maze;
    }

    /**
     * Returns the solution of the current maze.
     *
     * @return maze solution
     */
    @Override
    public Solution getSolution() {
        return solution;
    }

    /**
     * Returns the current character row.
     *
     * @return character row
     */
    @Override
    public int getCharacterRow() {
        return characterRow;
    }

    /**
     * Returns the current character column.
     *
     * @return character column
     */
    @Override
    public int getCharacterColumn() {
        return characterColumn;
    }

    /**
     * Checks whether the character has reached
     * the goal position.
     *
     * @return true if the goal has been reached
     */
    @Override
    public boolean isGoalReached() {
        return maze != null && characterRow == maze.getGoalPosition().getRowIndex() && characterColumn == maze.getGoalPosition().getColumnIndex();
    }
}