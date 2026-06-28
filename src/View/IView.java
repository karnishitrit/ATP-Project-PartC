package View;

public interface IView {
    void displayMaze();
    void displaySolution();
    void displayError(String message);
    void displayWinMessage();

    void saveMaze();
    void loadMaze();
}
