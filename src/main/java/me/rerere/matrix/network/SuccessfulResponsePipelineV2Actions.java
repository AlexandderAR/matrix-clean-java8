package me.rerere.matrix.network;

import me.rerere.matrix.compat.scheduler.TrialSchedulerActions;
import me.rerere.matrix.license.LicenseResponseSetupActions;

/** Combined operations required by the second successful-response pipeline. */
public interface SuccessfulResponsePipelineV2Actions
        extends LicenseResponseSetupActions, TrialSchedulerActions, FeatureRegistrationPasses {
}
