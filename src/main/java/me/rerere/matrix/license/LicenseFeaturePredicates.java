package me.rerere.matrix.license;

import java.util.Iterator;
import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

public final class LicenseFeaturePredicates {
    private LicenseFeaturePredicates() {
    }

    public static boolean hasPremiumOrEnterprise(List<?> features) {
        if (features.isEmpty()) {
            return false;
        }
        Iterator<?> iterator = features.iterator();
        while (iterator.hasNext()) {
            String feature = (String) iterator.next();
            if (StringsKt.contains(feature, "Premium", false)
                    || StringsKt.contains(feature, "Enterprise", false)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasEnterprise(List<?> features) {
        if (features.isEmpty()) {
            return false;
        }
        Iterator<?> iterator = features.iterator();
        while (iterator.hasNext()) {
            String feature = (String) iterator.next();
            if (StringsKt.contains(feature, "Enterprise", false)) {
                return true;
            }
        }
        return false;
    }
}
