package net.shadew.gradle.decompile.util;

import java.util.Locale;

public enum OS {
    WINDOWS,
    LINUX,
    OSX;

    private static OS detected;

    public static OS get() {
        if (detected == null) {
            String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if (OS.contains("mac") || OS.contains("darwin")) {
                detected = OSX;
            } else if (OS.contains("win")) {
                detected = WINDOWS;
            } else if (OS.contains("nux")) {
                detected = LINUX;
            } else {
                detected = null;
            }
        }
        return detected;
    }
}
