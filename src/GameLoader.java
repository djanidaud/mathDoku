import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.*;
import java.util.*;

public class GameLoader {
    private Grid myGrid;
    private GridPane gridpane;

    public GameLoader(GridPane gridPane) {
        this.gridpane = gridPane;
        this.myGrid = new Grid(6, gridPane);
    }

    public Grid getGrid() {
        return myGrid;
    }

    public void loadDefaultGrid() {
        myGrid.addCage(new int[]{1, 7}, 11, '+');
        myGrid.addCage(new int[]{2, 3}, 2, '÷');
        myGrid.addCage(new int[]{4, 10}, 20, 'x');
        myGrid.addCage(new int[]{5, 6, 12, 18}, 6, 'x');
        myGrid.addCage(new int[]{8, 9}, 3, '-');
        myGrid.addCage(new int[]{11, 17}, 3, '÷');
        myGrid.addCage(new int[]{13, 14, 19, 20}, 240, 'x');
        myGrid.addCage(new int[]{15, 16}, 6, 'x');
        myGrid.addCage(new int[]{21, 27}, 6, 'x');
        myGrid.addCage(new int[]{22, 28, 29}, 7, '+');
        myGrid.addCage(new int[]{23, 24}, 30, 'x');
        myGrid.addCage(new int[]{25, 26}, 6, 'x');
        myGrid.addCage(new int[]{30, 36}, 9, '+');
        myGrid.addCage(new int[]{31, 32, 33}, 8, '+');
        myGrid.addCage(new int[]{34, 35}, 2, '÷');

        myGrid.display();
        myGrid.getCatcher().checkMistakes(true);
    }

    public boolean loadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        BufferedReader br = null;
        File selectedFile = fileChooser.showOpenDialog(gridpane.getScene().getWindow());

