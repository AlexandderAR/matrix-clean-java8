package me.rerere.matrix.listeners;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.player.PlayerCollectionLookup;

public final class ListenerRegistry {
    private final List<PlayerCollectionLookup> listeners;

    public final void addListener(PlayerCollectionLookup listener) {
        listeners.add(listener);
    }

    public final void removeListener(PlayerCollectionLookup listener) {
        listeners.remove(listener);
    }

    public ListenerRegistry() {
        this.listeners = new ArrayList<PlayerCollectionLookup>();
    }
}
