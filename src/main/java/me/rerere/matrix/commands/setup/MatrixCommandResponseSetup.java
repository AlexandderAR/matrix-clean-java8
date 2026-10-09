package me.rerere.matrix.commands.setup;

import java.util.List;
import me.rerere.matrix.compat.scheduler.TrialSchedulerActions;
import me.rerere.matrix.license.LicenseResponseParser.ParsedLicenseResponse;
import me.rerere.matrix.license.LicenseResponseSetupActions;
/** Applies the selected response fields to the Matrix command registration. */
public final class MatrixCommandResponseSetup {
    private MatrixCommandResponseSetup() { }

    public static CommandSetupResult apply(ParsedLicenseResponse parsed, LicenseResponseSetupActions actions,
            TrialSchedulerActions schedulerActions, TrialSchedule trialSchedule) throws Throwable {
        if (parsed == null || actions == null) throw new UnsupportedSetupPathError("missing setup input");
        if (parsed.singleTrialField) {
            if (schedulerActions == null || trialSchedule == null)
                throw new UnsupportedSetupPathError("missing Trial scheduler adapter");
            trialSchedule.run(schedulerActions);
        }
        if (!actions.isAngleCheckEnabled()) actions.setAngleCheckEnabled(true);
        Object matrix = actions.resolveMatrix();
        Object command = actions.pluginCommand("matrix");
        Object executor = actions.createVerifier();
        actions.installVerifier(matrix, executor);
        actions.requireNonNull(command);
        Object commandHandler = actions.verifierForMatrix(matrix);
        actions.setExecutor(command, commandHandler);
        Object tabCompleter = actions.verifierForMatrix(matrix);
        actions.setTabCompleter(command, tabCompleter);
        actions.setSkBoolean(parsed.parsedBoolean);
        List<String> fields = parsed.commaFields;
        actions.setSkList(fields);
        boolean trialPresent = fields.contains("Trial");
        if (trialPresent) {
            actions.setHiBoolean(true);
            actions.setJeBoolean(true);
        }
        return new CommandSetupResult(fields, trialPresent, matrix);
    }
}
