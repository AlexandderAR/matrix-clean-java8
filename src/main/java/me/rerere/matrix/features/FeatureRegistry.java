package me.rerere.matrix.features;

import java.util.Collection;
import me.rerere.matrix.util.LongCollectionTransform;

/** Singleton feature registry retaining its collection-transform ABI marker. */
public final class FeatureRegistry implements LongCollectionTransform {
    public static final FeatureRegistry INSTANCE;

    private FeatureRegistry() { }

    @Override
    public Collection apply(long key, Collection values) {
        throw new AbstractMethodError("me.rerere.matrix.features.FeatureRegistry.apply(JLjava/util/Collection;)Ljava/util/Collection;");
    }

    static {
        INSTANCE = new FeatureRegistry();
    }
}
