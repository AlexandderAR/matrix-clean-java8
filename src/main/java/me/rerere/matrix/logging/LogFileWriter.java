package me.rerere.matrix.logging;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import me.rerere.matrix.compat.KotlinTimeAccess;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.thirdparty.kotlin.collections.ArraysKt;
import me.rerere.matrix.thirdparty.kotlin.io.FilesKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.sequences.Sequence;
import me.rerere.matrix.thirdparty.kotlin.sequences.SequencesKt;
import me.rerere.matrix.thirdparty.kotlin.text.Charsets;
import me.rerere.matrix.thirdparty.kotlin.time.Duration;
import me.rerere.matrix.thirdparty.kotlin.time.DurationKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationUnit;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;
import org.bukkit.ChatColor;

public final class LogFileWriter {
    @NotNull
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    @NotNull
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public void appendLog(String message) {
        if (!MatrixConfiguration.logFile) return;
        try {
            File directory = new File(Matrix.Companion.matrixInstance().getDataFolder(), "logs");
            if (!directory.exists()) directory.mkdirs();
            File file = new File(directory, fileName(new Date()));
            if (!file.exists()) file.createNewFile();
            String timestampPrefix = "(" + timeFormat.format(new Date()) + ") ";
            String logLine = new StringBuilder(timestampPrefix)
                    .append(ChatColor.stripColor(message)).append("\n").toString();
            FilesKt.appendText(file, logLine, Charsets.UTF_8);
        } catch (IOException failure) {
            failure.printStackTrace();
            Matrix.Companion.logMessage("Can't log info to file | " + failure.getMessage());
        }
    }

    public void deleteOutdatedLogs(int retentionDays) {
        if (retentionDays < 0) return;
        if (retentionDays < 3) {
            Matrix.Companion.matrixInstance().getLogger().warning("You can't set `delete_time` lower than 3!");
            return;
        }
        Matrix.Companion.logMessage("Scanning outdated log files....");
        int deleted = 0;
        File directory = new File(Matrix.Companion.matrixInstance().getDataFolder(), "logs");
        long now = MillisecondSampler.currentTimeMillis();
        File[] files = directory.exists() && directory.isDirectory() ? directory.listFiles() : null;
        if (files != null) {
            Sequence<File> logFiles = ArraysKt.asSequence(files);
            logFiles = SequencesKt.filter(logFiles, file -> file.getName().startsWith("matrix_log_"));
            logFiles = SequencesKt.filter(logFiles, file -> file.getName().endsWith(".txt"));
            Iterator<File> iterator = logFiles.iterator();
            while (iterator.hasNext()) {
                File file = iterator.next();
                try {
                    String fileDate = file.getName().substring(11, 21);
                    Intrinsics.checkNotNullExpressionValue(fileDate, "substring(...)");
                    Date logDate = dateFormat.parse(fileDate);
                    Object durationCompanion = Duration.Companion;
                    long millisecondsPerDay = KotlinTimeAccess.wholeMilliseconds(DurationKt.toDuration(1, DurationUnit.DAYS));
                    int ageInDays = (int) ((now - logDate.getTime()) / millisecondsPerDay);
                    if (ageInDays >= retentionDays && file.delete()) deleted++;
                } catch (ParseException failure) {
                    failure.printStackTrace();
                }
            }
        }
        Matrix.Companion.logMessage("Deleted " + deleted + " outdated log files!");
    }

    private String fileName(Date date) { return "matrix_log_" + dateFormat.format(date) + ".txt"; }

}
