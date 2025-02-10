package org.byeautumn.chuachua.undo;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ActionRecorder {
    public static final Material POLY_SELECT_TYPE = Material.CANDLE;
    public static final Material DIA_SELECT_TYPE = Material.CANDLE;
    private final Stack<ActionRecord> editUndoStack = new Stack<>();
    private final Stack<ActionRecord> generationUndoStack = new Stack<>();
    private final Stack<ActionRecord> editRedoStack = new Stack<>();
    private final Stack<ActionRecord> generationRedoStack = new Stack<>();
    private boolean isPolySelection = false;
    private final List<Block> polySelectedBlocks = new ArrayList<Block>();
    private boolean isDiaSelection = false;
    private final List<Block> diaSelectedBlocks = new ArrayList<>(2);

    public void record(ActionRecord record) {
        editRedoStack.empty();
        editUndoStack.add(record);
    }

    public void record(ActionType type, ActionRecord record) {
        if (type == ActionType.EDIT) {
            record(record);
        }
        else if (type == ActionType.GENERATION) {
            generationRedoStack.empty();
            generationUndoStack.add(record);
        }
    }

    private ActionRecord getPreviousGenerationAction() {
        if (this.generationUndoStack.isEmpty()) {
            System.out.println("There is no previous Generation Action.");
            return null;
        }

        ActionRecord record = this.generationUndoStack.pop();
        this.generationRedoStack.add(record);
        return record;
    }

    public ActionRecord getPreviousAction() {
        if (this.editUndoStack.isEmpty()) {
            System.out.println("There is no previous Edit Action.");
            return null;
        }

        ActionRecord record = this.editUndoStack.pop();
        this.editRedoStack.add(record);
        return record;
    }

    public ActionRecord getPreviousAction(ActionType type) {
        if (type == ActionType.EDIT) {
            return getPreviousAction();
        }

        if (type == ActionType.GENERATION) {
            return getPreviousGenerationAction();
        }

        return null;
    }

    public ActionRecord getNextAction() {
        if (this.editRedoStack.isEmpty()) {
            System.out.println("There is no next Edit Action.");
            return null;
        }

        ActionRecord record = this.editRedoStack.pop();
        this.editUndoStack.add(record);
        return record;
    }

    private ActionRecord getNextGenerationAction() {
        if (this.generationRedoStack.isEmpty()) {
            System.out.println("There is no next Generation Action.");
            return null;
        }

        ActionRecord record = this.generationRedoStack.pop();
        this.generationUndoStack.add(record);
        return record;
    }

    public ActionRecord getNextAction(ActionType type) {
        if (type == ActionType.EDIT) {
            return getNextAction();
        }

        if (type == ActionType.GENERATION) {
            return getNextGenerationAction();
        }

        return null;
    }

    public boolean isPolySelection() {
        return this.isPolySelection;
    }

    public void setPolySelection(boolean b) {
        this.isPolySelection = b;
    }

    public void resetPolySelection() {
        setPolySelection(false);
        this.polySelectedBlocks.clear();
    }

    public void polySelect(Block block) {
        if (!isPolySelection()) {
            System.err.println("PolySelect operation cannot be done since it is not in the selection model.");
            return;
        }

        this.polySelectedBlocks.add(block);
    }

    public Block getLastPolySelection() {
        if (this.polySelectedBlocks.isEmpty()) {
            return null;
        }
        return this.polySelectedBlocks.get(this.polySelectedBlocks.size() - 1);
    }

    public void cancelLastPolySelection() {
        if (this.polySelectedBlocks.isEmpty()) {
            return;
        }
        this.polySelectedBlocks.remove(this.polySelectedBlocks.size() - 1);
    }

    public List<Block> getPolySelectedBlocks() {
        return polySelectedBlocks;
    }

    public boolean isDiaSelection() {
        return this.isDiaSelection;
    }

    public void setDiaSelection(boolean diaSelection) {
        isDiaSelection = diaSelection;
    }

    public void resetDiaSelection() {
        setDiaSelection(false);
        this.diaSelectedBlocks.clear();
    }

    public void diaSelect(Block block) {
        if (!isDiaSelection()) {
            System.err.println("DiaSelect operation cannot be done since it is not the selection model.");
            return;
        }
        if (getDiaSelectedBlocks().size() > 1) {
            System.err.println("Only 2 selections are allowed in DiaSelection. Please either delete previous selection or cancel the whole DiaSelection and start over.");
            return;
        }

        this.diaSelectedBlocks.add(block);
    }

    public List<Block> getDiaSelectedBlocks() {
        return this.diaSelectedBlocks;
    }

    public Block getLastDiaSelection() {
        if (getDiaSelectedBlocks().isEmpty()) {
            return null;
        }
        return getDiaSelectedBlocks().get(getDiaSelectedBlocks().size() - 1);
    }

    public void cancelLastDiaSelection() {
        if (getDiaSelectedBlocks().isEmpty()) {
            return;
        }
        getDiaSelectedBlocks().remove(getDiaSelectedBlocks().size() - 1);
    }
}
