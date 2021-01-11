import java.util.*;

public class Solver {
    private Grid myGrid;
    private ArrayList<ArrayList<ArrayList<Integer>>> possibleVariations;
    private ArrayList<ArrayList<ArrayList<Integer>>> possibleArrangements;
    private HashMap<Integer, Set<Integer>> onlyPossibleValuesInLinedCage;
    private ArrayList<int[][]> possibleMatrices;

    public Solver(Grid myGrid) {
        this.myGrid = myGrid;
        this.possibleVariations = new ArrayList<>();
        this.possibleArrangements = new ArrayList<>();
        this.onlyPossibleValuesInLinedCage = new HashMap<>();
        this.possibleMatrices = new ArrayList<>();
    }

    public int getNumberOfSolutions(){
        return possibleMatrices.size();
    }

    public void solveGame() {
        generateCageVariations();
        removeDups();
        updateVariations();

        if (checkPermuts() > 10_000 || checkPermuts() < 0) if (tryToGuessCell()) {
            System.out.println("GUESSED CELLS");
        }
        if (myGrid.getSize() < 8)
            for (int i = 2; i <= 7 * 7 * 7 * 7; i++)
                if (checkPermuts() > 10_000 || checkPermuts() < 0) if (tryToGuess(i)) {
                    System.out.println("GUESSED A SOLUTION");
                    break;
                }
        if (checkPermuts() > 10_000 || checkPermuts() < 0) if(tryToGuessRowOrCol()){
            System.out.println("GUESSED A ROWCOL");
        };


        System.out.println(possibleVariations);
        System.out.println(checkPermuts());

        long permuts = checkPermuts();
        if (permuts < 3_000_000L && permuts > 0) {
            generateMatrixArrangements();

            ArrayList<int[][]> solutions = scanMatrices();
            int[][] solution = solutions.size()==0 ? null : solutions.get(0);
            myGrid.setSolution(solution);

            if (solution != null) {
                System.out.println("Solution:");
                printMatrix(solution);
                possibleMatrices = solutions;
            }//else -> no solution
        }//else -> cant compute solution
    }

    private void generateCageVariations() {
        ArrayList<ArrayList<ArrayList<Integer>>> possibleCombinations = new ArrayList<>();
        ArrayList<Cage> cages = myGrid.getCages();

        for (Cage cage : cages) {
            int cellsCount = cage.getCageCells().size();
            int result = cage.getResult();
            char operation = cage.getOperation();

            ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();
            findCombinations(result, cellsCount, combinations, operation);

            possibleCombinations.add(combinations);
        }
        generateVariations(possibleCombinations);
    }

    private void findCombinations(int result, int cellsCount, ArrayList<ArrayList<Integer>> combinations, char operation) {
        int max = getSize();
        int[] arr = new int[max];
        for (int i = 0; i < max; i++) arr[i] = i + 1;

        CombinationRepetition(arr, max, cellsCount, combinations, result, operation);
    }

    private int getSize(){
        return myGrid.getSize();
    }

    private boolean isCorrectResult(List<Integer> arr, int result, char operation) {
        boolean isSum = operation == '+';
        boolean isSub = operation == '-';
        boolean isDiv = operation == '÷';
        boolean isMul = operation == 'x';
        Collections.sort(arr);

        int temp = isSum || isSub ? 0 : 1;
        if (isSum) for (int i : arr) temp += i;
        if (isMul) for (int i : arr) temp *= i;
        if (isSub) {
            temp = 2 * arr.get(arr.size() - 1);
            for (int i : arr)
                temp -= i;

            //temp = Math.abs(temp);
        }
        if (isDiv) {
            double tempDouble;
            tempDouble = arr.get(arr.size() - 1) * arr.get(arr.size() - 1);

            for (int i : arr) {
                tempDouble /= i;
            }
            return tempDouble == result;
        }
        return temp == result;
    }

    private void CombinationRepetitionUtil(int[] chosen, int[] arr,
                                           int index, int r, int start, int end,
                                           ArrayList<ArrayList<Integer>> combinations, int result, char operation) {
        if (index == r) {
            ArrayList<Integer> combos = new ArrayList<>();
            for (int i = 0; i < r; i++) {
                combos.add(arr[chosen[i]]);
            }
            if (isCorrectResult(combos, result, operation))
                combinations.add(combos);
            return;
        }
        for (int i = start; i <= end; i++) {
            chosen[index] = i;
            CombinationRepetitionUtil(chosen, arr, index + 1,
                    r, i, end, combinations, result, operation);
        }
    }

    private void CombinationRepetition(int arr[], int n, int r,
                                       ArrayList<ArrayList<Integer>> combinations, int result, char operation) {
        int[] chosen = new int[r + 1];
        CombinationRepetitionUtil(chosen, arr, 0, r, 0, n - 1, combinations, result, operation);
    }

    private void updateVariations() {
        long prevPermuts;
        do {
            prevPermuts = checkPermuts();
            if (checkPermuts() > 5_000L || 0 > checkPermuts()) {
                findOnlyPossibleValuesInLinedCage();
                eliminateInconsidtencies();
                lookForAbsoluteCells();
                scanMatchingValues();
                scanLineGroups();

                findOnlyPossibleValuesInLinedCage();
                eliminateInconsidtencies();
                lookForAbsoluteCells();

                for (int pairSize = 2; pairSize <= 4; pairSize++) lookForMatchingPairs(pairSize);

                scanLineMatchGroups();
            }
        } while (prevPermuts != checkPermuts());
    }

