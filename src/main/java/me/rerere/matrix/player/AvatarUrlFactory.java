package me.rerere.matrix.player;

import java.util.UUID;

/** Builds the avatar URL and exposes the original random-index helper. */
public final class AvatarUrlFactory {
    
    private static final long RANDOM_UPPER_BOUND = -6636892338335514625L;

    public AvatarUrlFactory() {
    }

    public final int nextRandomIndex() {
        return (int) (Math.random() * (int) RANDOM_UPPER_BOUND);
    }

    public final String createAvatarUrl(UUID uuid) {
        return "https://crafatar.com/avatars/" + uuid + "?overlay";
    }
}
