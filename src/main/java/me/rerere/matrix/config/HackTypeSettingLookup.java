package me.rerere.matrix.config;

import me.rerere.matrix.api.HackType;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.switches.HackTypeSwitchMap;
import me.rerere.matrix.thirdparty.kotlin.NoWhenBranchMatchedException;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

/** Maps public hack categories and names to their internal setting keys. */
public final class HackTypeSettingLookup {
    public CheckCategory byHackType(HackType hackType) {
        int category = HackTypeSwitchMap.CASE_BY_HACK_TYPE[hackType.ordinal()];
        try {
            switch (category) {
                case 1: return CheckCategory.KILLAURA;
                case 2: return CheckCategory.CLICK;
                case 3: return CheckCategory.HITBOX;
                case 4: return CheckCategory.MOVE;
                case 5: return CheckCategory.BADPACKETS;
                case 6: return CheckCategory.DELAY;
                case 7: return CheckCategory.BLOCK;
                case 8: return CheckCategory.SCAFFOLD;
                case 9: return CheckCategory.VELOCITY;
                case 10: return CheckCategory.CHAT;
                case 11: return CheckCategory.INTERACT;
                case 12: return CheckCategory.PHASE;
                case 13: return CheckCategory.AUTOBOT;
                case 14: return CheckCategory.ELYTRA;
                case 15: return CheckCategory.VEHICLE;
                default: throw new NoWhenBranchMatchedException();
            }
        } catch (NoWhenBranchMatchedException failure) {
            throw failure;
        }
    }

    public CheckCategory bySettingName(String name) {
        EnumEntries<?> entries = CheckCategory.enumEntries();
        for (Object entry : (Iterable<?>) entries) {
            CheckCategory candidate = (CheckCategory) entry;
            if (StringsKt.equals(candidate.getDisplayName(), name, true)) {
                return candidate;
            }
        }
        return null;
    }

    private HackTypeSettingLookup() {
    }

    public HackTypeSettingLookup(me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker ignoredMarker) {
        this();
    }

}


