package me.rerere.matrix.features;

import java.util.List;
import me.rerere.matrix.cloud.CloudRegistration.Actions;

/** Two ordered text passes applied by the response feature composer. */
public interface FeatureRegistrationPipeline extends Actions {
    void firstPass(List<String> featureLines);
    void secondPass(List<String> featureLines);
}
