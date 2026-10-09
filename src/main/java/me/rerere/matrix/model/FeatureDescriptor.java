package me.rerere.matrix.model;

import java.util.Map;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Feature record with id, informational text, and tooltip values. */
public final class FeatureDescriptor {
    private final String id;
    private final String info;
    private final Map<?, ?> tooltip;

    public FeatureDescriptor(String id, String info, Map<?, ?> tooltip) {
        this.id = id;
        this.info = info;
        this.tooltip = tooltip;
    }

    public String componentId() { return id; }
    public String componentInfo() { return info; }
    public Map<?, ?> componentTooltip() { return tooltip; }

    public static FeatureDescriptor copyDefaults(FeatureDescriptor source, String id, String info,
                                          Map<?, ?> tooltip, int mask) {
        if ((mask & 1) != 0) id = source.id;
        if ((mask & 2) != 0) info = source.info;
        if ((mask & 4) != 0) tooltip = source.tooltip;
        return source.copy(id, info, tooltip);
    }

    public FeatureDescriptor copy(String id, String info, Map<?, ?> tooltip) {
        return new FeatureDescriptor(id, info, tooltip);
    }

    public String render(String id, String info, Map<?, ?> tooltip) {
        return "Feature(id=" + id + ", info=" + info + ", tooltip=" + tooltip + ")";
    }

    @Override public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + info.hashCode();
        result = 31 * result + tooltip.hashCode();
        return result;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FeatureDescriptor)) return false;
        FeatureDescriptor that = (FeatureDescriptor) other;
        return Intrinsics.areEqual(id, that.id)
                && Intrinsics.areEqual(info, that.info)
                && Intrinsics.areEqual(tooltip, that.tooltip);
    }

    @Override public String toString() {
        return "Feature(id=" + id + ", info=" + info + ", tooltip=" + tooltip + ")";
    }

}
