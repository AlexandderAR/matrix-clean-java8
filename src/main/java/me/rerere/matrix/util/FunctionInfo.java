package me.rerere.matrix.util;

import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.commands.condition.CommandExpressionParser;

/** A parsed function declaration and its positional/named argument accessors. */
public final class FunctionInfo {
    private final String name;
    private final List<String> arguments;
    private final List<String> indexedArguments;
    private final Map<String, String> namedArguments;

    public FunctionInfo(String name, List<String> arguments, List<String> indexedArguments,
                        Map<String, String> namedArguments) {
        this.name = name;
        this.arguments = arguments;
        this.indexedArguments = indexedArguments;
        this.namedArguments = namedArguments;
    }

    public static FunctionInfo create(String name, List<String> arguments,
                                      List<String> indexedArguments,
                                      Map<String, String> namedArguments) {
        return new FunctionInfo(name, arguments, indexedArguments, namedArguments);
    }

    public String getName() { return name; }
    public List<String> getArguments() { return arguments; }
    public List<String> getIndexedArguments() { return indexedArguments; }
    public Map<String, String> getNamedArguments() { return namedArguments; }

    public String getNamedValue(String key) {
        return namedArguments.get(key);
    }

    public String requiredIndexedArgument(int index) {
        String value = CollectionsKt.getOrNull(indexedArguments, index);
        if (value == null) {
            throw new IllegalArgumentException(argumentCountError(name, index + 1, indexedArguments.size()));
        }
        return value;
    }

    private static String argumentCountError(String functionName, int required, int actual) {
        return "Function " + functionName + " requires at least " + required
                + " arguments, but only " + actual + " arguments were provided";
    }

    public boolean indexedBoolean(int index) {
        return Boolean.parseBoolean(requiredIndexedArgument(index));
    }

    public int indexedInteger(int index) {
        return Integer.parseInt(requiredIndexedArgument(index));
    }

    public double indexedDouble(int index) {
        return Double.parseDouble(requiredIndexedArgument(index));
    }

    public Player indexedPlayer(int index) {
        return Bukkit.getPlayer(requiredIndexedArgument(index));
    }

    public String indexedArgument(int index) {
        return requiredIndexedArgument(index);
    }

    public FunctionInfo indexedFunction(int index) {
        String argument = requiredIndexedArgument(index);
        return CommandExpressionParser.parseCommand(argument);
    }

    public FunctionInfo namedFunction(String key) {
        String value = getNamedValue(key);
        return value == null ? null
                : CommandExpressionParser.parseCommand(value);
    }

    public Boolean namedBoolean(String key) {
        String value = getNamedValue(key);
        return value == null ? null : Boolean.valueOf(Boolean.parseBoolean(value));
    }

    public Double namedDouble(String key) {
        String value = getNamedValue(key);
        return value == null ? null : Double.valueOf(Double.parseDouble(value));
    }

    public Player namedPlayer(String key) {
        String value = getNamedValue(key);
        return value == null ? null : Bukkit.getPlayer(value);
    }

    public Integer namedInteger(String key) {
        String value = getNamedValue(key);
        return value == null ? null : Integer.valueOf(Integer.parseInt(value));
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + arguments.hashCode();
        result = 31 * result + indexedArguments.hashCode();
        return 31 * result + namedArguments.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FunctionInfo)) return false;
        FunctionInfo that = (FunctionInfo) other;
        return Intrinsics.areEqual(name, that.name)
                && Intrinsics.areEqual(arguments, that.arguments)
                && Intrinsics.areEqual(indexedArguments, that.indexedArguments)
                && Intrinsics.areEqual(namedArguments, that.namedArguments);
    }

    @Override
    public String toString() {
        String text = "\n            |FunctionInfo(\n            |    name = " + name
                + ",\n            |    args = " + arguments
                + ",\n            |    indexedArgs = " + indexedArguments
                + ",\n            |    namedArgs = " + namedArguments
                + "\n            |)\n        ";
        return me.rerere.matrix.thirdparty.kotlin.text.StringsKt.trimIndent(text);
    }
}
