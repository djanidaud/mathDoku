import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Random;

public class Grid {
    private int size;
    private GridPane gridPane;
    private ArrayList<GridCell> cells;
    private GridCell selectedCell;
    private ArrayList<Cage> cages;
    private MovesTracker movesTracker;
    private MistakeCatcher catcher;
    private int[][] solution;


    public Grid(int size, GridPane gridPane) {
        this.size = size;
        this.gridPane = gridPane;
        this.cells = new ArrayList<>();
        this.cages = new ArrayList<>();
        this.movesTracker = new MovesTracker();
        this.catcher = new MistakeCatcher(this);
    }

    public ArrayList<Cage> getCages() {
        return cages;
    }

    public MovesTracker getMovesTracker() {
        return movesTracker;
    }

    public int getSize() {
        return size;
    }

    public void display() {
        int id = 1;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                GridCell cell = new GridCell(id, this);
                addCell(cell, j, i);
                id++;
            }
        }
        for (Cage cage : cages) cage.display();

        tryToSolve();
    }

    public void tryToSolve() {
        Solver solver = new Solver(this);
        //if (solution == null)
        solver.solveGame();
    }

    public boolean hasSolution() {
        return solution != null;
    }

    public void addCell(GridCell cell, int col, int row) {
        gridPane.add(cell.getStackPane(), col, row);
        cells.add(cell);
    }

    public void setSelectedCell(GridCell selectedCell) {
        this.selectedCell = selectedCell;
    }

    public void addCage(int[] cagedCellsIdsArr, int result, char operation) {
        ArrayList<Integer> cagedCellIds = new ArrayList<>();

        for (int i = 0; i < cagedCellsIdsArr.length; i++) cagedCellIds.add(cagedCellsIdsArr[i]);

        Cage cage = new Cage(this, cagedCellIds, result, operation);
        cages.add(cage);
    }

    public void addCage(Cage cage) {
        cages.add(cage);
    }

    public GridCell getCellById(int id) {
        for (GridCell cell : cells) if (cell.getId() == id) return cell;
        return null;
    }

    public MistakeCatcher getCatcher() {
        return catcher;
    }

    public ArrayList<GridCell> getRowCells(int rowIndex) {
        ArrayList<GridCell> rowCells = new ArrayList<>();

        for (int i = 1; i <= size; i++) {
            int id = rowIndex * size + i;
            rowCells.add(getCellById(id));
        }
        return rowCells;
    }

    public ArrayList<GridCell> getColumnCells(int columnIndex) {
        ArrayList<GridCell> columnCells = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            int id = columnIndex + i * size + 1;
            columnCells.add(getCellById(id));
        }
        return columnCells;
    }

    public void repaintCells() {
        for (GridCell cell : cells) {
            cell.getRectangle().setFill(Color.WHITE);
            cell.getLabel().setTextFill(Color.BLACK);
        }
    }

    public void resetGrid() {
        for (GridCell cell : cells) cell.setText("");
        repaintCells();
        movesTracker.restartTracker();
    }

    public boolean checkForVictory() {
        boolean isCorrectlyFilled = true;

        for (GridCell cell : cells)
            if (!cell.isFilled()) isCorrectlyFilled = false;

        if (isCorrectlyFilled) for (int i = 0; i < size; i++)
            if (catcher.getDuplicates(getColumnCells(i)).size() != 0
                    || catcher.getDuplicates(getRowCells(i)).size() != 0) isCorrectlyFilled = false;

        if (isCorrectlyFilled) for (Cage cage : cages)
            if (catcher.hasCageMistakes(cage)) isCorrectlyFilled = false;

        return isCorrectlyFilled;
    }

    public void displayNumberButtons(HBox container) {
        container.getChildren().clear();
        for (int i = 1; i <= size; i++) {
            Button newButton = new Button(i + "");

            newButton.setOnAction(event -> {
                if (selectedCell != null)
                    selectedCell.setText(newButton.getText());
            });
            container.getChildren().add(newButton);
        }
        Button backspace = new Button("⌫");
        backspace.setOnAction(event -> {
            if (selectedCell != null)
                selectedCell.setText("");
        });

        Button hint = new Button("Hint");
        hint.setOnAction(event -> {
            if (selectedCell != null) revealCell(selectedCell);
        });
        container.getChildren().addAll(backspace, hint);

        if (!hasSolution()) hint.setDisable(true);
    }

    public void setGridFontSize(int fontSize) {
        int labelSize = fontSize - 9;
        gridPane.setStyle("-fx-font: " + fontSize + " arial;");
        for (GridCell cell : cells)
            cell.getLabel().setStyle("-fx-font:" + labelSize + " arial;");
    }

    public void playAnimation(Node rectParallel){
        Random random = new Random();
        int randomX = random.nextInt(500) + 100;
        int randomY = random.nextInt(500) + 100;

        randomX = random.nextInt() > 0.5 ? randomX : -randomX;
        randomY = random.nextInt() > 0.5 ? randomY : -randomY;

        FadeTransition fadeTransition =
                new FadeTransition(Duration.millis(3000), rectParallel);
        fadeTransition.setFromValue(1.0f);
        fadeTransition.setToValue(0.3f);

        TranslateTransition translateTransition =
                new TranslateTransition(Duration.millis(3000), rectParallel);
        translateTransition.setToX(randomX);
        translateTransition.setToY(randomY);

        ScaleTransition scaleTransition =
                new ScaleTransition(Duration.millis(3000), rectParallel);
        scaleTransition.setToX(2f);
        scaleTransition.setToY(2f);

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(
                fadeTransition,
                translateTransition,
                scaleTransition
        );
        parallelTransition.play();
    }

    public void play(){
        for (GridCell cell : cells) playAnimation(cell.getStackPane());
    }

    public void updateCellsSize(Stage stage) {
        int width = (int) (stage.getWidth() / (size * 2) + 5);
        int height = (int) stage.getHeight() / (size * 2) + 5;

        int cellSize = Math.max(width, height);

        for (GridCell cell : cells) cell.setSize(cellSize);

        stage.setMinHeight(gridPane.getHeight() + 150);
    }

    public Cage getCellCage(GridCell cell) {
        for (Cage cage : cages) if (cage.getCageCells().contains(cell)) return cage;

        return null;
    }

    public void setSolution(int[][] solution) {
        this.solution = solution;
    }

    public void displaySolution() {
        if (solution != null)
            for (GridCell cell : cells)
                revealCell(cell);
    }

    private void revealCell(GridCell cell) {
        if (solution != null) {
            int row = getCellRow(cell.getId());
            int col = getCellCol(cell.getId());
            cell.setText(solution[row][col] + "");
        }
    }

    public int getCellRow(int id) {
        return (id - 1) / size;
    }

    public int getCellCol(int id) {
        return (id - 1) % size;
    }
}