        try {
            br = new BufferedReader(new FileReader(selectedFile));
        } catch (FileNotFoundException | NullPointerException ignored) {
        }
        return readConfiguration(br);
    }

    public boolean loadTextInput() {
        Dialog dialog = new Dialog();
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toString());
        dialog.setTitle("Load a file from text input");
        dialog.setHeaderText("Enter the configuration values of the game you want to load:");

        TextArea textArea = new TextArea();
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().setStyle("-fx-padding:0 10 0 10");


        ButtonType submit = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancel, submit);

        Optional result = dialog.showAndWait();
        if (result.isPresent()) {
            if (result.get() == submit) {
                String content = textArea.getText();

                BufferedReader br = new BufferedReader(new StringReader(content));
                return readConfiguration(br);
            }
        }
        return false;
    }

    public void notifyForError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Wrong format");
        alert.setHeaderText("The file you tried to load is in wrong format.");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toString());

        alert.show();
    }

    private boolean operationExists(char operation) {
        return "+÷x-".indexOf(operation) >= 0;
    }

    private boolean isNumber(String result) {
        return result.matches("\\d*");
    }

    private boolean checkAdjacency(ArrayList<Integer> cellIds, int gridSize) {
        HashSet<Integer> adjacentCells = new HashSet<>();

        for (int i = 0; i < cellIds.size(); i++) {
            int id = cellIds.get(i);

            int top = id > gridSize ? id - gridSize : 0;
            int left = id % gridSize != 1 ? id - 1 : 0;
            int bottom = id < gridSize * gridSize - gridSize + 1 ? id + gridSize : 0;
            int right = id % gridSize != 0 ? id + 1 : 0;

            if (top != 0 && cellIds.contains(top)) adjacentCells.add(top);
            if (left != 0 && cellIds.contains(left)) adjacentCells.add(left);
            if (right != 0 && cellIds.contains(right)) adjacentCells.add(right);
            if (bottom != 0 && cellIds.contains(bottom)) adjacentCells.add(bottom);
        }
        return adjacentCells.size() == cellIds.size();
    }

    private boolean readConfiguration(BufferedReader br) {
        boolean isCorrectFormat = true;
        boolean isFileSelected = true;
        String line = "";
        ArrayList<Cage> cages = new ArrayList<>();
        HashSet<Integer> cellsSet = new HashSet<>();
        int cellCount = 0;

        while (true) {
            try {
                if ((line = br.readLine()) == null) break;
            } catch (IOException | NullPointerException e) {
                isCorrectFormat = false;
                isFileSelected = false;
                break;
            }
            String[] splitedLine = line.split(" ");

            if (splitedLine.length == 2) {
                String cageConfig = splitedLine[0];
                String cagedCellsString = splitedLine[1];
                String[] cellIdsString = cagedCellsString.split(",");

                char operation = cellIdsString.length == 1 ? '+' : cageConfig.charAt(cageConfig.length() - 1);

                if (operationExists(operation)) {
                    String resultString = cellIdsString.length == 1 ? cageConfig :
                            cageConfig.substring(0, cageConfig.length() - 1);

                    if (isNumber(resultString)) {
                        int result = Integer.parseInt(resultString);
                        ArrayList<Integer> cellIds = new ArrayList<>();

                        for (String cellId : cellIdsString)
                            if (isNumber(cellId)) {
                                cellIds.add(Integer.parseInt(cellId));
                            } else isCorrectFormat = false;

                        if (isCorrectFormat) {
                            Collections.sort(cellIds);
                            Cage cage = new Cage(null, cellIds, result, operation);
                            cages.add(cage);

                            cellCount += cellIds.size();
                            cellsSet.addAll(cellIds);
                        }
                    } else isCorrectFormat = false;
                } else isCorrectFormat = false;
            } else isCorrectFormat = false;
        }
        int gridSize = (int) Math.sqrt(cellCount);

        if (cellCount == cellsSet.size() && cellCount > 3) {
            if (Math.sqrt(cellCount) == gridSize) {

                for (int i = 1; i <= cellCount; i++)
                    if (!cellsSet.contains(i)) {
                        isCorrectFormat = false;
                        break;
                    }

                for (Cage cage : cages) {
                    ArrayList<Integer> cagedCells = cage.getCagedCellIds();
                    if (!checkAdjacency(cagedCells, gridSize) && cagedCells.size() > 1)
                        isCorrectFormat = false;
                }
            } else isCorrectFormat = false;
        } else isCorrectFormat = false;

        if (isCorrectFormat) {
            gridpane.getChildren().clear();
            Grid newGrid = new Grid(gridSize, gridpane);
            for (Cage cage : cages) {
                newGrid.addCage(cage);
                cage.setMyGrid(newGrid);
            }
            this.myGrid = newGrid;
            newGrid.display();
            return true;
        } else if (isFileSelected) notifyForError();
        return false;
    }

    public boolean createRandomGame() {
        Dialog dialog = new Dialog();
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toString());
        dialog.setTitle("Create a Random Game");
        dialog.setHeaderText("Choose the level of difficulty and size of board.");

        ChoiceBox<Integer> sizes = new ChoiceBox<>();
        sizes.setPrefWidth(100);
        sizes.getItems().addAll(2, 3, 4, 5, 6, 7, 8);
        sizes.setValue(5);

        ChoiceBox<String> difficulty = new ChoiceBox<>();
        difficulty.setPrefWidth(100);
        difficulty.getItems().addAll("Easy", "Normal", "Hard");
        difficulty.setValue("Normal");


        Label sizesLabel = new Label("Size:");
        Label difficultyLabel = new Label("Difficulty:");

        GridPane content = new GridPane();
        content.add(sizesLabel, 0, 0);
        content.add(sizes, 1, 0);
        content.add(difficultyLabel, 0, 1);
        content.add(difficulty, 1, 1);
        content.setVgap(10);
        content.setHgap(15);
        content.setAlignment(Pos.CENTER_LEFT);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-padding:0 10 0 10");


        ButtonType submit = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancel, submit);

        Optional result = dialog.showAndWait();
        if (result.isPresent())
            if (result.get() == submit) {
                int size = sizes.getValue();
                String level = difficulty.getValue();
                int maxCageLength = level.equals("Hard") ? 4 : 0;

                if (maxCageLength == 0)
                    maxCageLength = level.equals("Normal") ? 3 : 2;

                int[][] newMatrix;
                int numberOfSolutions;
                //for (int i = 0; i < 1; i++) {
                do {
                    do newMatrix = generateRandomMatrix(size);
                    while (newMatrix == null);

                    ArrayList<ArrayList<Integer>> cellClusters = generateClusters(newMatrix, maxCageLength);
                    ArrayList<Cage> cages = generateCages(newMatrix, cellClusters);

                    gridpane.getChildren().clear();
                    Grid newGrid = new Grid(newMatrix.length, gridpane);
                    for (Cage cage : cages) {
                        newGrid.addCage(cage);
                        cage.setMyGrid(newGrid);
                    }
                    this.myGrid = newGrid;
                    newGrid.setSolution(newMatrix);
                    newGrid.display();
                    if (size < 7) {
                        Solver solver = new Solver(myGrid);
                        solver.solveGame();
                        numberOfSolutions = solver.getNumberOfSolutions();
                    } else numberOfSolutions = 1;
                }
                while (numberOfSolutions != 1);
                return true;
            }
        return false;
    }

    public int[][] generateRandomMatrix(int size) {
        int[][] matrix = new int[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int value = getPossibleValue(matrix, r, c);
                if (value == 0) return null;
                matrix[r][c] = value;
            }
        }
        return matrix;
    }

    public int getPossibleValue(int[][] matrix, int rowId, int colId) {
        int maxValue = matrix.length;
        Set<Integer> range = new HashSet<>();
        for (int i = 0; i < maxValue; i++) range.add(i + 1);

        Set<Integer> occupiedValues = new HashSet<>();

        int[] row = matrix[rowId];
        int[] col = Arrays.stream(matrix).mapToInt(ints -> ints[colId]).toArray();

        for (int i : row) occupiedValues.add(i);
        for (int i : col) occupiedValues.add(i);

        Set<Integer> possibleValues = new HashSet<>();
        for (int i : range) if (!occupiedValues.contains(i)) possibleValues.add(i);

        int size = possibleValues.size();
        if (size != 0) {
            int item = new Random().nextInt(size);

            int i = 0;
            for (int value : possibleValues) {
                if (i == item)
                    return value;
                i++;
            }
        }
        return 0;
    }

    public ArrayList<ArrayList<Integer>> generateClusters(int[][] matrix, int maxCageSize) {
        ArrayList<ArrayList<Integer>> clusters = new ArrayList<>();
        int size = matrix.length;
        boolean[][] isOccupiedMatrix = new boolean[size][size];

        for (int i = 1; i <= size * size; i++) {
            ArrayList<Integer> cluster = new ArrayList<>();
            int row = (i - 1) / size;
            int col = (i - 1) % size;

            generateCluster(matrix, maxCageSize, isOccupiedMatrix, row, col, cluster, maxCageSize);
            if (cluster.size() != 0)
                clusters.add(cluster);
        }
        return clusters;
    }

    public void generateCluster(int[][] matrix, int maxCageSize,
                                boolean[][] isOccupiedMatrix, int row, int col, ArrayList<Integer> cluster, int cageSize) {
        boolean isOccupied = isOccupiedMatrix[row][col];
        int size = matrix.length;
        int randomSize = new Random().nextInt(2);
        cageSize = cageSize == maxCageSize && randomSize == 0 ? cageSize - 1 : maxCageSize;

        if (cageSize > 2) {
            randomSize = new Random().nextInt(2);
            cageSize = randomSize == 0 ? 2 : cageSize;
        }

        if (!isOccupied) {
            int id = 1 + col + row * size;
            if (cluster.size() < cageSize) {
                cluster.add(id);
                isOccupiedMatrix[row][col] = true;

                int rightCell = col == (size - 1) ? 0 : matrix[row][col + 1];
                int downCell = row == (size - 1) ? 0 : matrix[row + 1][col];

                boolean canGoRight;
                boolean canGoDown;

                if (rightCell != 0 && downCell != 0) {
                    canGoRight = !isOccupiedMatrix[row][col + 1];
                    canGoDown = !isOccupiedMatrix[row + 1][col];

                    if (canGoDown && canGoRight) {
                        //can go both direrions =>  50/50
                        int random = new Random().nextInt(2);
                        if (random == 0)//go down
                            generateCluster(matrix, maxCageSize, isOccupiedMatrix, row + 1, col, cluster, cageSize);
                        else generateCluster(matrix, maxCageSize, isOccupiedMatrix, row, col + 1, cluster, cageSize);
                        // go left
                    }
                    if (canGoDown && !canGoRight)//can go down Only =>
                        generateCluster(matrix, maxCageSize, isOccupiedMatrix, row + 1, col, cluster, cageSize);

                    if (!canGoDown && canGoRight)//can go right Only =>
                        generateCluster(matrix, maxCageSize, isOccupiedMatrix, row, col + 1, cluster, cageSize);
                }
                if (rightCell != 0 && downCell == 0) {
                    canGoRight = !isOccupiedMatrix[row][col + 1];
                    if (canGoRight) //can go right only
                        generateCluster(matrix, maxCageSize, isOccupiedMatrix, row, col + 1, cluster, cageSize);
                }
                if (rightCell == 0 && downCell != 0) {
                    canGoDown = !isOccupiedMatrix[row + 1][col];
                    if (canGoDown)//can go down only
                        generateCluster(matrix, maxCageSize, isOccupiedMatrix, row + 1, col, cluster, cageSize);

                }
            }
        }
    }

    public ArrayList<Cage> generateCages(int[][] matrix, ArrayList<ArrayList<Integer>> clusters) {
        ArrayList<Cage> cages = new ArrayList<>();
        int size = matrix.length;
        for (ArrayList<Integer> cluster : clusters) {
            Random random = new Random();
            int randomOperation = random.nextInt(4);
            char operation;
            int result;
            switch (randomOperation) {
                case 1:
                    operation = '-';
                    ArrayList<Integer> values = new ArrayList<>();
                    for (int id : cluster) {
                        int row = (id - 1) / size;
                        int col = (id - 1) % size;
                        values.add(matrix[row][col]);
                    }
                    Collections.sort(values);
                    int firstValue = values.get(values.size() - 1);
                    result = 2 * firstValue;
                    for (int i : values) result -= i;
                    //result = Math.abs(result);
                    if (result >= 0) break;
                case 2:
                    operation = 'x';
                    result = 1;
                    for (int id : cluster) {
                        int row = (id - 1) / size;
                        int col = (id - 1) % size;

                        result *= matrix[row][col];
                    }
                    break;
                case 3:
                    operation = '÷';
                    values = new ArrayList<>();
                    for (int id : cluster) {
                        int row = (id - 1) / size;
                        int col = (id - 1) % size;
                        values.add(matrix[row][col]);
                    }
                    boolean canDivide = true;
                    result = values.get(values.size() - 1) * values.get(values.size() - 1);
                    double resultDouble = result;
                    for (int i : values) {
                        resultDouble /= i;

                        if (resultDouble != Math.floor(resultDouble)) {
                            canDivide = false;
                        }
                    }
                    if (canDivide) {
                        result = (int) resultDouble;
                        break;
                    }
                default:
                    result = 0;
                    operation = '+';
                    for (int id : cluster) {
                        int row = (id - 1) / size;
                        int col = (id - 1) % size;

                        result += matrix[row][col];
                    }
                    break;
            }
            if (cluster.size() == 1) {
                int row = (cluster.get(0) - 1) / size;
                int col = (cluster.get(0) - 1) % size;
                result = matrix[row][col];
                operation = '+';
            }
            cages.add(new Cage(null, cluster, result, operation));
        }
        return cages;
    }
}
