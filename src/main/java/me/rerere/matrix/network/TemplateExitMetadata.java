package me.rerere.matrix.network;

/** Metadata required to finish the template exit response. */
public interface TemplateExitMetadata {
    String userKey();
    String pluginCloud();
    String token();
    String timestamp() throws Throwable;
}
