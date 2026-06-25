package View;

import algorithms.mazeGenerators.Maze;
import algorithms.search.AState;
import algorithms.search.Solution;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class MazeDisplayer extends Canvas {
    private Maze maze;
    private Solution solution;
    private int characterRow;
    private int characterColumn;

    private final Image robotImage = loadImage("/Images/robot.png");
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

    private Image loadImage(String path) {
        return new Image(getClass().getResourceAsStream(path));
    }

    public void setMaze(Maze maze) {
        this.maze = maze;
        redraw();
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
        redraw();
    }

    public void setCharacterPosition(int row, int column) {
        this.characterRow = row;
        this.characterColumn = column;
        redraw();
    }

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

    private void drawWallsAsObjects(GraphicsContext gc, int[][] map, double cellWidth, double cellHeight) {
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col] == 1) {
                    Image image = getObjectImageForCell(row, col);

                    gc.drawImage(
                            image,
                            col * cellWidth,
                            row * cellHeight,
                            cellWidth,
                            cellHeight
                    );
                }
            }
        }
    }

    private Image getObjectImageForCell(int row, int col) {
        int index = Math.abs(row * 31 + col * 17) % wallImages.length;
        return wallImages[index];
    }

    private void drawGoal(GraphicsContext gc, double cellWidth, double cellHeight) {
        int goalRow = maze.getGoalPosition().getRowIndex();
        int goalCol = maze.getGoalPosition().getColumnIndex();

        gc.drawImage(
                goalImage,
                goalCol * cellWidth,
                goalRow * cellHeight,
                cellWidth,
                cellHeight
        );
    }

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

    private void drawCharacter(GraphicsContext gc, double cellWidth, double cellHeight) {
        gc.drawImage(
                robotImage,
                characterColumn * cellWidth,
                characterRow * cellHeight,
                cellWidth,
                cellHeight
        );
    }

    private int[] parsePosition(String text) {
        text = text.replace("{", "").replace("}", "");
        String[] parts = text.split(",");

        int row = Integer.parseInt(parts[0].trim());
        int col = Integer.parseInt(parts[1].trim());

        return new int[]{row, col};
    }

    public int getClickedRow(double y) {
        if (maze == null)
            return -1;

        return (int) (y / (getHeight() / maze.getMaze().length));
    }

    public int getClickedColumn(double x) {
        if (maze == null)
            return -1;

        return (int) (x / (getWidth() / maze.getMaze()[0].length));
    }

    @Override
    public boolean isResizable() {
        return true;
    }
}