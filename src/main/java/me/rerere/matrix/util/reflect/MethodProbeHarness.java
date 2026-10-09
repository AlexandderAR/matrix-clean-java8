package me.rerere.matrix.util.reflect;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.org.objectweb.asm.ClassReader;
import me.rerere.matrix.thirdparty.org.objectweb.asm.ClassVisitor;
import me.rerere.matrix.thirdparty.org.objectweb.asm.MethodVisitor;
import me.rerere.matrix.util.ClassPredicateSet;
import me.rerere.matrix.util.ReflectionGreetingSample;
import me.rerere.matrix.util.bytecode.InstructionLoggingVisitor;
import me.rerere.matrix.util.bytecode.MatrixExactMethodVisitor;

/** Reflection and ASM probe used by the embedded matcher diagnostics. */
public final class MethodProbeHarness {
    private static int diagnosticMode;
    static {
        diagnosticMode = 41;
    }

    public static void main(String[] arguments) {
        Class<?> target = verifyClass(ReflectionGreetingSample.class, new Function1() {
            @Override public Object invoke(Object value) {
                return configureClassMatcher((ClassPredicateSet) value);
            }
        });
        Set<Method> candidates = selectMethods(target, new Function1() {
            @Override public Object invoke(Object value) {
                return configureMethodMatcher((MethodMatcherGroup) value);
            }
        });
        Method selected = (Method) CollectionsKt.first(candidates);
        System.out.println(selected);
        inspectMethod(selected, new InstructionLoggingVisitor());
    }

    public static Set<Method> selectMethods(Class<?> target, Function1 matcherInitializer) {
        MethodMatcherGroup matcher = new MethodMatcherGroup();
        matcherInitializer.invoke(matcher);

        Method[] declaredMethods = target.getDeclaredMethods();
        List<Method> matches = new ArrayList<>();
        for (int index = 0; index < declaredMethods.length; index++) {
            Method candidate = declaredMethods[index];
            if (matcher.test(candidate))
                matches.add(candidate);
        }
        return CollectionsKt.toSet(matches);
    }

    public static MethodMatcherGroup createMethodMatcher(Function1 initializer) {
        MethodMatcherGroup matcher = new MethodMatcherGroup();
        initializer.invoke(matcher);
        return matcher;
    }

    private static boolean endsWithTest(String name) {
        return name.endsWith("Test");
    }

    private static Unit configureOpcodeFilter(ReflectionMethodRegistry filter) {
        filter.addParameterCounts(new int[] { 153, 154, 198, 199 });
        return Unit.INSTANCE;
    }

    private static Unit configureMethodMatcher(MethodMatcherGroup matcher) {
        matcher.addMethodNamePredicate(new Function1() {
            @Override public Object invoke(Object value) { return Boolean.valueOf(startsWithGet((String) value)); }
        });
        matcher.addModifierConstraint(java.lang.reflect.Modifier.PRIVATE);
        matcher.addPredicate(new Function1() {
            @Override public Object invoke(Object value) { return configureStringField((ClassPredicateSet) value); }
        });
        matcher.addCallbackPredicate(new Function1() {
            @Override public Object invoke(Object value) { return configureOpcodeFilter((ReflectionMethodRegistry) value); }
        });
        matcher.addMethodNamePredicates(new Function1[] {
            new Function1() { @Override public Object invoke(Object value) { return configureFieldType((MethodMatcherGroup) value); } },
            new Function1() { @Override public Object invoke(Object value) { return configureSimpleNameCandidate((MethodMatcherGroup) value); } }
        });
        return Unit.INSTANCE;
    }

    private static Unit configureClassMatcher(ClassPredicateSet matcher) {
        matcher.addPredicate(new Function1() {
            @Override public Object invoke(Object value) { return Boolean.valueOf(endsWithTest((String) value)); }
        });
        matcher.addIndex(java.lang.reflect.Modifier.PUBLIC);
        return Unit.INSTANCE;
    }

    private static Unit configureSimpleNameCandidate(MethodMatcherGroup matcher) {
        matcher.addParameterCountConstraint(2);
        return Unit.INSTANCE;
    }

    private static String getResourcePath(Class<?> type) {
        return type.getName().replace('.', '/') + ".class";
    }

    public static Class<?> verifyClass(Class<?> type, Function1 initializer) {
        ClassPredicateSet modifier = new ClassPredicateSet();
        initializer.invoke(modifier);
        if (!modifier.test(type)) {
            throw new IllegalArgumentException(formatMismatch(type));
        }
        return type;
    }

    public static FieldPredicateSet createFieldMatcher(Function1 initializer) {
        FieldPredicateSet matcher = new FieldPredicateSet();
        initializer.invoke(matcher);
        return matcher;
    }

    public static ClassPredicateSet createModifierMatcher(Function1 initializer) {
        ClassPredicateSet matcher = new ClassPredicateSet();
        initializer.invoke(matcher);
        return matcher;
    }

    private static Unit configureStringField(ClassPredicateSet predicate) {
        predicate.addClassPredicate(new Class<?>[] { String.class });
        return Unit.INSTANCE;
    }

    private static String formatMismatch(Class<?> type) {
        return "Class " + type + " does not match the given matcher";
    }

    private static boolean startsWithGet(String name) {
        return name.startsWith("get");
    }

    public static void inspectMethod(Method method, MethodVisitor visitor) {
        Class<?> declaringClass = method.getDeclaringClass();
        String resource = getResourcePath(declaringClass);
        ClassLoader loader = declaringClass.getClassLoader();
        InputStream stream = loader.getResourceAsStream(resource);
        ClassReader reader = new ClassReader(stream);
        ClassVisitor filteringVisitor = new MatrixExactMethodVisitor(method, visitor);
        reader.accept(filteringVisitor, 0);
    }

    private static Unit configureFieldType(MethodMatcherGroup matcher) {
        matcher.addParameterCountConstraint(4);
        return Unit.INSTANCE;
    }

    public static void setProbeMode(int value) { diagnosticMode = value; }
    public static int getProbeMode() { return diagnosticMode; }

    public static int diagnosticStatus() {
        int mode = getProbeMode();
        return mode == 0 ? 46 : 0;
    }
}
