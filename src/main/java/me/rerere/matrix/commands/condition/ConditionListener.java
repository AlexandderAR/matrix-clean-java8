package me.rerere.matrix.commands.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.util.NamedTimestamp;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.commands.condition.ActionProfile;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckRuntimeState;

/** Loads and evaluates configured module combinations. */
public class ConditionListener extends CheckRuntimeState {
    private Set<ActionProfile> combinedConditions;

    public ConditionListener(PlayerCheckContext configuration) {
        super(configuration);
        this.setCheckType(CheckCategory.KILLAURA);
        this.loadCombinedRules();
    }

    private static String timeKey(String ruleName) {
        return new StringBuilder(ruleName).append(".time").toString();
    }

    private static String combineFailure(String conditionName) {
        return "(combine) failed " + conditionName + " check";
    }

    private static String conditionKey(String ruleName) {
        return new StringBuilder(ruleName).append(".condition").toString();
    }

    private static String actionKey(String ruleName) {
        return new StringBuilder(ruleName).append(".action").toString();
    }

    private static void appendEntry(List<NamedTimestamp> entries, String conditionName) {
        entries.add(new NamedTimestamp(conditionName, 0L));
    }

    private static boolean isWithinLimit(ActionProfile rule, NamedTimestamp entry) {
        return MillisecondSampler.currentTimeMillis() - entry.getTimestamp() <= rule.firstOption();
    }

    @Override
    public void onCheckNotice(CheckRuntimeState sourceCheck, String componentName,
                              String detail, int violationWeight) {
        for (ActionProfile condition : combinedConditions) {
            Optional<NamedTimestamp> found = condition.findNamedByTimestamp(componentName);
            if (found.isPresent()) {
                NamedTimestamp deadline = found.get();
                deadline.setTimestamp(MillisecondSampler.currentTimeMillis());
                List<NamedTimestamp> entries = condition.entries();
                boolean allWithinLimit = entries.stream().allMatch(entry -> isWithinLimit(condition, entry));
                if (allWithinLimit) {
                    String conditionName = condition.name();
                    String message = combineFailure(conditionName);
                    this.flag("ka.cbn", message, condition.secondOption());
                    condition.closeEntries();
                }
            }
        }
    }

    public void loadCombinedRules() {
        combinedConditions = new HashSet<>();
        ConfigurationSection section = configuration().getConfigurationSection("modules.combine");
        if (section == null) return;
        for (String ruleName : section.getKeys(false)) {
            List<String> conditionNames = section.getStringList(conditionKey(ruleName));
            int timeLimit = section.getInt(timeKey(ruleName));
            int action = section.getInt(actionKey(ruleName));
            List<NamedTimestamp> entries = new ArrayList<>();
            conditionNames.forEach(conditionName -> appendEntry(entries, conditionName));
            combinedConditions.add(new ActionProfile(ruleName, timeLimit, entries, action));
        }
    }

}
