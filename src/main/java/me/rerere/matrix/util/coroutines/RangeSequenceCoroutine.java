package me.rerere.matrix.util.coroutines;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.coroutines.Continuation;
import me.rerere.matrix.thirdparty.kotlin.coroutines.intrinsics.IntrinsicsKt;
import me.rerere.matrix.thirdparty.kotlin.coroutines.jvm.internal.RestrictedSuspendLambda;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function2;
import me.rerere.matrix.thirdparty.kotlin.ranges.ClosedRange;
import me.rerere.matrix.thirdparty.kotlin.sequences.SequenceScope;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;

/** Coroutine that yields a closed double range, advancing by the configured step. */
public final class RangeSequenceCoroutine extends RestrictedSuspendLambda
        implements Function2<Object, Object, Object> {
    private Object coroutineState;
    public double current;
    public int label;
    public final double step;
    public final ClosedRange<Double> range;
    private static final String invalidResumeMessage = "call to 'resume' before 'invoke' with coroutine";

    public RangeSequenceCoroutine(ClosedRange<Double> range, double step, Continuation<?> completion) {
        super(2, (Continuation<Object>) completion);
        this.range = range;
        this.step = step;
    }

    private Object invokeInScope(SequenceScope<Object> scope, Continuation<?> completion) {
        RangeSequenceCoroutine coroutine = (RangeSequenceCoroutine) (Object) create(scope, completion);
        return coroutine.invokeSuspend(Unit.INSTANCE);
    }

    @Override
    public Object invoke(Object scope, Object completion) {
        return invokeInScope((SequenceScope<Object>) scope, (Continuation<?>) completion);
    }

    @Override
    public Continuation<Unit> create(Object receiver, Continuation<?> completion) {
        RangeSequenceCoroutine coroutine = new RangeSequenceCoroutine(range, step, completion);
        coroutine.coroutineState = receiver;
        return (Continuation<Unit>) (Continuation<?>) coroutine;
    }

    @Override
    public Object invokeSuspend(Object result) {
        Object suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        SequenceScope<Object> scope;
        double value;
        switch (label) {
            case 0:
                ResultKt.throwOnFailure(result);
                scope = (SequenceScope<Object>) coroutineState;
                value = ((Number) ((ClosedRange<?>) range).getStart()).doubleValue();
                break;
            case 1:
                value = current;
                scope = (SequenceScope<Object>) coroutineState;
                ResultKt.throwOnFailure(result);
                value += step;
                break;
            default:
                throw new IllegalStateException(invalidResumeMessage);
        }

        while (value <= ((Number) ((ClosedRange<?>) range).getEndInclusive()).doubleValue()) {
            coroutineState = scope;
            current = value;
            label = 1;
            Object yielded = scope.yield(Double.valueOf(value), this);
            if (yielded == suspended) return suspended;
            value += step;
        }
        return Unit.INSTANCE;
    }

    /** Decodes the byte format emitted by the original compact UTF-8 helper. */
    public static String decodeBytes(byte[] bytes) {
        int decodedLength = 0;
        int inputLength = bytes.length;
        char[] decoded = new char[inputLength];
        int inputIndex = 0;
        while (inputIndex < inputLength) {
            int firstByte = bytes[inputIndex] & 0xFF;
            if (firstByte < 192) {
                decoded[decodedLength++] = (char) firstByte;
            } else if (firstByte < 224) {
                char decodedCharacter = (char) ((firstByte & 31) << 6);
                int secondByte = bytes[++inputIndex];
                decodedCharacter |= (char) (secondByte & 63);
                decoded[decodedLength++] = decodedCharacter;
            } else if (inputIndex < inputLength - 2) {
                char decodedCharacter = (char) ((firstByte & 15) << 12);
                int secondByte = bytes[++inputIndex];
                decodedCharacter |= (char) ((secondByte & 63) << 6);
                int thirdByte = bytes[++inputIndex];
                decodedCharacter |= (char) (thirdByte & 63);
                decoded[decodedLength++] = decodedCharacter;
            }
            inputIndex++;
        }
        return new String(decoded, 0, decodedLength);
    }
}
