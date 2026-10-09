package me.rerere.matrix.compat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.rerere.matrix.compat.SystemPropertyAccess;
import me.rerere.matrix.config.ProgramInfo;

/** Windows-only notice that detects Notepad++ and offers replacement-editor links. */
public final class NotepadPlusPlusNotice {
    private static final long POST_NOTICE_DELAY_MILLIS = 3000L;

    private NotepadPlusPlusNotice() {}

    public static boolean isWindows() {
        String osName = SystemPropertyAccess.getProperty("os.name");
        return osName.toLowerCase(Locale.ROOT).contains("windows");
    }

    public static String uninstallCommand(String command) {
        return "cmd /c " + command;
    }

    public static String uninstallFailureMessage(String failureText) {
        return "(!) 卸载Notepad++失败: " + failureText;
    }

    /** Searches the Windows uninstall registry and extracts display and uninstall strings. */
    public static Object findInstalledPrograms() {
        try {
            Process registryQuery = Runtime.getRuntime().exec(
                    "reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\" /S");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(registryQuery.getInputStream(), StandardCharsets.UTF_8), 8192);
            Throwable readerFailure = null;
            try {
                List<String> registryLines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) registryLines.add(line);
                StringBuilder registryOutput = new StringBuilder();
                for (int i = 0; i < registryLines.size(); i++) {
                    if (i != 0) registryOutput.append('\n');
                    registryOutput.append(registryLines.get(i));
                }
                List<ProgramInfo> installedPrograms = new ArrayList<>();
                Pattern displayPattern = Pattern.compile("DisplayName\\s+REG_SZ\\s+(.*)");
                Pattern uninstallPattern = Pattern.compile("UninstallString\\s+REG_SZ\\s+(.*)");
                for (String registryEntry : me.rerere.matrix.thirdparty.kotlin.text.StringsKt__StringsKt.split(
                        registryOutput.toString(), new String[] {"\n\n"}, false, 0)) {
                    String displayName = groupOne(displayPattern.matcher(registryEntry));
                    String uninstallString = groupOne(uninstallPattern.matcher(registryEntry));
                    if (displayName != null && uninstallString != null) {
                        installedPrograms.add(new ProgramInfo(displayName, uninstallString));
                    }
                }
                return installedPrograms;
            } catch (Throwable failure) {
                readerFailure = failure;
                throw failure;
            } finally {
                me.rerere.matrix.thirdparty.kotlin.io.CloseableKt.closeFinally(reader, readerFailure);
            }
        } catch (Throwable failure) {
            return me.rerere.matrix.thirdparty.kotlin.ResultKt.createFailure(failure);
        }
    }

    private static String groupOne(Matcher matcher) {
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Recovers the first declared method matching name, return class and parameter classes. */
    private static Method findDeclaredMethod(Class<?> owner, String name, Class<?> returnType,
                                             int parameterCount, Class<?>[] parameterTypes) {
        for (Method method : owner.getDeclaredMethods()) {
            if (!method.getName().equals(name) || method.getReturnType() != returnType) continue;
            Class<?>[] actual = method.getParameterTypes();
            if (actual.length != parameterCount) continue;
            boolean sameParameters = true;
            for (int i = 0; i < parameterCount; i++) {
                if (actual[i] != parameterTypes[i]) {
                    sameParameters = false;
                    break;
                }
            }
            if (sameParameters) return method;
        }
        return null;
    }

    /** Searches interfaces recursively; it deliberately does not walk superclasses. */
    private static Method findInterfaceMethod(Class<?> owner, String name, Class<?> returnType,
                                              int parameterCount, Class<?>[] parameterTypes) {
        Method direct = findDeclaredMethod(owner, name, returnType, parameterCount, parameterTypes);
        if (direct != null) return direct;
        Class<?>[] interfaces = owner.getInterfaces();
        if (interfaces != null) {
            for (Class<?> implemented : interfaces) {
                Method inherited = findInterfaceMethod(implemented, name, returnType,
                        parameterCount, parameterTypes);
                if (inherited != null) return inherited;
            }
        }
        return null;
    }

    /** Performs the original Notepad++ check and its user-visible process/console side effects. */
    public static void inspectAndNotify() {
        if (!isWindows()) return;

        Object scanResult = findInstalledPrograms();
        if (!(scanResult instanceof List<?>)) return;
        @SuppressWarnings("unchecked")
        List<ProgramInfo> matches = (List<ProgramInfo>) scanResult;

        List<ProgramInfo> notepadEntries = new ArrayList<>();
        for (ProgramInfo program : matches) {
            if (program.getDisplayName().contains("Notepad++")) {
                notepadEntries.add(program);
            }
        }

        for (ProgramInfo program : notepadEntries) {
            try {
                System.out.println("(!) 检测到已安装Notepad++, 推荐你使用以下编辑器代替:");
                System.out.println("(!) Notepad--: https://gitee.com/cxasm/notepad--");
                System.out.println("(!) VSCode: https://code.visualstudio.com/");
                System.out.println("(!) Sublime Text: https://www.sublimetext.com/");
                System.out.println("(!) Lapce: https://lapce.dev/");
                System.out.println("(!) 即将为你打开卸载程序, 请自行卸载");
                Thread.sleep(POST_NOTICE_DELAY_MILLIS);
                Runtime.getRuntime().exec("cmd /c start https://code.visualstudio.com/");
                String uninstall = program.getUninstallString();
                Runtime.getRuntime().exec(uninstallCommand(uninstall));
            } catch (Throwable launchFailure) {
                System.out.println(uninstallFailureMessage(launchFailure.getMessage()));
            }
        }
    }

    static long delayMillis() { return POST_NOTICE_DELAY_MILLIS; }
}
