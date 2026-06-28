package View;

import algorithms.mazeGenerators.Maze;
import algorithms.search.AState;
import algorithms.search.Solution;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Custom canvas responsible for drawing the maze,
 * character, goal, walls, floor and solution path.
 */
public class MazeDisplayer extends Canvas {
    private Maze maze;
    private Solution solution;
    private int characterRow;
    private int characterColumn;

    private final Image robotRightImage = loadImage("/Images/robot_right.png");
    private final Image robotLeftImage = loadImage("/Images/robot_left.png");

    private boolean facingRight = true;

    private final Image floorImage = loadImage("/Images/floor.png");
    private final Image goalImage = loadImage("/Images/charging_station.png");

    private final Image[] wallImages = {
            loadImage("/Images/dust_small.png"),
            loadImage("/Images/dust_medium.png"),
            loadImage("/Images/dust_large.png"),
            loadImage("/Images/glass.png"),
            loadImage("/Images/crack.png"),
            loadImage("/Images/plant.png"),
            loadImage("/Images/water.png"),
            loadImage("/Images/cabinet.png")
    };

    /**
     * Loads an image resource from the project files.
     *
     * @param path image resource path
     * @return loaded image
     */
    private Image loadImage(String path) {
        return new Image(getClass().getResourceAsStream(path));
    }

    /**
     * Sets the maze to display and redraws the canvas.
     *
     * @param maze maze to display
     */
    public void setMaze(Maze maze) {
        this.maze = maze;
        redraw();
    }

    /**
     * Sets the maze solution and redraws it.
     *
     * @param solution solution path to display
     */
    public void setSolution(Solution solution) {
        this.solution = solution;
        redraw();
    }

    /**
     * Updates the character position on the maze.
     *
     * @param row character row
     * @param column character column
     */
    public void setCharacterPosition(int row, int column) {
        this.characterRow = row;
        this.characterColumn = column;
        redraw();
    }

    /**
     * Updates the direction the character is facing.
     *
     * @param facingRight true if the character faces right
     */
    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
        redraw();
    }

    /**
     * Redraws the entire maze canvas.
     */
    public void redraw() {
        if (maze == null)
            return;

        double canvasHeight = getHeight();
        double canvasWidth = getWidth();

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, canvasWidth, canvasHeight);

        int[][] map = maze.getMaze();

        double cellHeight = canvasHeight / map.length;
        double cellWidth = canvasWidth / map[0].length;

        drawFloor(gc, map, cellWidth, cellHeight);
        drawWallsAsObjects(gc, map, cellWidth, cellHeight);
        drawSolution(gc, cellWidth, cellHeight);
        drawGoal(gc, cellWidth, cellHeight);
        drawCharacter(gc, cellWidth, cellHeight);
    }

    /**
     * Draws the floor image on every maze cell.
     */
    private void drawFloor(GraphicsContext gc, int[][] map, double cellWidth, double cellHeight) {
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                gc.drawImage(
                        floorImage,
                        col * cellWidth,
                        row * cellHeight,
                        cellWidth,
                        cellHeight
                );
            }
        }
    }

    /**
     * Draws wall cells as visual objects.
     */
    private void drawWallsAsObjects(GraphicsContext gc, int[][] map, double cellWidth, double cellHeight) {
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col] == 1) {
                    Image image = getObjectImageForCell(row, col);
                    gc.drawImage(image, col * cellWidth, row * cellHeight, cellWidth, cellHeight);
                }
            }
        }
    }

    /**
     * Selects a wall object image for a specific cell.
     *
     * @return image for the cell
     */
    private Image getObjectImageForCell(int row, int col) {
        int index = Math.abs(row * 31 + col * 17) % wallImages.length;
        return wallImages[index];
    }

    /**
     * Draws the goal position on the maze.
     */
    private void drawGoal(GraphicsContext gc, double cellWidth, double cellHeight) {
        int goalRow = maze.getGoalPosition().getRowIndex();
        int goalCol = maze.getGoalPosition().getColumnIndex();

        gc.drawImage(goalImage, goalCol * cellWidth, goalRow * cellHeight, cellWidth, cellHeight);
    }

    /**
     * Draws the solution path over the maze.
     */
    private void drawSolution(GraphicsContext gc, double cellWidth, double cellHeight) {
        if (solution == null)
            return;

        for (AState state : solution.getSolutionPath()) {
            int[] position = parsePosition(state.toString());

            int row = position[0];
            int col = position[1];

            double x = col * cellWidth;
            double y = row * cellHeight;

            gc.fillOval(x + cellWidth * 0.25, y + cellHeight * 0.35, cellWidth * 0.14, cellHeight * 0.14);
            gc.fillOval(x + cellWidth * 0.58, y + cellHeight * 0.35, cellWidth * 0.14, cellHeight * 0.14);
            gc.fillOval(x + cellWidth * 0.25, y + cellHeight * 0.62, cellWidth * 0.14, cellHeight * 0.14);
            gc.fillOval(x + cellWidth * 0.58, y + cellHeight * 0.62, cellWidth * 0.14, cellHeight * 0.14);
        }
    }

    /**
     * Draws the character at its current position.
     */
    private void drawCharacter(GraphicsContext gc, double cellWidth, double cellHeight) {
        gc.drawImage(
                facingRight ? robotRightImage : robotLeftImage,
                characterColumn * cellWidth,
                characterRow * cellHeight,
                cellWidth,
                cellHeight
        );
    }

    /**
     * Parses a state position from text format into row and column.
     *
     * @param text state text
     * @return array containing row and column
     */
    private int[] parsePosition(String text) {
        text = text.replace("{", "").replace("}", "");
        String[] parts = text.split(",");

        int row = Integer.parseInt(parts[0].trim());
        int col = Integer.parseInt(parts[1].trim());

        return new int[]{row, col};
    }

    /**
     * Converts a mouse Y coordinate to a maze row index.
     *
     * @param y mouse Y coordinate
     * @return clicked row, or -1 if no maze exists
     */
    public int getClickedRow(double y) {
        if (maze == null)
            return -1;

        return (int) (y / (getHeight() / maze.getMaze().length));
    }

    /**
     * Converts a mouse X coordinate to a maze column index.
     *
     * @param x mouse X coordinate
     * @return clicked column, or -1 if no maze exists
     */
    public int getClickedColumn(double x) {
        if (maze == null)
            return -1;

        return (int) (x / (getWidth() / maze.getMaze()[0].length));
    }

    /**
     * Allows the canvas to resize inside its parent layout.
     *
     * @return true
     */
    @Override
    public boolean isResizable() {
        return true;
    }

    /**
     * Returns the minimum canvas width.
     */
    @Override
    public double minWidth(double height) {
        return 0;
    }

    /**
     * Returns the minimum canvas height.
     */
    @Override
    public double minHeight(double width) {
        return 0;
    }

    /**
     * Returns the maximum canvas width.
     */
    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    /**
     * Returns the maximum canvas height.
     */
    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }
}