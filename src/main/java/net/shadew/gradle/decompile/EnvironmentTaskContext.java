package net.shadew.gradle.decompile;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.shadew.gradle.decompile.task.Task;

public class EnvironmentTaskContext implements TaskRunContext {
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARNING = 2;
    public static final int ERROR = 3;
    public static final int QUIET = 4;

    private final Environment env;
    private final Task task;

    private double progress = -1;
    private String status;
    private boolean skipLast;

    private int loggingLevel = WARNING;

    public EnvironmentTaskContext(Environment env, Task task) {
        this.env = env;
        this.task = task;
    }

    public void printProgress() {
        System.out.print("\r");
        System.out.print("> ");
        System.out.print(task.getName());
        if (progress >= 0) {
            System.out.print(" [");
            for (int i = 0; i < 50; i++) {
                if (i < progress * 50) {
                    System.out.print("#");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.print("] ");
            System.out.print((int) (progress * 100) + "%");
        }
        if(status != null) {
            System.out.print(" (");
            System.out.print(status);
            System.out.print(")");
        }
    }

    public void setLoggingLevel(int loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public int getLoggingLevel() {
        return loggingLevel;
    }

    private void println(String msg) {
        System.out.print("\r");
        System.out.println(msg);
    }

    @Override
    public void progress(double progress) {
        this.progress = progress;
        printProgress();
    }

    @Override
    public void status(String status) {
        this.status = status;
        printProgress();
    }

    @Override
    public void skipLast() {
        skipLast = true;
    }

    @Override
    public boolean doesSkipLast() {
        return skipLast;
    }

    @Override
    public void debug(String message) {
        if (loggingLevel <= DEBUG) {
            println("DEBUG: " + message);
            printProgress();
        }
    }

    @Override
    public void info(String message) {
        if (loggingLevel <= INFO) {
            println("INFO : " + message);
            printProgress();
        }
    }

    @Override
    public void warning(String message) {
        if (loggingLevel <= WARNING) {
            println("WARN : " + message);
            printProgress();
        }
    }

    @Override
    public void error(String message) {
        if (loggingLevel <= ERROR) {
            println("ERROR: " + message);
            printProgress();
        }
    }

    @Override
    public void debug(String message, Throwable stacktrace) {
        if (loggingLevel <= DEBUG) {
            println("DEBUG: " + message);
            stacktrace.printStackTrace(System.out);
            printProgress();
        }
    }

    @Override
    public void info(String message, Throwable stacktrace) {
        if (loggingLevel <= INFO) {
            println("INFO : " + message);
            stacktrace.printStackTrace(System.out);
            printProgress();
        }
    }

    @Override
    public void warning(String message, Throwable stacktrace) {
        if (loggingLevel <= WARNING) {
            println("WARN : " + message);
            stacktrace.printStackTrace(System.out);
            printProgress();
        }
    }

    @Override
    public void error(String message, Throwable stacktrace) {
        if (loggingLevel <= ERROR) {
            println("ERROR: " + message);
            stacktrace.printStackTrace(System.out);
            printProgress();
        }
    }

    @Override
    public Environment env() {
        return env;
    }

    @Override
    public String file(String path) {
        path = path.replace('\\', '/');

        Pattern property = Pattern.compile("\\[\\[(.*?)]]");
        Matcher matcher = property.matcher(path);
        StringBuffer buf = new StringBuffer();
        while(matcher.find()) {
            matcher.appendReplacement(buf, env.getProperty(matcher.group(1)));
        }
        matcher.appendTail(buf);
        path = buf.toString();

        if (path.equals("{taskDir}")) {
            return new File(env.getWorkingDir(), task.getName()).getAbsolutePath() + "";
        }

        if (path.startsWith("{taskDir}/")) {
            return new File(env.getWorkingDir(), task.getName()).getAbsolutePath()
                       + "/" + path.substring("{taskDir}/".length());
        }

        specificTaskPath:
        if (path.startsWith("{taskDir:")) {
            int close = path.indexOf('}');
            if (close < 0) break specificTaskPath;

            String task = path.substring("{taskDir:".length(), close);
            if (close + 1 == path.length()) {
                return new File(env.getWorkingDir(), task).getAbsolutePath() + "";
            }

            if (path.charAt(close + 1) != '/') break specificTaskPath;

            return new File(env.getWorkingDir(), task).getAbsolutePath() + "/" + path.substring(close + 1);
        }
        return env.file(path);
    }
}
