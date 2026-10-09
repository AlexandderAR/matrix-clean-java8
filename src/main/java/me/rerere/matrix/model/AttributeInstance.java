package me.rerere.matrix.model;

import java.util.ArrayList;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.attribute.Attribute;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Attribute instance value object reconstructed from constructor, accessors and data methods. */
public final class AttributeInstance {
    private boolean touched;
    private double originalBaseValue;
    private final Attribute attribute;
    private double baseValue;
    private final ArrayList modifiers;

    public AttributeInstance(Attribute attribute, double baseValue, ArrayList modifiers) {
        this.attribute = attribute;
        this.baseValue = baseValue;
        this.modifiers = modifiers;
        this.originalBaseValue = baseValue;
        this.touched = true;
    }

    public Attribute attribute() { return attribute; }
    public double baseValue() { return baseValue; }
    public void setBaseValue(double value) { baseValue = value; }
    public ArrayList modifiers() { return modifiers; }
    public void markTouched() { touched = true; }

    /** Copies this instance, applying defaults selected by the Kotlin-style mask. */
    public static AttributeInstance copyFrom(AttributeInstance self, Attribute nextAttribute,
            double nextBaseValue, ArrayList nextModifiers, int defaultValueMask) {
        return copyWithMask(self, nextAttribute, nextBaseValue, nextModifiers, defaultValueMask);
    }

    private static AttributeInstance copyWithMask(AttributeInstance self, Attribute nextAttribute,
            double nextBaseValue, ArrayList nextModifiers, int defaultValueMask) {
        if ((defaultValueMask & 1) != 0) nextAttribute = self.attribute;
        if ((defaultValueMask & 2) != 0) nextBaseValue = self.baseValue;
        if ((defaultValueMask & 4) != 0) nextModifiers = self.modifiers;
        return self.newInstance(nextAttribute, nextBaseValue, nextModifiers);
    }

    public static String format(Attribute attribute, double baseValue, ArrayList modifiers) {
        return formatAttribute(attribute, baseValue, modifiers);
    }

    private static String formatAttribute(Attribute attribute, double baseValue, ArrayList modifiers) {
        return "AttributeInstance(attribute=" + attribute + ", baseValue=" + baseValue + ", modifiers=" + modifiers + ")";
    }

    public AttributeInstance newInstance(Attribute attribute, double baseValue, ArrayList modifiers) {
        return new AttributeInstance(attribute, baseValue, modifiers);
    }

    @Override public String toString() {
        return formatAttribute(attribute, baseValue, modifiers);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AttributeInstance)) return false;
        AttributeInstance that = (AttributeInstance) other;
        return Intrinsics.areEqual(attribute, that.attribute)
                && Double.compare(baseValue, that.baseValue) == 0
                && Intrinsics.areEqual(modifiers, that.modifiers);
    }

    @Override public int hashCode() {
        int result = attribute.hashCode();
        result = 31 * result + Double.hashCode(baseValue);
        result = 31 * result + modifiers.hashCode();
        return result;
    }
}