    private boolean isCageConfigurationAcceptable(Cage cage, ArrayList<Integer> list) {
        HashMap<Integer, Integer> cellValueMap = new HashMap<>();
        ArrayList<Integer> cellIds = cage.getCagedCellIds();
        for (int i = 0; i < cellIds.size(); i++) {
            cellValueMap.put(cellIds.get(i), list.get(i));
        }

        boolean isCorrect = true;
        for (int id : cellIds) {
            int row = myGrid.getCellRow(id);
            int col = myGrid.getCellCol(id);

            ArrayList<GridCell> rowCells = myGrid.getRowCells(row);
            ArrayList<GridCell> colCells = myGrid.getColumnCells(col);
            ArrayList<GridCell> cageLineRowMates = new ArrayList<>();
            ArrayList<GridCell> cageLineColMates = new ArrayList<>();

            for (GridCell cell : rowCells)
                if (cage.getCageCells().contains(cell) && !cageLineRowMates.contains(cell))
                    cageLineRowMates.add(cell);
            for (GridCell cell : colCells)
                if (cage.getCageCells().contains(cell) && !cageLineColMates.contains(cell))
                    cageLineColMates.add(cell);

            ArrayList<Integer> inlineRowValues = new ArrayList<>();
            for (GridCell cell : cageLineRowMates)
                inlineRowValues.add(cellValueMap.get(cell.getId()));

            Set<Integer> set = new HashSet<>(inlineRowValues);
            if (set.size() != inlineRowValues.size())
                isCorrect = false;


            ArrayList<Integer> inlineColCagedMatesValues = new ArrayList<>();
            for (GridCell cell : cageLineColMates)
                inlineColCagedMatesValues.add(cellValueMap.get(cell.getId()));

            Set<Integer> set1 = new HashSet<>(inlineColCagedMatesValues);
            if (set1.size() != inlineColCagedMatesValues.size())
                isCorrect = false;
        }
        return isCorrect;
    }

    //generates all possible ways to fill each cage of the puzzle
    private void generateVariations(ArrayList<ArrayList<ArrayList<Integer>>> possibleCombinations) {
        ArrayList<Cage> cages = myGrid.getCages();
        for (int cageId = 0; cageId < cages.size(); cageId++) {
            ArrayList<ArrayList<Integer>> combinations = possibleCombinations.get(cageId);
            ArrayList<ArrayList<Integer>> variations = new ArrayList<>();

            for (ArrayList<Integer> list : combinations)
                permutations(list, new Stack<>(), list.size(), variations);
            possibleVariations.add(variations);
        }
    }

    private void removeDups() {
        ArrayList<ArrayList<ArrayList<Integer>>> temp = new ArrayList<>();

        int cageId = 0;
        for (ArrayList<ArrayList<Integer>> lists : possibleVariations) {
            ArrayList<ArrayList<Integer>> noDupsList = new ArrayList<>();
            Set<ArrayList<Integer>> set = new HashSet<>(lists);
            Cage cage = myGrid.getCages().get(cageId);
            boolean isInLine = isInLine(cage);

            for (ArrayList<Integer> list : set) {
                boolean hasDups = hasDups(list);
                if (hasDups && isInLine) continue;
                if (isCageConfigurationAcceptable(cage, list))
                    noDupsList.add(list);
            }
            temp.add(noDupsList);
            cageId++;
        }
        for (ArrayList<ArrayList<Integer>> list : temp) if (list.size() == 0) System.out.println("EMPTY");

        possibleVariations = temp;
    }

    private boolean hasDups(ArrayList<Integer> list) {
        Set<Integer> set = new HashSet<>(list);
        return set.size() != list.size();
    }

    private boolean isInLine(Cage cage) {
        return isColLined(cage) || isRowLined(cage);
    }

    private long checkPermuts() {
        long permuts = 1;
        for (ArrayList<ArrayList<Integer>> lists : possibleVariations)
            permuts *= lists.size();
        return permuts;
    }

    private void permutations(List<Integer> items, Stack<Integer> permutation, int size,
                              ArrayList<ArrayList<Integer>> variations) {
        if (permutation.size() == size) {
            ArrayList<Integer> list = new ArrayList<>(permutation);
            variations.add(list);
        }
        Integer[] availableItems = items.toArray(new Integer[0]);
        for (Integer i : availableItems) {
            permutation.push(i);
            items.remove(i);
            permutations(items, permutation, size, variations);
            items.add(permutation.pop());
        }
    }

    private ArrayList<ArrayList<ArrayList<Integer>>> getCombinations(ArrayList<ArrayList<ArrayList<Integer>>> lists) {
        ArrayList<ArrayList<ArrayList<Integer>>> combinations = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> newCombinations;
        int index = 0;

        for (ArrayList<Integer> i : lists.get(0)) {
            ArrayList<ArrayList<Integer>> newList = new ArrayList<>();
            newList.add(i);
            combinations.add(newList);
        }
        index++;
        while (index < lists.size()) {
            List<ArrayList<Integer>> nextList = lists.get(index);
            newCombinations = new ArrayList<>();
            for (List<ArrayList<Integer>> first : combinations) {
                for (ArrayList<Integer> second : nextList) {
                    ArrayList<ArrayList<Integer>> newList = new ArrayList<>();
                    newList.addAll(first);
                    newList.add(second);
                    newCombinations.add(newList);
                }
            }
            combinations = newCombinations;
            index++;
        }
        return combinations;
    }

