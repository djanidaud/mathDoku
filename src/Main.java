import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Optional;

public class Main extends Application {
    private AnimationTimer timer;

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.getStylesheets().add(getClass().getResource("style.css").toString());
        root.setId("mainBody");

        GridPane gridPane = new GridPane();
        gridPane.setId("gridPane");
        GameLoader gameLoader = new GameLoader(gridPane);
        gameLoader.loadDefaultGrid();

        HBox gridHbox = new HBox(gridPane);
        VBox gridVBox = new VBox(gridHbox);
        root.setCenter(gridVBox);

        gridHbox.setAlignment(Pos.CENTER);
        gridVBox.setAlignment(Pos.CENTER);

        HBox numButtonsBox = new HBox();
        numButtonsBox.setId("numberButtons");
        gameLoader.getGrid().displayNumberButtons(numButtonsBox);

        HBox bottomMenu = new HBox(10);
        bottomMenu.setId("bottomMenu");

        Button undoButton = new Button("Undo");
        undoButton.setId("undoButton");
        undoButton.setOnAction(event -> {
            Grid myGrid = gameLoader.getGrid();
            MovesTracker movesTracker = myGrid.getMovesTracker();
            ArrayList<Object> info = movesTracker.popUndoStack();

            if (info != null) {
                int cellId = (int) info.get(0);
                String value = (String) info.get(1);

                movesTracker.addToRedoStack(cellId, myGrid.getCellById(cellId).getText());
                movesTracker.setDoAddToStack(false);
                GridCell cell = myGrid.getCellById(cellId);
                cell.setText(value);
            }
        });

        Button redoButton = new Button("Redo");
        redoButton.setId("redoButton");
        redoButton.setOnAction(event -> {
            Grid myGrid = gameLoader.getGrid();
            MovesTracker movesTracker = myGrid.getMovesTracker();
            ArrayList<Object> info = movesTracker.popRedoStack();

            if (info != null) {
                int cellId = (int) info.get(0);
                String value = (String) info.get(1);

                movesTracker.addToUndoStack(cellId, myGrid.getCellById(cellId).getText());
                movesTracker.setDoAddToStack(false);
                GridCell cell = myGrid.getCellById(cellId);
                cell.setText(value);
            }
        });

        Label mistakesLabel = new Label("Show mistakes");
        mistakesLabel.setId("mistakesLabel");

        ToggleGroup group = new ToggleGroup();
        ToggleButton on = new ToggleButton("On");
        ToggleButton off = new ToggleButton("Off");
        on.setToggleGroup(group);
        off.setToggleGroup(group);
        on.setSelected(true);

        on.setId("selectedOn");
        off.setId("not-selected");
        on.setUserData("on");
        off.setUserData("off");

        HBox toggleSection = new HBox(mistakesLabel, on, off);
        toggleSection.setId("toggleSection");

        group.selectedToggleProperty().addListener((ov, toggle, new_toggle) -> {
            if (new_toggle == null) {
                toggle.setSelected(true);
            } else {
                MistakeCatcher catcher = gameLoader.getGrid().getCatcher();
                ToggleButton selected = ((ToggleButton) new_toggle);
                ToggleButton notSelected = ((ToggleButton) toggle);

                if (new_toggle.getUserData().equals("on")) {
                    selected.setId("selectedOn");
                    catcher.checkMistakes(true);
                } else {
                    selected.setId("selectedOff");
                    catcher.checkMistakes(false);
                }
                if (toggle != null) notSelected.setId("not-selected");
            }
        });

