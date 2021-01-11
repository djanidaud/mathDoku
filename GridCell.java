import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class GridCell {
    private int id;
    private StackPane stackPane;
    private Grid myGrid;

    public GridCell(int id, Grid myGrid) {
        this.id = id;
        this.myGrid = myGrid;
        this.stackPane = new StackPane();

        Rectangle rect = new Rectangle();
        rect.setStyle("-fx-fill: white; -fx-stroke: black; -fx-stroke-width: 0.5;");
        TextField textField = new TextField();

        textField.setOnMouseClicked(e -> {
            myGrid.setSelectedCell(this);
            textField.positionCaret(1);
        });

        Label label = new Label();
        label.setPadding(new Insets(5));
        StackPane.setAlignment(label, Pos.TOP_LEFT);
        stackPane.getChildren().addAll(rect, label, textField);

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            MovesTracker tracker = myGrid.getMovesTracker();
            textField.requestFocus();
            myGrid.setSelectedCell(this);

            if (!newValue.matches("\\d*"))
                setText(newValue.replaceAll("[^\\d]", ""));
            else {
                oldValue = oldValue.replaceAll("[^\\d]", "");

                if (!oldValue.equals(newValue)) {
                    if (!isValueInBounds(newValue)) {
                        int oldNumber = oldValue.equals("") ? 1 : Integer.parseInt(oldValue);

                        int newNumber = Integer.parseInt(newValue);
                        int value = newNumber - 10 * oldNumber;

                        if (isValueInBounds(value + "") && value != oldNumber) {
                            setText(value + "");
                            tracker.addToUndoStack(id, oldValue);
                        } else
                            setText(oldValue);
                    } else if (isValueInBounds(oldValue))
                        tracker.addToUndoStack(id, oldValue);
                }
            }
            MistakeCatcher catcher = myGrid.getCatcher();
            if (catcher.doShowMistakes()) {
                myGrid.repaintCells();
                catcher.displayAllMistakes();
                catcher.displayAllCageErrors();
            }
        });
    }

    public int getId() {
        return id;
    }

    public StackPane getStackPane() {
        return stackPane;
    }

    public TextField getTextField() {
        return (TextField) stackPane.getChildren().get(2);
    }

    public Label getLabel() {
        return (Label) stackPane.getChildren().get(1);
    }

    public Rectangle getRectangle() {
        return (Rectangle) stackPane.getChildren().get(0);
    }

    public int getValue() {
        return getText().equals("") ? 0 : Integer.parseInt(getText());
    }

    public boolean isFilled() {
        return getValue() != 0;
    }

    public boolean isValueInBounds(String value) {
        int maxValue = myGrid.getSize();
        int number = value.equals("") ? 1 : Integer.parseInt(value);
        return 1 <= number && number <= maxValue;
    }

    public void setSize(int size) {
        getTextField().setMinWidth(size);
        getTextField().setMinHeight(size);
        getTextField().setPrefWidth(size);
        getTextField().setPrefHeight(size);

        getRectangle().setWidth(size);
        getRectangle().setHeight(size);

        getStackPane().setPrefWidth(size);
        getStackPane().setPrefHeight(size);
    }

    public String getText() {
        return getTextField().getText();
    }

    public void setText(String str) {
        getTextField().setText(str);
    }
}
