package me.rerere.matrix.network.packet;

import java.util.Set;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import me.rerere.matrix.thirdparty.kotlin.collections.SetsKt;

/** Entity types whose hitboxes use the nonstandard packet representation. */
public final class NonstandardHitboxEntityTypes {
    private static final Set<EntityType> TYPES = SetsKt.setOf(new EntityType[] {
            EntityTypes.END_CRYSTAL,
            EntityTypes.ITEM_FRAME,
            EntityTypes.PAINTING,
            EntityTypes.ENDER_DRAGON
    });
    private NonstandardHitboxEntityTypes() { }

    public static Set<EntityType> getTypes() { return TYPES; }
}
