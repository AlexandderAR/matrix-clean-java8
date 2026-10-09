package me.rerere.matrix.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.rerere.matrix.model.Parameter;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class PrefixedTokenGroupParser {
    private static final String substringResultExpression = "substring(...)".intern();
    public static final PrefixedTokenGroupParser INSTANCE = new PrefixedTokenGroupParser();

    private PrefixedTokenGroupParser() {
    }

    public static Set<Parameter> parseGroups(String[] tokens) {
        Set<Parameter> groups = new HashSet<>();
        int headerLimit = tokens.length - 1;
        for (int index = 0; index < headerLimit; index++) {
            String candidate = tokens[index];
            if (!candidate.startsWith("-")) {
                continue;
            }
            if (candidate.length() <= 1) {
                continue;
            }
            String groupName = candidate.substring(1);
            Intrinsics.checkNotNullExpressionValue(groupName, substringResultExpression);
            List<String> values = new ArrayList<>();
            for (int next = index + 1; next < tokens.length; next++) {
                String value = tokens[next];
                if (value.startsWith("-")) {
                    break;
                }
                values.add(value);
            }
            groups.add(new Parameter(groupName, String.join(" ", values)));
        }
        return groups;
    }

}
