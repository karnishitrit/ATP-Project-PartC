package Model;

import algorithms.mazeGenerators.Maze;
import algorithms.search.Solution;

import java.io.File;

public interface IModel {
    void generateMaze(int rows, int columns);
    Maze getMaze();

    void solveMaze();
    Solution getSolution();

    void moveCharacter(int rowChange, int columnChange);
    int getCharacterRow();
    int getCharacterColumn();

    void saveMaze(File file);
    void loadMaze(File file);

    void startServers();
    void stopServers();

    boolean isGoalReached();
}
