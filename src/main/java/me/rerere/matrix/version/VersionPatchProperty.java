package me.rerere.matrix.version;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl;

public final class VersionPatchProperty extends PropertyReference1Impl {
    public static final VersionPatchProperty INSTANCE;

    static {
        INSTANCE = new VersionPatchProperty();
    }

    public VersionPatchProperty() {
        super(VersionText.class, "patch", "getPatch-impl(Ljava/lang/String;)I", 0);
    }

    @Override
    public Object get(Object receiver) {
        VersionText version = (VersionText) receiver;
        String patchText = version.versionText();
        int patch = VersionText.patchVersion(patchText);
        return Integer.valueOf(patch);
    }
}
