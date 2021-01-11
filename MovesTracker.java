import java.util.ArrayList;
import java.util.Stack;

public class MovesTracker {
    private Stack<ArrayList<Object>> undoStack;
    private Stack<ArrayList<Object>> redoStack;
    private boolean doAddToStack;

    public MovesTracker() {
        undoStack = new Stack<>();
        redoStack = new Stack<>();
        doAddToStack = true;
    }

    public void setDoAddToStack(boolean doAddToStack) {
        this.doAddToStack = doAddToStack;
    }

    public void addToUndoStack(int cellId, String value) {
        if (doAddToStack) {
            ArrayList<Object> info = new ArrayList<>();
            info.add(cellId);
            info.add(value);

            if (!undoStack.isEmpty()) {
                ArrayList<Object> lastEntry = undoStack.peek();
                int lastId = (int) lastEntry.get(0);
                String lastValue = (String) lastEntry.get(1);

                if (lastId != cellId || !lastValue.equals(value))
                    undoStack.add(info);
            } else
                undoStack.add(info);
        } else doAddToStack = true;
    }

    public ArrayList<Object> popUndoStack() {
        if (!undoStack.isEmpty())
            return undoStack.pop();
        return null;
    }

    public ArrayList<Object> popRedoStack() {
        if (!redoStack.isEmpty())
            return redoStack.pop();
        return null;
    }

    public boolean isUndoStackEmpty() {
        if (undoStack.isEmpty()) doAddToStack = true;
        return undoStack.isEmpty();
    }

    public boolean isRedoStackEmpty() {
        return redoStack.isEmpty();
    }

    public void restartTracker() {
        undoStack.clear();
        redoStack.clear();
        doAddToStack = true;
    }

    public void addToRedoStack(int cellId, String value) {
        ArrayList<Object> info = new ArrayList<>();
        info.add(cellId);
        info.add(value);
        redoStack.add(info);
    }
}
