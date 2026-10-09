package me.rerere.matrix.util.bytecode;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$BooleanRef;
import me.rerere.matrix.thirdparty.kotlin.collections.ArraysKt;
import me.rerere.matrix.thirdparty.org.objectweb.asm.Label;
import me.rerere.matrix.thirdparty.org.objectweb.asm.MethodVisitor;

/** Marks a shared BooleanRef when a visited jump opcode is in the configured set. */
public final class JumpOpcodeFilter extends MethodVisitor {
    public final int[] opcodes;
    public final Ref$BooleanRef matched;

    public JumpOpcodeFilter(int[] opcodes, Ref$BooleanRef matched) {
        super(589824);
        this.opcodes = opcodes;
        this.matched = matched;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (ArraysKt.contains(opcodes, opcode)) matched.element = true;
    }
}
