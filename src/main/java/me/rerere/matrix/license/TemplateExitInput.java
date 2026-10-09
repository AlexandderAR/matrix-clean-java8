package me.rerere.matrix.license;

/** Supplies the four template sentinels from the reconstructed runtime input. */
final class TemplateExitInput implements me.rerere.matrix.network.TemplateExitMetadata {
    private final me.rerere.matrix.config.PrefaceRuntimeContext input;

    TemplateExitInput(me.rerere.matrix.config.PrefaceRuntimeContext input) {
        this.input = input;
    }

    @Override
    public String userKey() {
        return input.getUserKey();
    }

    @Override
    public String pluginCloud() {
        return input.getPluginCloud();
    }

    @Override
    public String token() {
        return input.getToken();
    }

    @Override
    public String timestamp() {
        return input.getTimestamp();
    }
}