        Button clear = new Button("Clear");
        clear.setId("clear");
        clear.setOnAction(event -> {
            primaryStage.setFullScreen(false);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Clear confirmation");
            alert.setHeaderText("You are  about to clear your board.");
            alert.setContentText("Do you want to proceed?");
            alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toString());

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK)
                gameLoader.getGrid().resetGrid();
        });

        Label fontSize = new Label("Font Size:");
        Slider slider = new Slider(20, 30, 25);
        HBox sliderBox = new HBox(10, fontSize, slider);

        slider.setMajorTickUnit(5);
        slider.setShowTickLabels(true);
        slider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double n) {
                if (n == 20) return "Small";
                if (n == 25) return "Medium";
                if (n == 30) return "Big";

                return "Expert";
            }
            @Override
            public Double fromString(String s) {
                switch (s) {
                    case "Small":
                        return 20d;
                    case "Medium":
                        return 25d;
                    case "Big":
                        return 30d;
                    case "Expert":
                        return 0d;

                    default:
                        return 3d;
                }
            }
        });

        Button solveButton = new Button("Solve");
        solveButton.setId("solveButton");
        solveButton.setOnAction(event -> gameLoader.getGrid().displaySolution());

        slider.valueProperty().addListener((ov, old_val, new_val) ->
                gameLoader.getGrid().setGridFontSize(new_val.intValue()));


        class Interface {
            private void setupInterface() {
                Grid myGrid = gameLoader.getGrid();
                MistakeCatcher catcher = myGrid.getCatcher();

                myGrid.displayNumberButtons(numButtonsBox);
                if (on.isSelected()) {
                    catcher.checkMistakes(true);
                } else catcher.checkMistakes(false);

                myGrid.setGridFontSize((int) slider.getValue());
                myGrid.updateCellsSize(primaryStage);

                solveButton.setDisable(!myGrid.hasSolution());
                timer.start();
            }
        }

        MenuButton load = new MenuButton("Load...");
        load.setId("load");
        MenuItem loadFile = new MenuItem("Load a game from a file...");
        loadFile.setOnAction(e -> {
            if (gameLoader.loadFile()) new Interface().setupInterface();
        });

        MenuItem loadInput = new MenuItem("Load a game from text input");
        loadInput.setOnAction(e -> {
            primaryStage.setFullScreen(false);
            if (gameLoader.loadTextInput()) new Interface().setupInterface();
        });

        MenuItem loadRandom = new MenuItem("Generate a random game");
        loadRandom.setOnAction(e -> {
            if (gameLoader.createRandomGame()) new Interface().setupInterface();
        });

        load.getItems().addAll(loadFile, loadInput, loadRandom);
        load.setPopupSide(Side.TOP);

        Button settings = new Button("⚙");
        settings.setId("settings");
        settings.setOnAction(e -> {
            primaryStage.setFullScreen(false);
            Dialog dialog = new Dialog();
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toString());

            VBox settingsBox = new VBox(10);
            settingsBox.setId("settingsBox");
            settingsBox.getChildren().addAll(new Label("Settings"), sliderBox);
            dialog.getDialogPane().setContent(settingsBox);

            ButtonType ok = new ButtonType("Done", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(ok);
            dialog.show();

        });

        HBox leftSection = new HBox(5, undoButton, redoButton, load, settings, solveButton);
        HBox rightSection = new HBox(5, toggleSection, clear);


        HBox.setHgrow(rightSection, Priority.ALWAYS);
        HBox.setHgrow(leftSection, Priority.ALWAYS);
        HBox.setHgrow(undoButton, Priority.ALWAYS);
        HBox.setHgrow(redoButton, Priority.ALWAYS);
        HBox.setHgrow(load, Priority.ALWAYS);
        HBox.setHgrow(settings, Priority.ALWAYS);
        HBox.setHgrow(solveButton, Priority.ALWAYS);
        HBox.setHgrow(clear, Priority.ALWAYS);
        HBox.setHgrow(mistakesLabel, Priority.ALWAYS);
        HBox.setHgrow(toggleSection, Priority.ALWAYS);

        bottomMenu.getChildren().addAll(leftSection, new Rectangle(150, 0), rightSection);
        root.setBottom(new VBox(numButtonsBox, bottomMenu));

        timer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long time) {
                if (this.lastUpdate > 100) {
                    Grid myGrid = gameLoader.getGrid();
                    MovesTracker movesTracker = myGrid.getMovesTracker();

                    if (myGrid.checkForVictory()) {
                        timer.stop();
                        primaryStage.setFullScreen(false);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toString());
                        alert.setTitle("VICTORY");
                        alert.setHeaderText("You are victorious!");
                        alert.show();
                        alert.setOnHidden(event -> myGrid.play());
                    }

                    undoButton.setDisable(movesTracker.isUndoStackEmpty());
                    redoButton.setDisable(movesTracker.isRedoStackEmpty());
                }
                this.lastUpdate = time;
            }
        };
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            gameLoader.getGrid().updateCellsSize(primaryStage);
        });
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            gameLoader.getGrid().updateCellsSize(primaryStage);
        });
        timer.start();

        primaryStage.setTitle("mathDoku");
        primaryStage.setScene(new Scene(root, 900, 700));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }
}
