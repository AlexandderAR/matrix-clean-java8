package me.rerere.matrix.util.reflect;

import java.util.ArrayList;

/** Base node for predicates applied to reflected members. */
public abstract class ReflectiveMemberMatcher<Candidate> {
    private final ArrayList<ReflectiveMemberMatcher<Candidate>> predicates = new ArrayList<>();

    public abstract boolean test(Candidate candidate);

    public final ArrayList<ReflectiveMemberMatcher<Candidate>> predicates() {
        return predicates;
    }
}
