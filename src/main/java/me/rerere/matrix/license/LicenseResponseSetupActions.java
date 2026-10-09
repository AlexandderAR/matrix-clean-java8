package me.rerere.matrix.license;

import java.util.List;

/** Operations needed to assemble the selected response setup. */
public interface LicenseResponseSetupActions {
    boolean isAngleCheckEnabled();
    void setAngleCheckEnabled(boolean angleCheckEnabled);
    Object resolveMatrix();
    Object pluginCommand(String commandName);
    Object createVerifier();
    void installVerifier(Object inventory, Object verifier);
    void requireNonNull(Object requiredValue);
    Object verifierForMatrix(Object matrix);
    void setExecutor(Object command, Object commandExecutor);
    void setTabCompleter(Object command, Object tabCompleter);
    void setSkBoolean(boolean value);
    void setSkList(List<?> values);
    void setHiBoolean(boolean value);
    void setJeBoolean(boolean value);
}
