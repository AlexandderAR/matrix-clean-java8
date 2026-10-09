package me.rerere.matrix.support;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

public final class ConstructorAccessMarker {
    private ConstructorAccessMarker() {
    }

    public ConstructorAccessMarker(DefaultConstructorMarker marker) {
        this();
    }
}
