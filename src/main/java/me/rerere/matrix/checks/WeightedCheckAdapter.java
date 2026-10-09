package me.rerere.matrix.checks;

import me.rerere.matrix.player.PlayerCheckContext;

/** Adapter binding a check to the canonical weight category. */
public final class WeightedCheckAdapter extends CheckRuntimeState {
    public WeightedCheckAdapter(PlayerCheckContext playerCheckContext) {
        super(playerCheckContext, CheckCategory.BADPACKETS);
    }
}
