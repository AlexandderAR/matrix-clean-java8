package me.rerere.matrix.license;

import java.lang.invoke.MethodHandle;

/** Preserves the invokedynamic factory-linking SAM contract. */
@FunctionalInterface
public interface TrialLinker {
    MethodHandle linkFactory(String name);
}
