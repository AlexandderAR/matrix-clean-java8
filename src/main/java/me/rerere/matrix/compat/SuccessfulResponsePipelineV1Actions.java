package me.rerere.matrix.compat;

import me.rerere.matrix.network.TlsResponseSetupActions;
import me.rerere.matrix.features.FeatureRegistrationPipeline;

/** Combined operations required by the selected successful-response pipeline. */
public interface SuccessfulResponsePipelineV1Actions
        extends TlsResponseSetupActions, FeatureRegistrationPipeline {
}
