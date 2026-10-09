package me.rerere.matrix.version;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl;
import me.rerere.matrix.version.VersionText;

public final class VersionMajorProperty extends PropertyReference1Impl {
    public static final VersionMajorProperty INSTANCE = new VersionMajorProperty();

    public VersionMajorProperty() {
        super(VersionText.class, "major", "getMajor-impl(Ljava/lang/String;)I", 0);
    }

    @Override
    public Object get(Object receiver) {
        VersionText version = (VersionText) receiver;
        String value = version.versionText();
        return Integer.valueOf(VersionText.majorVersion(
                value));
    }
}
