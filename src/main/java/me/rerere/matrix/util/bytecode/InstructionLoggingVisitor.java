package me.rerere.matrix.util.bytecode;

import me.rerere.matrix.thirdparty.org.objectweb.asm.Label;
import me.rerere.matrix.thirdparty.org.objectweb.asm.MethodVisitor;

/** Prints the operands of visited jump and method instructions. */
public final class InstructionLoggingVisitor extends MethodVisitor {
    private static final int ASM_API_VERSION = 589824;

    public InstructionLoggingVisitor() { super((int) ASM_API_VERSION); }

    @Override public void visitJumpInsn(int opcode, Label label) {
        System.out.println((Object) formatJumpInstruction(opcode, label));
    }

    @Override public void visitMethodInsn(int opcode, String owner, String name,
                                          String descriptor, boolean isInterface) {
        System.out.println((Object) formatMethodInstruction(opcode, owner, name, descriptor, isInterface));
    }

    private static String formatJumpInstruction(int opcode, Label label) {
        return new StringBuilder("visitJumpInsn: ").append(opcode).append(", ")
                .append(label).toString();
    }

    private static String formatMethodInstruction(int opcode, String owner, String name,
            String descriptor, boolean isInterface) {
        return new StringBuilder("visitMethodInsn: ").append(opcode).append(", ")
                .append(owner).append(", ").append(name).append(", ").append(descriptor)
                .append(", ").append(isInterface).toString();
    }
}
