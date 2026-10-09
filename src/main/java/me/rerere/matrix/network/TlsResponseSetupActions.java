package me.rerere.matrix.network;

import java.util.List;

/** Operations needed to assemble the selected response setup. */
public interface TlsResponseSetupActions {
    boolean isAngleCheckEnabled();
    void setAngleCheckEnabled(boolean enabled);
    Object resolveMatrix();
    Object pluginCommand(String name);
    Object createVerifier();
    void installVerifier(Object inventory, Object verifier);
    void checkNotNull(Object value);
    Object verifierForMatrix(Object matrix);
    void setExecutor(Object command, Object executor);
    void setTabCompleter(Object command, Object completer);
    void setSkBoolean(boolean value);
    void setSkList(List<?> values);
    void setHiBoolean(boolean value);
    void setJeBoolean(boolean value);
}
