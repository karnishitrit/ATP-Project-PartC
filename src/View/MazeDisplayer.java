package View;

import algorithms.mazeGenerators.Maze;
import algorithms.search.AState;
import algorithms.search.Solution;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Custom maze board control.
 * The control knows when and where to draw the maze parts,
 * but the actual images are provided from outside so the
 * control can be reused with different themes.
 */
public class MazeDisplayer extends Canvas {
    private Maze maze;
    private Solution solution;

    private int characterRow;
    private int characterColumn;
    private boolean facingRight = true;

    private Image characterRightImage;
    private Image characterLeftImage;
    private Image floorImage;
    private Image goalImage;
    private Image[] wallImages;

    /**
     * Sets the images used by the maze display.
     * This keeps the control independent from a specific game theme.
     *
     * @param characterRightImage character image facing right
     * @param characterLeftImage character image facing left
     * @param floorImage floor cell image
     * @param goalImage goal cell image
     * @param wallImages images used for wall cells
     */
    public void setImages(Image characterRightImage, Image characterLeftImage, Image floorImage, Image goalImage, Image[] wallImages) {
        this.characterRightImage = characterRightImage;
        this.characterLeftImage = characterLeftImage;
        this.floorImage = floorImage;
        this.goalImage = goalImage;
        this.wallImages = wallImages;
        redraw();
    }

    /**
     * Sets the maze to display and redraws the board.
     *
     * @param maze maze to display
     */
    public void setMaze(Maze maze) {
        this.maze = maze;
        redraw();
    }

    /**
     * Sets the solution path and redraws the board.
     *
     * @param solution solution to display
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
     * Updates the direction of the character.
     *
     * @param facingRight true if the character faces right
     */
    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
        redraw();
    }

    /**
     * Redraws the full maze board.
     */
    public void redraw() {
        if (maze == null || !imagesAreReady()) {
            return;
        }

        double canvasWidth = getWidth();
        double canvasHeight = getHeight();

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, canvasWidth, canvasHeight);

        int[][] map = maze.getMaze();

        double cellWidth = canvasWidth / map[0].length;
        double cellHeight = canvasHeight / map.length;

        drawFloor(gc, map, cellWidth, cellHeight);
        drawWalls(gc, map, cellWidth, cellHeight);
        drawSolution(gc, cellWidth, cellHeight);
        drawGoal(gc, cellWidth, cellHeight);
        drawCharacter(gc, cellWidth, cellHeight);
    }

    /**
     * Checks that all required images were provided.
     *
     * @return true if the board can be drawn
     */
    private boolean imagesAreReady() {
        return characterRightImage != null
                && characterLeftImage != null
                && floorImage != null
                && goalImage != null
                && wallImages != null
                && wallImages.length > 0;
    }

    /**
     * Draws the floor image in every maze cell.
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
     * Draws wall cells using the wall images supplied from outside.
     */
    private void drawWalls(GraphicsContext gc, int[][] map, double cellWidth, double cellHeight) {
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col] == 1) {
                    Image image = getWallImageForCell(row, col);
                    gc.drawImage(image, col * cellWidth,row * cellHeight, cellWidth, cellHeight);
                }
            }
        }
    }

    /**
     * Selects a wall image according to the cell position.
     * The same cell will always receive the same image.
     *
     * @param row cell row
     * @param col cell column
     * @return wall image for the cell
     */
    private Image getWallImageForCell(int row, int col) {
        int index = Math.abs(row * 31 + col * 17) % wallImages.length;
        return wallImages[index];
    }

    /**
     * Draws the goal position.
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
        if (solution == null) {
            return;
        }

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
     * Draws the character according to its current direction.
     */
    private void drawCharacter(GraphicsContext gc, double cellWidth, double cellHeight) {
        Image characterImage = facingRight ? characterRightImage : characterLeftImage;

        gc.drawImage(
                characterImage,
                characterColumn * cellWidth,
                characterRow * cellHeight,
                cellWidth,
                cellHeight
        );
    }

    /**
     * Converts a state text representation into row and column indexes.
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
     * Converts a mouse Y coordinate into a maze row.
     *
     * @param y mouse Y coordinate
     * @return clicked row, or -1 if no maze exists
     */
    public int getClickedRow(double y) {
        if (maze == null) {
            return -1;
        }

        return (int) (y / (getHeight() / maze.getMaze().length));
    }

    /**
     * Converts a mouse X coordinate into a maze column.
     *
     * @param x mouse X coordinate
     * @return clicked column, or -1 if no maze exists
     */
    public int getClickedColumn(double x) {
        if (maze == null) {
            return -1;
        }

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
     *
     * @param height available height
     * @return minimum width
     */
    @Override
    public double minWidth(double height) {
        return 0;
    }

    /**
     * Returns the minimum canvas height.
     *
     * @param width available width
     * @return minimum height
     */
    @Override
    public double minHeight(double width) {
        return 0;
    }

    /**
     * Returns the maximum canvas width.
     *
     * @param height available height
     * @return maximum width
     */
    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    /**
     * Returns the maximum canvas height.
     *
     * @param width available width
     * @return maximum height
     */
    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }
}