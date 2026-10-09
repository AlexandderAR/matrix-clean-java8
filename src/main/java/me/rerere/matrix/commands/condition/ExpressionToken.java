package me.rerere.matrix.commands.condition;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.config.MetadataKind;

public final class ExpressionToken {
    private final String value;
    private final MetadataKind type;
    public ExpressionToken(String value, MetadataKind type) {
        this.value = value;
        this.type = type;
    }

    public final ExpressionToken copy(String value, MetadataKind type) {
        return new ExpressionToken(value, type);
    }
    public static ExpressionToken copyDefault(ExpressionToken original, String value,
                                               MetadataKind type, int defaultMask) {
        if ((defaultMask & 1) != 0) value = original.value;
        if ((defaultMask & 2) != 0) type = original.type;
        return original.copy(value, type);
    }
    public final String getValue() { return value; }
    public final MetadataKind getType() { return type; }
    public final String componentValue() { return value; }
    public final MetadataKind componentType() { return type; }
    @Override
    public String toString() {
        return "Token(value=" + value + ", type=" + type + ")";
    }

    @Override
    public int hashCode() {
        return value.hashCode() * 31 + type.hashCode();
    }

    @Override public boolean equals(Object candidate) {
        if (this == candidate) return true;
        if (!(candidate instanceof ExpressionToken)) return false;
        ExpressionToken otherToken = (ExpressionToken) candidate;
        return Intrinsics.areEqual(value, otherToken.value) && type == otherToken.type;
    }
}
