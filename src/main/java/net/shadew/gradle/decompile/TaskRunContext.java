package net.shadew.gradle.decompile;

import net.shadew.gradle.decompile.util.Constants;
import net.shadew.gradle.decompile.util.OS;

public interface TaskRunContext {
    void progress(double progress);
    void status(String status);

    void skipLast();
    boolean doesSkipLast();

    void debug(String message);
    void info(String message);
    void warning(String message);
    void error(String message);

    void debug(String message, Throwable stacktrace);
    void info(String message, Throwable stacktrace);
    void warning(String message, Throwable stacktrace);
    void error(String message, Throwable stacktrace);

    Environment env();
    String file(String path);

    default String mcVersion() {
        return env().getProperty(Constants.MC_VERSION);
    }

    default OS os() {
        return env().getProperty(Constants.OS);
    }

    default String osName() {
        return env().getProperty(Constants.OS_NAME);
    }

    default String assetsVersion() {
        return env().getProperty(Constants.ASSETS_VERSION);
    }

    default void progress(int n, int size) {
        progress((double) n / size);
    }

    default void finish(String status) {
        progress(-1);
        status(status);
    }

    default void upToDate() {
        skipLast();
        finish("UP-TO-DATE");
    }

    default void done() {
        finish("DONE");
    }

    default void skipped() {
        skipLast();
        finish("SKIPPED");
    }
}
