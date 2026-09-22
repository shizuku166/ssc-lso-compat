package com.ssclso.compat;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 独立文件日志：写入游戏目录下的 ssc-lso-compat-logs/ssc-lso-compat.log，
 * 便于用户直接将该文件发送给开发者排查，无需翻 latest.log。
 */
public final class FileLogger {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static Path logFile;
    private static boolean initFailed = false;

    private FileLogger() {
    }

    private static Path ensureFile() {
        if (logFile == null && !initFailed) {
            try {
                Path dir = FMLPaths.GAMEDIR.get().resolve("ssc-lso-compat-logs");
                Files.createDirectories(dir);
                logFile = dir.resolve("ssc-lso-compat.log");
            } catch (Throwable t) {
                initFailed = true;
                return null;
            }
        }
        return logFile;
    }

    /** 追加一行日志；失败静默（不影响游戏）。 */
    public static void log(String msg) {
        Path f = ensureFile();
        if (f == null) {
            return;
        }
        try (BufferedWriter w = Files.newBufferedWriter(f, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write("[" + LocalDateTime.now().format(TS) + "] " + msg);
            w.newLine();
        } catch (IOException e) {
            initFailed = true;
        }
    }

    /** 追加一行日志 + 异常堆栈。 */
    public static void log(String msg, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        log(msg + System.lineSeparator() + sw);
    }
}
