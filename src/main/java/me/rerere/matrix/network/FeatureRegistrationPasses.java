package me.rerere.matrix.network;

import java.util.List;
import me.rerere.matrix.cloud.CloudRegistration.Actions;

/** Two ordered text passes applied to registered response features. */
public interface FeatureRegistrationPasses extends Actions {
    void firstPass(List<?> featureLines);
    void secondPass(List<?> featureLines);
}
