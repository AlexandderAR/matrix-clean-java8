package me.rerere.matrix.config;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Describes a Matrix program by its display and uninstall strings. */
public final class ProgramInfo {
    private final String uninstallString;
    private final String displayName;

    public ProgramInfo(String displayName, String uninstallString) {
        this.displayName = displayName;
        this.uninstallString = uninstallString;
    }

    public final String component1() {
        return displayName;
    }

    public final String getDisplayName() {
        return displayName;
    }

    public final String component2() {
        return uninstallString;
    }

    public final String getUninstallString() {
        return uninstallString;
    }

    public final ProgramInfo copy(String displayName, String uninstallString) {
        return new ProgramInfo(displayName, uninstallString);
    }

    public static ProgramInfo copyWithDefaults(ProgramInfo source, String displayName,
                                               String uninstallString, int mask) {
        if ((mask & 1) != 0) {
            displayName = source.displayName;
        }
        if ((mask & 2) != 0) {
            uninstallString = source.uninstallString;
        }
        return source.copy(displayName, uninstallString);
    }

    @Override
    public int hashCode() {
        return displayName.hashCode() * 31 + uninstallString.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProgramInfo)) {
            return false;
        }
        ProgramInfo that = (ProgramInfo) other;
        return Intrinsics.areEqual(displayName, that.displayName)
                && Intrinsics.areEqual(uninstallString, that.uninstallString);
    }

    @Override
    public String toString() {
        return new StringBuilder("ProgramInfo(displayName=")
                .append(displayName)
                .append(", uninstallString=")
                .append(uninstallString)
                .append(")")
                .toString();
    }

}
