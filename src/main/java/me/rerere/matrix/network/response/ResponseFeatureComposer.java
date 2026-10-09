package me.rerere.matrix.network.response;

import java.util.List;
import me.rerere.matrix.commands.setup.CommandSetupResult;
import me.rerere.matrix.commands.setup.MatrixCommandResponseSetup;
import me.rerere.matrix.commands.setup.TrialSchedule;
import me.rerere.matrix.commands.setup.UnsupportedSetupPathError;
import me.rerere.matrix.compat.scheduler.TrialSchedulerActions;
import me.rerere.matrix.license.LicenseResponseParser.ParsedLicenseResponse;
import me.rerere.matrix.network.FeatureRegistrationPasses;
import me.rerere.matrix.cloud.CloudRegistration;
import me.rerere.matrix.license.LicenseResponseSetupActions;

/** Executes the selected parse's setup and two feature passes, then stops at the cloud boundary. */
public final class ResponseFeatureComposer {
    private ResponseFeatureComposer() {}

    public static StoppedAtPostCloudBoundary continueAfterSelectedParse(
            ParsedLicenseResponse parsed,
            LicenseResponseSetupActions setupActions,
            TrialSchedulerActions trialActions,
            FeatureRegistrationPasses featureActions,
            TrialSchedule trialSchedule) throws Throwable {
        if (featureActions == null) {
            throw new UnsupportedSetupPathError("missing feature actions");
        }
        CommandSetupResult ready =
                MatrixCommandResponseSetup.apply(parsed, setupActions, trialActions, trialSchedule);
        List<String> fields = ready.fields;
        featureActions.firstPass(fields);
        featureActions.secondPass(fields);
        boolean cloudMatched = CloudRegistration.applySelected(fields, featureActions);
        return new StoppedAtPostCloudBoundary(ready, cloudMatched);
    }

    public static final class StoppedAtPostCloudBoundary {
        public final CommandSetupResult setup;
        public final boolean cloudMatched;

        StoppedAtPostCloudBoundary(CommandSetupResult setup, boolean cloudMatched) {
            this.setup = setup;
            this.cloudMatched = cloudMatched;
        }
    }
}
