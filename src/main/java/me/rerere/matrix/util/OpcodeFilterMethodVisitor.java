package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.org.objectweb.asm.MethodVisitor;
import me.rerere.matrix.thirdparty.kotlin.collections.ArraysKt;

/** Method visitor that filters selected zero-operand instructions. */
public final class OpcodeFilterMethodVisitor extends MethodVisitor {
    public final int[] filteredOpcodes;

    public OpcodeFilterMethodVisitor(int[] filteredOpcodes) {
        super(589824);
        this.filteredOpcodes = filteredOpcodes;
    }

    @Override
    public void visitInsn(int opcode) {
        try {
            if (ArraysKt.contains(filteredOpcodes, opcode)) {
                return;
            }
        } catch (me.rerere.matrix.license.LicenseResponseException failure) {
            throw failure;
        }
    }

}