    private void generateMatrixArrangements() {
        ArrayList<ArrayList<ArrayList<Integer>>> combs = getCombinations(possibleVariations);
        possibleArrangements.clear();
        possibleMatrices.clear();

        possibleArrangements.addAll(combs);
        buildMatrixes();
    }

    private void findOnlyPossibleValuesInLinedCage() {
        int cageId = 0;
        for (ArrayList<ArrayList<Integer>> lists : possibleVariations) {
            ArrayList<Set<Integer>> sets = new ArrayList<>();
            for (ArrayList<Integer> list : lists) {
                Set<Integer> set = new HashSet<>(list);
                if (!sets.contains(set) && isInLine(myGrid.getCages().get(cageId)))
                    sets.add(set);
            }
            if (sets.size() == 1) onlyPossibleValuesInLinedCage.put(cageId, sets.get(0));
            cageId++;
        }
    }

    private void eliminateInconsidtencies() {
        for (Map.Entry<Integer, Set<Integer>> entry : onlyPossibleValuesInLinedCage.entrySet()) {
            int cageId = entry.getKey();
            Set<Integer> set = entry.getValue();

            Cage cage = myGrid.getCages().get(cageId);
            boolean isRowLined = isRowLined(cage);

            if (isRowLined) {
                int cellId = cage.getCagedCellIds().get(0);
                int row = myGrid.getCellRow(cellId);

                ArrayList<GridCell> rowCells = myGrid.getRowCells(row);

                for (GridCell cell : rowCells) {
                    if (!cage.getCageCells().contains(cell)) {
                        Cage neighbourCage = myGrid.getCellCage(cell);
                        int neighbourCageId = myGrid.getCages().indexOf(neighbourCage);
                        int neighbourCellPosition = neighbourCage.getCageCells().indexOf(cell);

                        ArrayList<ArrayList<Integer>> newVariations = new ArrayList<>();
                        ArrayList<ArrayList<Integer>> variations = possibleVariations.get(neighbourCageId);
                        for (ArrayList<Integer> variation : variations) {
                            int questionableValue = variation.get(neighbourCellPosition);

                            if (!set.contains(questionableValue)) newVariations.add(variation);
                        }
                        if (newVariations.size() != 0) possibleVariations.set(neighbourCageId, newVariations);
                    }
                }
            } else {
                int cellId = cage.getCagedCellIds().get(0);
                int col = myGrid.getCellCol(cellId);
                ArrayList<GridCell> colCells = myGrid.getColumnCells(col);

                for (GridCell cell : colCells) {
                    if (!cage.getCageCells().contains(cell)) {
                        Cage neighbourCage = myGrid.getCellCage(cell);
                        int neighbourCageId = myGrid.getCages().indexOf(neighbourCage);
                        int neighbourCellPosition = neighbourCage.getCageCells().indexOf(cell);

                        ArrayList<ArrayList<Integer>> newVariations = new ArrayList<>();
                        ArrayList<ArrayList<Integer>> variations = possibleVariations.get(neighbourCageId);
                        for (ArrayList<Integer> variation : variations) {
                            int questionableValue = variation.get(neighbourCellPosition);

                            if (!set.contains(questionableValue)) newVariations.add(variation);
                        }
                        if (newVariations.size() != 0) possibleVariations.set(neighbourCageId, newVariations);
                    }
                }
            }
        }
    }

    private boolean isRowLined(Cage cage) {
        ArrayList<GridCell> cells = cage.getCageCells();

        int firstCellId = cells.get(0).getId();
        int row = myGrid.getCellRow(firstCellId);
        ArrayList<GridCell> rowCells = myGrid.getRowCells(row);
        boolean inLineRow = true;
        for (GridCell cell : cells) {
            int rowId = myGrid.getCellRow(cell.getId());
            ArrayList<GridCell> rowTemp = myGrid.getRowCells(rowId);
            if (!rowTemp.equals(rowCells)) inLineRow = false;
        }
        return inLineRow;
    }

    private boolean isColLined(Cage cage) {
        ArrayList<GridCell> cells = cage.getCageCells();

        int firstCellId = cells.get(0).getId();
        int col = myGrid.getCellCol(firstCellId);

        ArrayList<GridCell> colCells = myGrid.getColumnCells(col);
        boolean inLineCol = true;
        for (GridCell cell : cells) {
            int colId = myGrid.getCellCol(cell.getId());
            ArrayList<GridCell> colTemp = myGrid.getColumnCells(colId);
            if (!colTemp.equals(colCells)) inLineCol = false;
        }
        return inLineCol;
    }

