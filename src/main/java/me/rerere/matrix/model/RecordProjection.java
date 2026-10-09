package me.rerere.matrix.model;

import java.util.Map;

/** Read-only projection of the opaque record used by the command checks. */
public interface RecordProjection {
    Map<String, String> getAttributes();

    String getPrimaryText();

    long getPrimaryValue();

    long getSecondaryValue();

    String getSecondaryText();
}
