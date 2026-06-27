package Model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.MyMazeGenerator;
import algorithms.search.BestFirstSearch;
import algorithms.search.ISearchingAlgorithm;
import algorithms.search.SearchableMaze;
import algorithms.search.Solution;

import java.io.*;
import java.util.Observable;

public class MyModel  extends Observable implements IModel {
    private Maze maze;
    private Solution solution;
    private int characterRow;
    private int characterColumn;
    private static final Logger logger = LogManager.getLogger(MyModel.class);

    @Override
    public void generateMaze(int rows, int columns) {
        maze = new MyMazeGenerator().generate(rows, columns);

        logger.info("Maze generated successfully");

        solution = null;
        characterRow = maze.getStartPosition().getRowIndex();
        characterColumn = maze.getStartPosition().getColumnIndex();
        setChanged();
        notifyObservers();
    }

    @Override
    public Maze getMaze() {
        return maze;
    }

    @Override
    public void solveMaze() {
        if (maze == null)
            return;

        logger.info("Started solving maze");

        characterRow = maze.getStartPosition().getRowIndex();
        characterColumn = maze.getStartPosition().getColumnIndex();

        SearchableMaze searchableMaze = new SearchableMaze(maze);
        ISearchingAlgorithm searcher = new BestFirstSearch();
        solution = searcher.solve(searchableMaze);

        logger.info("Maze solved successfully");

        setChanged();
        notifyObservers();
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

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] mazeBytes = fileInputStream.readAllBytes();
            maze = new Maze(mazeBytes);

            characterRow = maze.getStartPosition().getRowIndex();
            characterColumn = maze.getStartPosition().getColumnIndex();
            solution = null;

            logger.info("Maze loaded successfully");

            setChanged();
            notifyObservers();

        } catch (IOException e) {
            e.printStackTrace();

            logger.error("Failed to load maze", e);
        }
    }

    @Override
    public void startServers() {
        // Later we will start the maze generation and solving servers here.
    }

    @Override
    public void stopServers() {
        // Later we will stop all servers here in a clean way.
    }
}
