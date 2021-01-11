import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.Collections;

public class MistakeCatcher {
    private Grid myGrid;
    private boolean doShowMistakes;

    public MistakeCatcher(Grid myGrid) {
        this.myGrid = myGrid;
    }

    public boolean doShowMistakes() {
        return doShowMistakes;
    }

    public ArrayList<GridCell> getDuplicates(ArrayList<GridCell> selectedCells) {
        ArrayList<GridCell> duplicateCells = new ArrayList<>();

        if (selectedCells != null)
            for (GridCell cell1 : selectedCells) {
                int value1 = cell1.getValue();
                if (value1 != 0)
                    for (GridCell cell2 : selectedCells) {
                        int value2 = cell2.getValue();
                        if (value1 == value2 && cell1.getId() != cell2.getId()) {
                            duplicateCells.add(cell1);
                        }
                    }
            }
        return duplicateCells;
    }

    public void displayMistakes(ArrayList<GridCell> selectedCells) {
        ArrayList<GridCell> mistakes = getDuplicates(selectedCells);

        if (selectedCells != null)
            for (GridCell cell : selectedCells) {
                Rectangle rect = cell.getRectangle();
                Cage cage = myGrid.getCellCage(cell);
                boolean hasCageMistake = hasCageMistakes(cage);

                if (!mistakes.isEmpty()) {
                    if (cell.getValue() == 0 || rect.getFill().equals(Color.WHITE))
                        rect.setFill(Color.rgb(245, 183, 177));
                    if (mistakes.contains(cell) || hasCageMistake && cell.getValue() != 0)
                        rect.setFill(Color.rgb(231, 76, 60));
                }
            }
    }

    public void checkMistakes(boolean doShowMistakes) {
        this.doShowMistakes = doShowMistakes;
        if (doShowMistakes) {
            displayAllMistakes();
            displayAllCageErrors();
        } else {
            myGrid.repaintCells();
        }
    }

    public boolean hasCageMistakes(Cage cage) {
        if (cage != null) {
            if(!cage.isFilled()) return false;
            
            int result = cage.getResult();
            char operation = cage.getOperation();

            boolean isSum = operation == '+';
            boolean isSub = operation == '-';
            boolean isMul = operation == 'x';
            boolean isDiv = operation == '÷';

            int temp = isSum || isSub ? 0 : 1;
            int count = 0;
            ArrayList<Integer> values = new ArrayList<>();
            for (GridCell cell : cage.getCageCells()) values.add(cell.getValue());
            Collections.sort(values);

            for (GridCell cell : cage.getCageCells()) {
                int cellValue = cell.getValue();

                if (isSum) temp += cellValue;

                if (isSub && count != (values.size() - 1)) {
                    if (count == 0)
                        temp += values.get(values.size() - 1);
                    temp -= values.get(count);
                }

                if (isMul) temp *= cellValue;

                if (isDiv && cellValue != 0) {
                    if (temp % cellValue == 0 || cellValue % temp == 0)
                        temp = temp % cellValue == 0 ? temp / cellValue : cellValue / temp;
                    else temp = 1;
                }
                count++;
            }
            //temp = Math.abs(temp);

            return temp != result;
        }
        return false;
    }

    public void displayCageError(Cage selectedCage) {
        if (selectedCage != null)
            for (GridCell cell : selectedCage.getCageCells())
                if (selectedCage.isFilled() && cell.getRectangle().getFill().equals(Color.WHITE)) {
                    if (hasCageMistakes(selectedCage)) {
                        cell.getRectangle().setFill(Color.rgb(231, 76, 60));
                    }
                } //else  cell.getRectangle().setFill(Color.WHITE);
    }

    public void displayAllCageErrors() {
        for (Cage cage : myGrid.getCages()) {
            displayCageError(cage);
        }
    }

    public void displayAllMistakes() {
        for (int i = 0; i < myGrid.getSize(); i++) {
            displayMistakes(myGrid.getColumnCells(i));
            displayMistakes(myGrid.getRowCells(i));
        }
    }
}
