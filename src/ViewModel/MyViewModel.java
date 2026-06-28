package ViewModel;

import Model.IModel;
import algorithms.mazeGenerators.Maze;
import algorithms.search.Solution;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

/**
 * ViewModel in the MVVM architecture.
 * Acts as a bridge between the View and the Model,
 * forwarding user requests and exposing observable data.
 */
public class MyViewModel extends Observable implements Observer {

    private final IModel model;

    public final IntegerProperty characterRow = new SimpleIntegerProperty();
    public final IntegerProperty characterColumn = new SimpleIntegerProperty();

    /**
     * Creates a new ViewModel and registers it
     * as an observer of the Model.
     *
     * @param model application model
     */
    public MyViewModel(IModel model) {
        this.model = model;
        ((Observable) model).addObserver(this);
    }

    /**
     * Requests the Model to generate a new maze.
     *
     * @param rows maze rows
     * @param columns maze columns
     */
    public void generateMaze(int rows, int columns) {
        model.generateMaze(rows, columns);
        updateCharacterPosition();
    }

    /**
     * Returns the current maze.
     *
     * @return current maze
     */
    public Maze getMaze() {
        return model.getMaze();
    }

    /**
     * Requests the Model to solve the current maze.
     */
    public void solveMaze() {
        model.solveMaze();
    }

    /**
     * Returns the current maze solution.
     *
     * @return maze solution
     */
    public Solution getSolution() {
        return model.getSolution();
    }

    /**
     * Attempts to move the character.
     *
     * @param rowChange row movement
     * @param columnChange column movement
     * @return true if the move succeeded
     */
    public boolean moveCharacter(int rowChange, int columnChange) {
        boolean moved = model.moveCharacter(rowChange, columnChange);
        updateCharacterPosition();
        return moved;
    }

    /**
     * Checks whether the goal has been reached.
     *
     * @return true if the character reached the goal
     */
    public boolean isGoalReached() {
        return model.isGoalReached();
    }

    /**
     * Updates the observable character position
     * according to the Model.
     */
    private void updateCharacterPosition() {
        characterRow.set(model.getCharacterRow());
        characterColumn.set(model.getCharacterColumn());
    }

    /**
     * Saves the current maze to a file.
     *
     * @param file destination file
     */
    public void saveMaze(File file) {
        model.saveMaze(file);
    }

    /**
     * Loads a maze from a file.
     *
     * @param file source file
     */
    public void loadMaze(File file) {
        model.loadMaze(file);
        updateCharacterPosition();
    }

    /**
     * Receives updates from the Model,
     * refreshes the character position,
     * and notifies the View.
     *
     * @param o observed object
     * @param arg update argument
     */
    @Override
    public void update(Observable o, Object arg) {
        updateCharacterPosition();

        setChanged();
        notifyObservers();
    }
}
