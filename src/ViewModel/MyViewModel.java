package ViewModel;

import Model.IModel;
import algorithms.mazeGenerators.Maze;
import algorithms.search.Solution;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

public class MyViewModel extends Observable implements Observer {
    private final IModel model;

    public final IntegerProperty characterRow = new SimpleIntegerProperty();
    public final IntegerProperty characterColumn = new SimpleIntegerProperty();

    public MyViewModel(IModel model) {
        this.model = model;
        ((Observable) model).addObserver(this);
    }

    public void generateMaze(int rows, int columns) {
        model.generateMaze(rows, columns);
        updateCharacterPosition();
    }

    public Maze getMaze() {
        return model.getMaze();
    }

    public void solveMaze() {
        model.solveMaze();
    }

    public Solution getSolution() {
        return model.getSolution();
    }

    public void moveCharacter(int rowChange, int columnChange) {
        model.moveCharacter(rowChange, columnChange);
        updateCharacterPosition();
    }

    public boolean isGoalReached() {
        return model.isGoalReached();
    }

    private void updateCharacterPosition() {
        characterRow.set(model.getCharacterRow());
        characterColumn.set(model.getCharacterColumn());
    }


    public void saveMaze(File file) {
        model.saveMaze(file);
    }

    public void loadMaze(File file) {
        model.loadMaze(file);
        updateCharacterPosition();
    }

    public void startServers() {
        model.startServers();
    }

    public void stopServers() {
        model.stopServers();
    }


    @Override
    public void update(Observable o, Object arg) {
        updateCharacterPosition();

        setChanged();
        notifyObservers();
    }
}
