import java.util.ArrayList;

public class Cage {
    private Grid myGrid;
    private ArrayList<Integer> cagedCellIds;
    private int result;
    private char operation;

    public Cage(Grid myGrid, ArrayList<Integer> cagedCellIds, int result, char operation) {
        this.myGrid = myGrid;
        this.cagedCellIds = cagedCellIds;
        this.result = result;
        this.operation = operation;
    }

    public int getResult() {
        return result;
    }

    public char getOperation() {
        return operation;
    }

    public ArrayList<Integer> getCagedCellIds() {
        return cagedCellIds;
    }

    public void setMyGrid(Grid myGrid) {
        this.myGrid = myGrid;
    }

    public void display() {
        int gridSize = myGrid.getSize();
        int gridArea = myGrid.getSize() * myGrid.getSize();

        GridCell firstCell = myGrid.getCellById(cagedCellIds.get(0));

        if (cagedCellIds.size()==1) firstCell.getLabel().setText(result + "");
        else firstCell.getLabel().setText(result + String.valueOf(operation));

        for (int id : cagedCellIds) {
            GridCell cell = myGrid.getCellById(id);

            GridCell cellTop = (id > gridSize) ? myGrid.getCellById(id - gridSize) : null;
            GridCell cellBottom = (id < gridArea - gridSize + 1) ? myGrid.getCellById(id + gridSize) : null;
            GridCell cellLeft = (id % gridSize != 1) ? myGrid.getCellById(id - 1) : null;
            GridCell cellRight = (id % gridSize != 0) ? myGrid.getCellById(id + 1) : null;

            int borderTop = isBordered(cellTop) ? 2 : 0;
            int borderBottom = isBordered(cellBottom) ? 2 : 0;
            int borderLeft = isBordered(cellLeft) ? 2 : 0;
            int borderRight = isBordered(cellRight) ? 2 : 0;

            cell.getTextField().setStyle("-fx-border-color:black; " +
                    "-fx-border-width: " + borderTop + " " + borderRight +
                    " " + borderBottom + " " + borderLeft + ";");
        }
    }

    public boolean isFilled() {
        int filledCells = 0;
        for (int id : cagedCellIds) {
            GridCell cell = myGrid.getCellById(id);
            if (cell.getValue() != 0) filledCells++;
        }
        return filledCells == cagedCellIds.size();
    }

    private boolean isBordered(GridCell cell) {
        return cell == null || !cagedCellIds.contains(cell.getId());
    }

    public ArrayList<GridCell> getCageCells() {
        ArrayList<GridCell> cells = new ArrayList<>();
        for (int id : cagedCellIds) cells.add(myGrid.getCellById(id));
        return cells;
    }
}
