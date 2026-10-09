package me.rerere.matrix.util.jvm;

import java.lang.management.RuntimeMXBean;
import java.util.List;

/** Checks startup arguments for selected verification and debugging options. */
public final class JvmArgumentPredicates {
    private JvmArgumentPredicates() { }

    public static DirectArgumentMatch selectedDirectContains(RuntimeMXBean runtime) {
        List<String> arguments = runtime.getInputArguments();
        if (arguments.contains("-noverify")) {
            return DirectArgumentMatch.NO_VERIFY;
        }
        arguments = runtime.getInputArguments();
        if (arguments.contains("-Xdebug")) {
            return DirectArgumentMatch.XDEBUG;
        }
        return DirectArgumentMatch.NONE;
    }

    public static AgentPrefixMatch selectedPrefixItem(String item) {
        if (item == null) {
            throw new IllegalArgumentException("null item outside selected IsInstanceOf(String) path");
        }
        if (item.startsWith("-agentlib:jdwp")) {
            return AgentPrefixMatch.JDWP_AGENTLIB;
        }
        if (item.startsWith("-agentpath")) {
            return AgentPrefixMatch.AGENTPATH;
        }
        if (item.startsWith("-Xrunjdwp")) {
            return AgentPrefixMatch.XRUN_JDWP;
        }
        return AgentPrefixMatch.NONE;
    }
}
