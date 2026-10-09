package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl;
import me.rerere.matrix.version.VersionText;

/** Kotlin reflection reference for the minor-version property. */
public final class VersionMinorPropertyReference extends PropertyReference1Impl {
    public static final VersionMinorPropertyReference INSTANCE;

    static {
        INSTANCE = new VersionMinorPropertyReference();
    }

    public VersionMinorPropertyReference() {
        super(VersionText.class, "minor", "getMinor-impl(Ljava/lang/String;)I", 0);
    }

    @Override
    public Object get(Object receiver) {
        VersionText version = (VersionText) receiver;
        String versionText = version.versionText();
        int minor = VersionText.minorVersion(versionText);
        return Integer.valueOf(minor);
    }

}
