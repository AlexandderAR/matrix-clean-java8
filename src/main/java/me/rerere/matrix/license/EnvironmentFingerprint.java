package me.rerere.matrix.license;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Locale;
import java.util.Optional;
import me.rerere.matrix.compat.SystemPropertyAccess;
import me.rerere.matrix.crypto.DigestFormatter;

/** Builds the machine and account fingerprint used by the licensing path. */
public final class EnvironmentFingerprint {
    private static final long TRIM_BOUNDARY = 9_119_851_337_767_452_704L;
    public static final EnvironmentFingerprint INSTANCE = new EnvironmentFingerprint();

    private EnvironmentFingerprint() {
    }

    private static String formatComponents(String processor, String computer, String user,
                                           String operatingSystem, int processors,
                                           String architecture, String language) {
        return new StringBuilder(processor).append('/').append(computer).append('/')
                .append(user).append('/').append(operatingSystem).append('/')
                .append(processors).append('/').append(architecture).append('/')
                .append(language).toString();
    }

    public static final String fingerprint() {
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();

        String processorIdentifier = System.getenv("PROCESSOR_IDENTIFIER");
        String computerName = System.getenv("COMPUTERNAME");
        String userName = SystemPropertyAccess.getProperty("user.name");
        String normalizedUserName = trimUserName(userName);
        String operatingSystemName = SystemPropertyAccess.getProperty("os.name");
        int processorCount = operatingSystem.getAvailableProcessors();
        String architecture = operatingSystem.getArch();
        String languageCode = Locale.getDefault().getLanguage();

        String components = formatComponents(processorIdentifier, computerName, normalizedUserName,
                operatingSystemName, processorCount, architecture, languageCode);
        return DigestFormatter.hashText(components);
    }

    private static String trimUserName(String user) {
        int start = 0;
        int end = user.length() - 1;
        boolean trimmingTrailingCharacters = false;
        while (start <= end) {
            int index = trimmingTrailingCharacters ? end : start;
            boolean atOrBelowBoundary = user.charAt(index) <= (int) TRIM_BOUNDARY;
            if (trimmingTrailingCharacters) {
                if (atOrBelowBoundary) {
                    end--;
                } else {
                    break;
                }
            } else {
                if (!atOrBelowBoundary) {
                    trimmingTrailingCharacters = true;
                    continue;
                }
                start++;
            }
        }
        return user.subSequence(start, end + 1).toString();
    }

    public static final Optional<String> environmentVariable(String name) {
        return Optional.ofNullable(System.getenv(name));
    }

}