    private void lookForAbsoluteCells() {
        int size = getSize();

        for (int i = 1; i <= size * size; i++) {
            Set<Integer> possibleValues = findPossibleValues(i);

            if (possibleValues.size() == 1) {
                GridCell cell = myGrid.getCellById(i);
                Cage homeCage = myGrid.getCellCage(cell);

                int row = myGrid.getCellRow(i);
                int col = myGrid.getCellCol(i);

                ArrayList<GridCell> rowCells = myGrid.getRowCells(row);
                ArrayList<GridCell> colCells = myGrid.getColumnCells(col);

                int absoluteValue = (int) possibleValues.toArray()[0];

                removeDuplicatePossibilities(rowCells, absoluteValue, homeCage);
                removeDuplicatePossibilities(colCells, absoluteValue, homeCage);
            }
        }
    }

    private void removeDuplicatePossibilities(ArrayList<GridCell> cells, int value, Cage homeCage) {

        for (GridCell cell : cells) {
            if (!homeCage.getCageCells().contains(cell)) {
                Cage cage = myGrid.getCellCage(cell);
                int cageIndex = myGrid.getCages().indexOf(cage);
                int cellPosition = cage.getCageCells().indexOf(cell);

                ArrayList<ArrayList<Integer>> variations = possibleVariations.get(cageIndex);
                ArrayList<ArrayList<Integer>> newVariations = new ArrayList<>();
                if (variations.size() > 1) {
                    for (ArrayList<Integer> variation : variations) {
                        int questionableValue = variation.get(cellPosition);
                        if (questionableValue != value) newVariations.add(variation);
                    }
                    if (newVariations.size() != 0) possibleVariations.set(cageIndex, newVariations);
                }
            }
        }
    }

    private void scanMatchingValues() {
        int cageId = 0;
        for (ArrayList<ArrayList<Integer>> variations : possibleVariations) {
            if (variations.size() > 1) {
                ArrayList<Set<Integer>> matchingValues = new ArrayList<>();

                for (int i = 0; i < 10; i++) matchingValues.add(new HashSet<>());

                for (ArrayList<Integer> variation : variations) {
                    for (int i = 0; i < variation.size(); i++) matchingValues.get(i).add(variation.get(i));
                }

                for (Set<Integer> set : matchingValues) {
                    if (set.size() == 1) {
                        int valueIndex = matchingValues.indexOf(set);
                        int value = (int) set.toArray()[0];

                        Cage cage = myGrid.getCages().get(cageId);
                        GridCell cell = cage.getCageCells().get(valueIndex);

                        int row = myGrid.getCellRow(cell.getId());
                        int col = myGrid.getCellCol(cell.getId());

                        ArrayList<GridCell> rowCells = myGrid.getRowCells(row);
                        ArrayList<GridCell> colCells = myGrid.getColumnCells(col);

                        removeDuplicatePossibilities(rowCells, value, cage);
                        removeDuplicatePossibilities(colCells, value, cage);
                    }
                }
            }
            cageId++;
        }
    }

    public void buildMatrixes() {
        for (ArrayList<ArrayList<Integer>> cagesConfig : possibleArrangements)
            buildMatrix(cagesConfig);
    }

    public void buildMatrix(ArrayList<ArrayList<Integer>> cagesConfig) {
        int size = getSize();
        int[][] matrix = new int[size][size];
        ArrayList<Cage> cages = myGrid.getCages();
        int cageId = 0;
        for (Cage cage : cages) {
            ArrayList<Integer> cellIds = cage.getCagedCellIds();
            ArrayList<Integer> config = cagesConfig.get(cageId);

            for (int id : cellIds) {
                int row = myGrid.getCellRow(id);
                int col = myGrid.getCellCol(id);

                matrix[row][col] = config.get(cellIds.indexOf(id));
            }
            cageId++;
        }
        possibleMatrices.add(matrix);
    }

    private int[] getColumn(int[][] matrix, int column) {
        return Arrays.stream(matrix).mapToInt(ints -> ints[column]).toArray();
    }

    public ArrayList<int[][]> scanMatrices() {
        ArrayList<int[][]> solutions = new ArrayList<>();
        int size = getSize();
        for (int[][] matrix : possibleMatrices) {
            boolean isCorrect = true;
            for (int r = 0; r < size; r++) {
                int[] row = matrix[r];
                Set<Integer> set = new HashSet<>();
                for (int i : row) set.add(i);
                if (set.size() != row.length) isCorrect = false;
            }
            if (isCorrect) {
                for (int c = 0; c < size; c++) {
                    int[] col = getColumn(matrix, c);
                    Set<Integer> set = new HashSet<>();
                    for (int i : col) set.add(i);
                    if (set.size() != col.length) isCorrect = false;
                }
            }
            if (isCorrect) solutions.add(matrix);
        }
        return solutions;
    }

