package Model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import algorithms.mazeGenerators.Maze;
import algorithms.search.Solution;

import Client.Client;
import IO.MyCompressorOutputStream;
import IO.MyDecompressorInputStream;
import Server.Server;
import Server.ServerStrategyGenerateMaze;
import Server.ServerStrategySolveSearchProblem;

import java.net.InetAddress;

import java.io.*;
import java.util.Observable;

public class MyModel  extends Observable implements IModel {
    private Maze maze;

    private Solution solution;
    private int characterRow;
    private int characterColumn;

    private static final Logger logger = LogManager.getLogger(MyModel.class);

    private Server mazeGeneratingServer;
    private Server solveSearchProblemServer;

    private static final int MAZE_SERVER_PORT = 5400;
    private static final int SOLVE_SERVER_PORT = 5401;


    @Override
    public Maze getMaze() {
        return maze;
    }

    @Override
    public Solution getSolution() {
        return solution;
    }

    @Override
    public void generateMaze(int rows, int columns) {

        maze = generateMazeFromServer(rows, columns);

        if (maze == null)
            return;

        logger.info("Maze generated successfully trough server");

        solution = null;
        characterRow = maze.getStartPosition().getRowIndex();
        characterColumn = maze.getStartPosition().getColumnIndex();

        setChanged();
        notifyObservers();
    }

    @Override
    public void solveMaze() {
        if (maze == null)
            return;

        characterRow = maze.getStartPosition().getRowIndex();
        characterColumn = maze.getStartPosition().getColumnIndex();

        solution = solveMazeFromServer();

        if (solution == null) {
            logger.error("Maze solving failed");
            return;
        }

        logger.info("Maze solved successfully through server");

        setChanged();
        notifyObservers();
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

        try (OutputStream out = new MyCompressorOutputStream(new FileOutputStream(file))) {
            out.write(maze.toByteArray());
            out.flush();

            logger.info("Maze saved successfully with compression");

        } catch (IOException e) {
            logger.error("Failed to save maze", e);
        }
    }

    @Override
    public void loadMaze(File file) {
        if (file == null || !file.exists())
            return;

        try {
            byte[] mazeBytes = tryLoadCompressed(file);

            try {
                maze = new Maze(mazeBytes);
            } catch (Exception compressedFailed) {
                logger.warn("Compressed load failed, trying regular load", compressedFailed);
                maze = new Maze(readAllBytes(file));
            }

            characterRow = maze.getStartPosition().getRowIndex();
            characterColumn = maze.getStartPosition().getColumnIndex();
            solution = null;

            logger.info("Maze loaded successfully");

            setChanged();
            notifyObservers();

        } catch (Exception e) {
            logger.error("Failed to load maze", e);
        }
    }

    private byte[] tryLoadCompressed(File file) throws IOException {
        try (InputStream in = new MyDecompressorInputStream(new FileInputStream(file))) {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

            int currentByte;
            while ((currentByte = in.read()) != -1) {
                byteOutputStream.write(currentByte);
            }

            return byteOutputStream.toByteArray();
        }
    }

    private byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return fileInputStream.readAllBytes();
        }
    }

    @Override
    public void startServers() {
        if (mazeGeneratingServer != null || solveSearchProblemServer != null)
            return;

        mazeGeneratingServer = new Server(MAZE_SERVER_PORT, 1000, new ServerStrategyGenerateMaze());
        solveSearchProblemServer = new Server(SOLVE_SERVER_PORT, 1000, new ServerStrategySolveSearchProblem());

        mazeGeneratingServer.start();
        solveSearchProblemServer.start();

        logger.info("Servers started successfully");
    }


    @Override
    public void stopServers() {
        if (mazeGeneratingServer != null)
            mazeGeneratingServer.stop();

        if (solveSearchProblemServer != null)
            solveSearchProblemServer.stop();

        logger.info("Servers stopped successfully");
    }

    private Maze generateMazeFromServer(int rows, int columns) {
        final Maze[] result = new Maze[1];

        try {
            Client client = new Client(InetAddress.getLocalHost(), MAZE_SERVER_PORT, (inFromServer, outToServer) -> {
                try {
                    ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                    ObjectInputStream fromServer = new ObjectInputStream(inFromServer);

                    toServer.flush();
                    toServer.writeObject(new int[]{rows, columns});
                    toServer.flush();

                    byte[] compressedMaze = (byte[]) fromServer.readObject();

                    InputStream decompressor = new MyDecompressorInputStream(new ByteArrayInputStream(compressedMaze));

                    byte[] decompressedMaze = new byte[rows * columns + 100];
                    decompressor.read(decompressedMaze);

                    result[0] = new Maze(decompressedMaze);

                    decompressor.close();
                    fromServer.close();
                    toServer.close();

                } catch (Exception e) {
                    logger.error("Failed to generate maze through server", e);
                }
            });

            client.communicateWithServer();

        } catch (Exception e) {
            logger.error("Failed to connect to maze generation server", e);
        }

        return result[0];
    }

    private Solution solveMazeFromServer() {
        final Solution[] result = new Solution[1];

        try {
            Client client = new Client(InetAddress.getLocalHost(), SOLVE_SERVER_PORT, (inFromServer, outToServer) -> {
                try {
                    ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                    ObjectInputStream fromServer = new ObjectInputStream(inFromServer);

                    toServer.flush();
                    toServer.writeObject(maze);
                    toServer.flush();

                    result[0] = (Solution) fromServer.readObject();

                    fromServer.close();
                    toServer.close();

                } catch (Exception e) {
                    logger.error("Failed to solve maze through server", e);
                }
            });

            client.communicateWithServer();

        } catch (Exception e) {
            logger.error("Failed to connect to solve server", e);
        }

        return result[0];
    }
}
