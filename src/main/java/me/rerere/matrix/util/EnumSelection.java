package me.rerere.matrix.util;

import me.rerere.matrix.movement.MoveData;

/** Mutable movement selection backed by a five-element sample buffer. */
public final class EnumSelection {
    private final ObjectSampleBuffer entries;
    private MoveData selected;

    public EnumSelection() {
        this.entries = new ObjectSampleBuffer(MoveData.class, 5);
    }

    public MoveData selected() { return selected; }

    public void select(MoveData value) {
        entries.add(value);
        selected = value;
    }

    public void clear() {
        selected = null;
        entries.clear();
    }

    public MoveData at(int index) {
        return ((MoveData[]) entries.getSamples())[4 - index];
    }
}
