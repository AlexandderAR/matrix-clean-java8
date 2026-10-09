package me.rerere.matrix.network;

import java.util.Map;
import me.rerere.matrix.model.RecordProjection;

/** Immutable five-component record exposed through the shared metadata projection. */
public final class ResponseMetadata implements RecordProjection {
    private final String primaryText;
    private final long firstValue;
    private final long secondValue;
    private final String secondaryText;
    private final Map<?, ?> attributes;

    private ResponseMetadata(String primaryText, long firstValue, long secondValue,
                             String secondaryText, Map<?, ?> attributes) {
        this.primaryText = primaryText;
        this.firstValue = firstValue;
        this.secondValue = secondValue;
        this.secondaryText = secondaryText;
        this.attributes = attributes;
    }

    public ResponseMetadata(String primaryText, long firstValue, long secondValue,
                            String secondaryText, Map<?, ?> attributes,
                            me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) {
        this(primaryText, firstValue, secondValue, secondaryText, attributes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getAttributes() { return (Map<String, String>) attributes; }
    @Override public String getPrimaryText() { return primaryText; }
    @Override public long getPrimaryValue() { return firstValue; }
    @Override public long getSecondaryValue() { return secondValue; }
    @Override public String getSecondaryText() { return secondaryText; }

    public Map<?, ?> attributes() { return attributes; }
    public long firstValue() { return firstValue; }
    public long secondValue() { return secondValue; }
    public String primaryText() { return primaryText; }
    public String secondaryText() { return secondaryText; }
}
