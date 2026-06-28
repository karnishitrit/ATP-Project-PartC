package Model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import algorithms.mazeGenerators.Maze;
import Client.Client;
import Client.IClientStrategy;
import IO.MyDecompressorInputStream;
import Server.Server;
import Server.ServerStrategyGenerateMaze;
import Server.ServerStrategySolveSearchProblem;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import algorithms.search.Solution;

import java.io.*;
import java.util.Observable;

public class MyModel  extends Observable implements IModel {
    private Maze maze;
    private Solution solution;
    private Server mazeGeneratingServer;
    private Server solveSearchProblemServer;
    private int characterRow;
    private int characterColumn;
    private static final Logger logger = LogManager.getLogger(MyModel.class);

    @Override
    public void generateMaze(int rows, int columns) {

        try {

            Client client = new Client(InetAddress.getLocalHost(), 5400, new IClientStrategy() {

                @Override
                public void clientStrategy(InputStream inFromServer, OutputStream outToServer) {

                    try {

                        ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                        ObjectInputStream fromServer = new ObjectInputStream(inFromServer);

                        toServer.flush();

                        int[] mazeDimensions = new int[]{rows, columns};

                        toServer.writeObject(mazeDimensions);
                        toServer.flush();

                        byte[] compressedMaze = (byte[]) fromServer.readObject();

                        InputStream is = new MyDecompressorInputStream(
                                new ByteArrayInputStream(compressedMaze));

                        byte[] decompressedMaze = new byte[rows * columns + 24];

                        is.read(decompressedMaze);

                        maze = new Maze(decompressedMaze);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            logger.info(
                    "Maze generation request | Client IP: {} | Server: {} | Maze size: {}x{}",
                    InetAddress.getLocalHost().getHostAddress(),
                    5400,
                    rows,
                    columns
            );

            client.communicateWithServer();

            logger.info("Maze received successfully from server");


            solution = null;

            characterRow = maze.getStartPosition().getRowIndex();
            characterColumn = maze.getStartPosition().getColumnIndex();

            setChanged();
            notifyObservers();

        } catch (Exception e) {

            logger.error("Failed to generate maze", e);
        }
    }

    @Override
    public Maze getMaze() {
        return maze;
    }

    @Override
    public void solveMaze() {

        if (maze == null)
            return;

        try {

            Client client = new Client(InetAddress.getLocalHost(), 5401, new IClientStrategy() {

                @Override
                public void clientStrategy(InputStream inFromServer, OutputStream outToServer) {

                    try {

                        ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                        ObjectInputStream fromServer = new ObjectInputStream(inFromServer);

                        toServer.flush();

                        toServer.writeObject(maze);
                        toServer.flush();

                        solution = (Solution) fromServer.readObject();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            logger.info("Started solving maze");

            logger.info(
                    "Maze solving request | Client IP: {} | Server: {} | Maze size: {}x{}",
                    InetAddress.getLocalHost().getHostAddress(),
                    5401,
                    maze.getMaze().length,
                    maze.getMaze()[0].length
            );

            client.communicateWithServer();

            characterRow = maze.getStartPosition().getRowIndex();
            characterColumn = maze.getStartPosition().getColumnIndex();

            logger.info("Solution received successfully from server");

            setChanged();
            notifyObservers();

        } catch (Exception e) {

            logger.error("Failed to solve maze", e);
        }
    }

    @Override
    public Solution getSolution() {
        return solution;
    }

    @Override
    public boolean moveCharacter(int rowChange, int columnChange) {
        if (maze == null)
            return false;

        int newRow = characterRow + rowChange;
        int newColumn = characterColumn + columnChange;

        if (canMoveTo(newRow, newColumn, rowChange, columnChange)) {
            characterRow = newRow;
            characterColumn = newColumn;

            setChanged();
            notifyObservers();

            return true;
        }
        return false;
    }

    private boolean canMoveTo(int row, int column, int rowChange, int columnChange) {
        int[][] map = maze.getMaze();

        if (row < 0 || row >= map.length || column < 0 || column >= map[0].length)
            return false;

        if (map[row][column] == 1)
            return false;

        boolean isDiagonal = rowChange != 0 && columnChange != 0;

        if (isDiagonal) {
            int sideRow = characterRow + rowChange;
            int sideColumn = characterColumn;

            int otherSideRow = characterRow;
            int otherSideColumn = characterColumn + columnChange;

            return map[sideRow][sideColumn] == 0 &&
                    map[otherSideRow][otherSideColumn] == 0;
        }

        return true;
    }
    @Override
    public int getCharacterRow() {
        return characterRow;
    }

    @Override
    public boolean isGoalReached() {
        if (maze == null)
            return false;

        return characterRow == maze.getGoalPosition().getRowIndex()
                && characterColumn == maze.getGoalPosition().getColumnIndex();
    }

    @Override
    public int getCharacterColumn() {
        return characterColumn;
    }

    @Override
    public void saveMaze(File file) {
        if (maze == null || file == null)
            return;

        logger.info(
                "Saving maze | File={} | Maze size={}x{}",
                file.getAbsolutePath(),
                maze.getMaze().length,
                maze.getMaze()[0].length
        );

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {


            fileOutputStream.write(maze.toByteArray());

            logger.info("Maze saved successfully");

        } catch (IOException e) {
            e.printStackTrace();

            logger.error("Failed to save maze", e);
        }
    }


    @Override
    public void loadMaze(File file) {
        if (file == null)
            return;

        logger.info(
                "Loading maze | File={}",
                file.getAbsolutePath()
        );
        try (FileInputStream fileInputStream = new FileInputStream(file)) {

            byte[] mazeBytes = fileInputStream.readAllBytes();
            maze = new Maze(mazeBytes);

            characterRow = maze.getStartPosition().getRowIndex();
            characterColumn = maze.getStartPosition().getColumnIndex();
            solution = null;

            logger.info(
                    "Maze loaded successfully | Maze size={}x{}",
                    maze.getMaze().length,
                    maze.getMaze()[0].length
            );

            setChanged();
            notifyObservers();

        } catch (IOException e) {
            e.printStackTrace();

            logger.error("Failed to load maze", e);
        }
    }

    @Override
    public void startServers() {

        mazeGeneratingServer = new Server(
                5400,
                1000,
                new ServerStrategyGenerateMaze());

        solveSearchProblemServer = new Server(
                5401,
                1000,
                new ServerStrategySolveSearchProblem());

        mazeGeneratingServer.start();
        solveSearchProblemServer.start();

        logger.info("Maze servers started");
    }

    @Override
    public void stopServers() {

        if (mazeGeneratingServer != null) {
            mazeGeneratingServer.stop();
        }

        if (solveSearchProblemServer != null) {
            solveSearchProblemServer.stop();
        }

        logger.info("Maze servers stopped");
    }
}
