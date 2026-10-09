package me.rerere.matrix.commands.condition;

import java.util.List;
import me.rerere.matrix.config.NamedListSource;
import me.rerere.matrix.util.IntegerArrayCache;

public final class FixedListValue extends IntegerArrayCache {
    private List<?> values;
    public FixedListValue(NamedListSource source) {
        super(null);
        values = source.lookup();
    }
    public final void setValues(List<?> values) { this.values = values; }
    public final List<?> getValues() { return values; }
    public final List<?> evaluate(Object context, Object parameter) { return values; }
}