    public void printMatrix(int[][] matrix) {
        if (matrix == null) return;
        int size = getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(matrix[i][j] + ",");
            }
            System.out.println();
        }
        System.out.println();
    }

    private Set<Integer> findPossibleValues(int id) {
        GridCell cell = myGrid.getCellById(id);
        Cage cage = myGrid.getCellCage(cell);
        int cageIndex = myGrid.getCages().indexOf(cage);
        int cellIndex = cage.getCageCells().indexOf(cell);

        Set<Integer> set = new HashSet<>();
        ArrayList<ArrayList<Integer>> variations = possibleVariations.get(cageIndex);
        for (ArrayList<Integer> variation : variations) {
            int value = variation.get(cellIndex);
            set.add(value);
        }
        return set;
    }

    private void lookForMatchingPairs(int length) {
        int size = getSize();
        if (size > 4) {
            for (int r = 0; r < size; r++) {
                ArrayList<GridCell> rowCells = myGrid.getRowCells(r);
                ArrayList<GridCell> colCells = myGrid.getColumnCells(r);
                ArrayList<Set<Integer>> rowPossibleValues = new ArrayList<>();
                ArrayList<Set<Integer>> colPossibleValues = new ArrayList<>();

                for (GridCell cell : rowCells) rowPossibleValues.add(findPossibleValues(cell.getId()));
                for (GridCell cell : colCells) colPossibleValues.add(findPossibleValues(cell.getId()));

                eliminatePairs(rowPossibleValues, rowCells, length);

                eliminatePairs(colPossibleValues, colCells, length);
            }
        }
    }

    private void eliminatePairs(ArrayList<Set<Integer>> combos, ArrayList<GridCell> cellGroup, int length) {
        ArrayList<Set<Integer>> twins = getPairs(combos, length);
        for (Set<Integer> twin : twins)
            for (GridCell cell : cellGroup) {
                int id = cell.getId();
                Set<Integer> possibleValues = findPossibleValues(id);

                if (containsTwinValue(possibleValues, twin)
                        && !possibleValues.equals(twin)
                        && possibleValues.size() != 1) {
                    Cage cage = myGrid.getCellCage(cell);
                    int cageIndex = myGrid.getCages().indexOf(cage);
                    int cellIndex = cage.getCageCells().indexOf(cell);
                    ArrayList<ArrayList<Integer>> variations = possibleVariations.get(cageIndex);
                    ArrayList<ArrayList<Integer>> newVariations = new ArrayList<>();
                    for (ArrayList<Integer> variation : variations) {
                        int value = variation.get(cellIndex);

                        if (!twin.contains(value))
                            newVariations.add(variation);
                    }
                    if (newVariations.size() != 0) possibleVariations.set(cageIndex, newVariations);
                }
            }
    }

    public boolean containsTwinValue(Set<Integer> possibleValues, Set<Integer> twin) {
       Object[] arr = possibleValues.toArray();
        for (Object obj : arr)
            if (twin.contains(obj))
                return true;
        return false;
    }

    public ArrayList<Set<Integer>> getPairs(ArrayList<Set<Integer>> possibleValues, int length) {
        ArrayList<Set<Integer>> matchingPairs = new ArrayList<>();
        List<List<Set<Integer>>> combinations = combination(possibleValues, length);
        for (List<Set<Integer>> combination : combinations) {
            Set<Integer> possiblePairs = new HashSet<>();
            for (Set<Integer> set : combination) {
                possiblePairs.addAll(set);
                if (set.size() == 1) {
                    possiblePairs.clear();
                    break;
                }
            }
            if (possiblePairs.size() == length) matchingPairs.add(possiblePairs);
        }
        return matchingPairs;
    }

    private List<List<Set<Integer>>> combination(List<Set<Integer>> values, int size) {
        if (0 == size) return Collections.singletonList(Collections.emptyList());
        if (values.isEmpty()) return Collections.emptyList();

        List<List<Set<Integer>>> combination = new LinkedList<>();
        Set<Integer> actual = values.iterator().next();

        List<Set<Integer>> subSet = new LinkedList<>(values);
        subSet.remove(actual);
        List<List<Set<Integer>>> subSetCombination = combination(subSet, size - 1);
        for (List<Set<Integer>> set : subSetCombination) {
            List<Set<Integer>> newSet = new LinkedList<>(set);
            newSet.add(0, actual);
            combination.add(newSet);
        }
        combination.addAll(combination(subSet, size));

        return combination;
    }

    private void scanLineGroups() {
        int size = getSize();
        for (int r = 0; r < size; r++) {
            ArrayList<GridCell> rowCells = myGrid.getRowCells(r);
            ArrayList<GridCell> colCells = myGrid.getColumnCells(r);

            Set<Cage> inlineSumRowCages = new HashSet<>();
            for (GridCell cell : rowCells) {
                Cage cage = myGrid.getCellCage(cell);
                if (isRowLined(cage) && cage.getOperation() == '+') inlineSumRowCages.add(cage);
            }

            Set<Cage> inlineSumColCages = new HashSet<>();
            for (GridCell cell : colCells) {
                Cage cage = myGrid.getCellCage(cell);
                if (isColLined(cage) && cage.getOperation() == '+') inlineSumColCages.add(cage);
            }

            scanLineSums(inlineSumRowCages, rowCells);
            scanLineSums(inlineSumColCages, colCells);
        }
    }

    private void scanLineMatchGroups() {
        int size = getSize();
        for (int r = 0; r < size; r++) {
            ArrayList<GridCell> rowCells = myGrid.getRowCells(r);
            ArrayList<GridCell> colCells = myGrid.getColumnCells(r);

            for (int length = 2; length < 8; length++) {
                ArrayList<Set<Integer>> rowPossibleValues = new ArrayList<>();
                ArrayList<Set<Integer>> colPossibleValues = new ArrayList<>();

                for (GridCell cell : rowCells) rowPossibleValues.add(findPossibleValues(cell.getId()));
                for (GridCell cell : colCells) colPossibleValues.add(findPossibleValues(cell.getId()));

                ArrayList<Set<Integer>> rowMatchingCells = getPairs(rowPossibleValues, length);
                ArrayList<Set<Integer>> colMatchingCells = getPairs(colPossibleValues, length);

                scanLineSums(rowMatchingCells, rowCells);
                scanLineSums(colMatchingCells, colCells);
            }
        }
    }

    private void scanLineSums(Set<Cage> inlineSumRowCages, ArrayList<GridCell> rowCells) {
        ArrayList<GridCell> insideCells = new ArrayList<>();
        int sum = 0;
        for (Cage cage : inlineSumRowCages) {
            insideCells.addAll(cage.getCageCells());
            sum += cage.getResult();
        }
        int expectedSum = getExpectedLineSum();
        int otherCellsSum = expectedSum - sum;

        if (insideCells.size() > 0) {
            ArrayList<GridCell> outsideCells = new ArrayList<>();
            for (GridCell rowCell : rowCells) {
                if (!insideCells.contains(rowCell)) outsideCells.add(rowCell);
            }
            int cellCount = outsideCells.size();
            ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();
            findCombinations(otherCellsSum, cellCount, combinations, '+');

            ArrayList<ArrayList<Integer>> noDupsCombinations = new ArrayList<>();
            for (ArrayList<Integer> combination : combinations) {
                if (!hasDups(combination)) noDupsCombinations.add(combination);
            }
            combinations = noDupsCombinations;

            ArrayList<ArrayList<Integer>> permuts = new ArrayList<>();
            for (ArrayList<Integer> combos : combinations) {
                permutations(combos, new Stack<>(), combos.size(), permuts);
            }
            combinations = permuts;

            ArrayList<ArrayList<Integer>> correctCombinations = new ArrayList<>();
            for (ArrayList<Integer> combination : combinations) {
                boolean isCombinationCorrect = true;
                for (GridCell outsideCell : outsideCells) {
                    int cellIndex = outsideCells.indexOf(outsideCell);
                    int value = combination.get(cellIndex);
                    int cellId = outsideCell.getId();

                    Set<Integer> possibleValues = findPossibleValues(cellId);

                    if (!possibleValues.contains(value)) isCombinationCorrect = false;
                }
                if (isCombinationCorrect) {
                    correctCombinations.add(combination);
                }
            }
            Set<ArrayList<Object>> newVariations = new HashSet<>();
            for (ArrayList<Integer> combination : correctCombinations) {
                for (GridCell outsideCell : outsideCells) {
                    int cellIndex = outsideCells.indexOf(outsideCell);
                    int value = combination.get(cellIndex);
                    Cage cage = myGrid.getCellCage(outsideCell);
                    int cageIndex = myGrid.getCages().indexOf(cage);
                    int cellCageIndex = cage.getCageCells().indexOf(outsideCell);

                    ArrayList<ArrayList<Integer>> variations = possibleVariations.get(cageIndex);
                    for (ArrayList<Integer> variation : variations) {
                        int varValue = variation.get(cellCageIndex);

                        if (varValue == value) {
                            ArrayList<Object> objects = new ArrayList<>();
                            objects.add(cageIndex);
                            objects.add(variation);
                            newVariations.add(objects);
                        }
                    }
                }
            }
            for (ArrayList<Object> objects : newVariations) {
                int cageIndex = (int) objects.get(0);
                possibleVariations.get(cageIndex).clear();
            }
            for (ArrayList<Object> objects : newVariations) {
                int cageIndex = (int) objects.get(0);
                possibleVariations.get(cageIndex).add((ArrayList<Integer>) objects.get(1));
            }
        }
    }

    private int getExpectedLineSum() {
        int size = getSize();
        int sum = 0;
        for (int i = 1; i <= size; i++) sum += i;
        return sum;
    }

    private boolean tryToGuess(int cellCount) {
        ArrayList<ArrayList<ArrayList<Integer>>> backup = getDeepCopy(possibleVariations);
        for (int i = 0; i < cellCount; i++) {
            int cageId = 0;
            for (ArrayList<ArrayList<Integer>> variations : possibleVariations) {
                if (variations.size() == cellCount) {
                    ArrayList<Integer> guess = variations.get(i);
                    possibleVariations.get(cageId).clear();
                    possibleVariations.get(cageId).add(guess);

                    updateVariations();

                    int[][] solution;
                    if (checkPermuts() < 1_000_000L && 0 < checkPermuts()) {
                        generateMatrixArrangements();

                        ArrayList<int[][]> solutions = scanMatrices();
                        solution = solutions.size()==0 ? null : solutions.get(0);

                        if (solution != null) return true;
                        else {
                            possibleVariations = getDeepCopy(backup);
                        }
                    }
                }
                cageId++;
            }
        }
        possibleVariations = getDeepCopy(backup);
        return false;
    }

    private void scanLineSums(ArrayList<Set<Integer>> matchingCells, ArrayList<GridCell> lineCells) {
        for (Set<Integer> set : matchingCells) {

            ArrayList<GridCell> insideCells = new ArrayList<>();
            int sum = 0;
            for (int value : set) sum += value;

            if (sum != 0) {
                for (GridCell cell : lineCells) {
                    int id = cell.getId();
                    Set<Integer> possibleValues = findPossibleValues(id);
                    if (possibleValues.equals(set))
                        insideCells.add(cell);
                }
                int expectedSum = getExpectedLineSum();
                int otherCellsSum = expectedSum - sum;
                if (insideCells.size() > 0) {
                    ArrayList<GridCell> outsideCells = new ArrayList<>();
                    for (GridCell rowCell : lineCells)
                        if (!insideCells.contains(rowCell)) outsideCells.add(rowCell);

                    int cellCount = outsideCells.size();
                    ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();
                    findCombinations(otherCellsSum, cellCount, combinations, '+');

                    ArrayList<ArrayList<Integer>> noDupsCombinations = new ArrayList<>();
                    for (ArrayList<Integer> combination : combinations) {
                        if (!hasDups(combination)) noDupsCombinations.add(combination);
                    }
                    combinations = noDupsCombinations;

                    ArrayList<ArrayList<Integer>> permuts = new ArrayList<>();
                    for (ArrayList<Integer> combos : combinations) {
                        permutations(combos, new Stack<>(), combos.size(), permuts);
                    }
                    combinations = permuts;

                    ArrayList<ArrayList<Integer>> correctCombinations = new ArrayList<>();
                    for (ArrayList<Integer> combination : combinations) {
                        boolean isCombinationCorrect = true;
                        for (GridCell outsideCell : outsideCells) {
                            int cellIndex = outsideCells.indexOf(outsideCell);
                            int value = combination.get(cellIndex);
                            int cellId = outsideCell.getId();

                            Set<Integer> possibleValues = findPossibleValues(cellId);

                            if (!possibleValues.contains(value)) isCombinationCorrect = false;
                        }
                        if (isCombinationCorrect) {
                            correctCombinations.add(combination);
                        }
                    }

                    if (correctCombinations.size() > 0) {
                        Set<Cage> inlineOutsideCages = new HashSet<>();
                        for (GridCell cell : outsideCells) inlineOutsideCages.add(myGrid.getCellCage(cell));

                        ArrayList<ArrayList<GridCell>> affectedCages = new ArrayList<>();
                        for (Cage cage : inlineOutsideCages) {
                            ArrayList<GridCell> affectedCagedCells = new ArrayList<>();
                            for (GridCell cell : cage.getCageCells())
                                if (outsideCells.contains(cell)) affectedCagedCells.add(cell);
                            affectedCages.add(affectedCagedCells);
                        }

                        ArrayList<Set<ArrayList<Integer>>> splittedCombinations = new ArrayList<>();
                        for (ArrayList<GridCell> cagedInlineCells : affectedCages) {
                            Set<ArrayList<Integer>> cagedInlineCellValues = new HashSet<>();

                            for (ArrayList<Integer> combination : correctCombinations) {
                                ArrayList<Integer> combo = new ArrayList<>();

                                for (GridCell cell : cagedInlineCells) {
                                    int cellIndex = outsideCells.indexOf(cell);
                                    combo.add(combination.get(cellIndex));
                                }
                                cagedInlineCellValues.add(combo);
                            }
                            splittedCombinations.add(cagedInlineCellValues);
                        }

                        int cageId = 0;
                        for (Set<ArrayList<Integer>> arrangements : splittedCombinations) {
                            Cage cage = (Cage) inlineOutsideCages.toArray()[cageId];
                            int cageIndex = myGrid.getCages().indexOf(cage);

                            ArrayList<ArrayList<Integer>> variations = possibleVariations.get(cageIndex);
                            ArrayList<ArrayList<Integer>> tempVars = new ArrayList<>();

                            for (ArrayList<Integer> arrangement : arrangements) {
                                int cellsBr = arrangement.size();
                                for (ArrayList<Integer> variation : variations) {
                                    boolean isVariationOkey = true;
                                    GridCell firstCell = affectedCages.get(cageId).get(0);
                                    int startCellId = myGrid.getCellCage(firstCell).getCageCells().indexOf(firstCell);

                                    for (int i = 0; i < cellsBr; i++) {
                                        if (!variation.get(i + startCellId).equals(arrangement.get(i))) {
                                            isVariationOkey = false;
                                            break;
                                        }
                                    }
                                    if (isVariationOkey) tempVars.add(variation);
                                }
                            }
                            if (tempVars.size() != 0)
                                if (tempVars.size() != possibleVariations.get(cageIndex).size())
                                    possibleVariations.set(cageIndex, tempVars);
                            cageId++;
                        }
                    }
                }
            }
        }
    }

    private boolean tryToGuessCell() {
        ArrayList<ArrayList<ArrayList<Integer>>> backup = getDeepCopy(possibleVariations);

        int size = getSize();
        for (int i = 1; i <= size * size; i++) {
            Set<Integer> possibleValues = findPossibleValues(i);
            GridCell cell = myGrid.getCellById(i);
            Cage cage = myGrid.getCellCage(cell);

            int cageIndex = myGrid.getCages().indexOf(cage);
            int cellIndex = cage.getCageCells().indexOf(cell);

            ArrayList<ArrayList<Integer>> variations = possibleVariations.get(cageIndex);
            ArrayList<ArrayList<Integer>> newVariations = new ArrayList<>();

            int value = (int) possibleValues.toArray()[0];

            for (ArrayList<Integer> variation : variations) {
                if (variation.get(cellIndex) == value) newVariations.add(variation);
            }

            possibleVariations.set(cageIndex, newVariations);
            updateVariations();

            int[][] solution;
            if (checkPermuts() < 10_000L && 0 < checkPermuts()) {
                generateMatrixArrangements();

                ArrayList<int[][]> solutions = scanMatrices();
                solution = solutions.size()==0 ? null : solutions.get(0);

                if (solution == null)
                    possibleVariations = getDeepCopy(backup);
                else return true;
            }
        }
        possibleVariations = getDeepCopy(backup);
        return false;
    }

    public void printAllPossibleValues() {
        int size = getSize();
        for (int i = 0; i < size; i++){
                for(int j=1;j<=size;j++)
                System.out.println(findPossibleValues(i*size + j) + " ; ");

                System.out.println();
        }
    }

    public ArrayList<Set<Integer>> getAllPossibleValues(ArrayList<GridCell> cells) {
        ArrayList<Set<Integer>> possibleValues = new ArrayList<>();
        for (GridCell cell : cells) possibleValues.add(findPossibleValues(cell.getId()));
        return possibleValues;
    }

    private ArrayList<ArrayList<ArrayList<Integer>>> getDeepCopy(ArrayList<ArrayList<ArrayList<Integer>>> original) {
        ArrayList<ArrayList<ArrayList<Integer>>> backup = new ArrayList<>();
        for (ArrayList<ArrayList<Integer>> variations : original) {
            backup.add((ArrayList<ArrayList<Integer>>) variations.clone());
        }
        return backup;
    }
    private boolean isArrangementCorrect(ArrayList<ArrayList<Integer>> arrangement,int index, boolean isRow){
        ArrayList<GridCell> cells =  isRow ? myGrid.getRowCells(index) :  myGrid.getColumnCells(index);
        ArrayList<Cage> inlinedCages = getInlinedCages(cells);

        ArrayList<Integer> values = new ArrayList<>();
        for(GridCell cell : cells){
            Cage cage = myGrid.getCellCage(cell);
            int cageIndex = inlinedCages.indexOf(cage);
            int cellIndex = cage.getCageCells().indexOf(cell);

            values.add(arrangement.get(cageIndex).get(cellIndex));
        }

        return !hasDups(values);
    }
    private boolean tryToGuessRowOrCol(){
        ArrayList<ArrayList<ArrayList<Integer>>>  backup = getDeepCopy(possibleVariations);
        for(int row = 0 ; row < getSize(); row++) {
            ArrayList<GridCell> rowCells = myGrid.getRowCells(row);
            ArrayList<GridCell> colCells = myGrid.getColumnCells(row);

            if(tryToGuessLine(rowCells , backup, row, true)) return true;
            if(tryToGuessLine(colCells , backup, row, false)) return true;
        }
        possibleVariations = getDeepCopy(backup);
        return false;
    }

    private boolean tryToGuessLine(ArrayList<GridCell> cells , ArrayList<ArrayList<ArrayList<Integer>>>  backup,
                                   int index, boolean isRow){
        ArrayList<Cage> inlinedCages = getInlinedCages(cells);
        ArrayList<ArrayList<ArrayList<Integer>>> possibleLineVariations = new ArrayList<>();

        for (Cage cage : inlinedCages) {
            int cageId = myGrid.getCages().indexOf(cage);
            possibleLineVariations.add(possibleVariations.get(cageId));
        }
        long arrangementsCount = 1;
        for(GridCell cell : cells) arrangementsCount *= findPossibleValues(cell.getId()).size();

        if(arrangementsCount < 1000 && 0 < arrangementsCount) {
            ArrayList<ArrayList<ArrayList<Integer>>> lineArrangements = getCombinations(possibleLineVariations);
            for (ArrayList<ArrayList<Integer>> arrangement : lineArrangements)
                if (isArrangementCorrect(arrangement, index , isRow)) {
                    int arrangementId = 0;
                    for (Cage cage : inlinedCages) {
                        int cageId = myGrid.getCages().indexOf(cage);

                        possibleVariations.get(cageId).clear();
                        possibleVariations.get(cageId).add(arrangement.get(arrangementId));
                        arrangementId++;
                    }

                    updateVariations();
                    int[][] solution;
                    if (checkPermuts() < 10_000L && 0 < checkPermuts()) {
                        generateMatrixArrangements();

                        ArrayList<int[][]> solutions = scanMatrices();
                        solution = solutions.size()==0 ? null : solutions.get(0);
                        if (solution == null)
                            possibleVariations = getDeepCopy(backup);
                        else return true;
                    }
                }
            possibleVariations = getDeepCopy(backup);
        }
        return false;
    }


    public ArrayList<Cage> getInlinedCages(ArrayList<GridCell> linedCells){
        ArrayList<Cage> inlinedCages = new ArrayList<>();
        for(GridCell cell : linedCells) {
            Cage cage = myGrid.getCellCage(cell);
            if(!inlinedCages.contains(cage)) inlinedCages.add(cage);
        }
        return inlinedCages;
    }
}
