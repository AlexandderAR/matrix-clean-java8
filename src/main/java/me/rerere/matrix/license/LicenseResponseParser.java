package me.rerere.matrix.license;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

/** Parses the selected successful license response fields. */
public final class LicenseResponseParser {
    private LicenseResponseParser() { }

    public static ParsedLicenseResponse parseSelected200(String response, String expectedPing) {
        List<String> outerFields = StringsKt.split(response, new String[] {"|"}, false, 0);
        if (!Intrinsics.areEqual(outerFields.get(0), expectedPing)) {
            throw new UnsupportedLicenseResponsePath("ping-unequal route unresolved");
        }
        String licenseFieldsText = outerFields.get(1);
        List<String> commaFields = StringsKt.split(licenseFieldsText, new String[] {","}, false, 0);
        boolean responseFlag = Boolean.parseBoolean(outerFields.get(2));
        boolean singleTrialField = commaFields.size() == 1 && commaFields.contains("Trial");
        return new ParsedLicenseResponse(outerFields, commaFields, responseFlag, singleTrialField);
    }

    public static final class ParsedLicenseResponse {
        public final List<String> outerFields;
        public final List<String> commaFields;
        public final boolean parsedBoolean;
        public final boolean singleTrialField;

        private ParsedLicenseResponse(List<String> outerFields, List<String> commaFields,
                                      boolean responseFlag, boolean singleTrialField) {
            this.outerFields = outerFields;
            this.commaFields = commaFields;
            this.parsedBoolean = responseFlag;
            this.singleTrialField = singleTrialField;
        }
    }

    public static final class UnsupportedLicenseResponsePath extends Error {
        public UnsupportedLicenseResponsePath(String message) {
            super(message);
        }
    }
}
