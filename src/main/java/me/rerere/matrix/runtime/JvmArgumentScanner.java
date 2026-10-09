package me.rerere.matrix.runtime;

import java.lang.management.RuntimeMXBean;
import java.util.List;
import me.rerere.matrix.util.jvm.AgentPrefixMatch;
import me.rerere.matrix.util.jvm.DirectArgumentMatch;
import me.rerere.matrix.util.jvm.JvmArgumentPredicates;

public final class JvmArgumentScanner {
    private JvmArgumentScanner() {}

    public static JvmArgumentMatch scan(RuntimeMXBean bean) {
        DirectArgumentMatch direct = JvmArgumentPredicates.selectedDirectContains(bean);
        if (direct == DirectArgumentMatch.NO_VERIFY) {
            return JvmArgumentMatch.NO_VERIFY;
        }
        if (direct == DirectArgumentMatch.XDEBUG) {
            return JvmArgumentMatch.XDEBUG;
        }

        List<String> inputArguments = bean.getInputArguments();
        for (String argument : inputArguments) {
            AgentPrefixMatch prefix = JvmArgumentPredicates.selectedPrefixItem(argument);
            if (prefix == AgentPrefixMatch.JDWP_AGENTLIB) {
                return JvmArgumentMatch.JDWP_AGENTLIB;
            }
            if (prefix == AgentPrefixMatch.AGENTPATH) {
                return JvmArgumentMatch.AGENTPATH;
            }
            if (prefix == AgentPrefixMatch.XRUN_JDWP) {
                return JvmArgumentMatch.XRUN_JDWP;
            }
        }
        return JvmArgumentMatch.NONE;
    }
}
