package net.shadew.gradle.decompile;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.shadew.gradle.decompile.task.Task;
import net.shadew.gradle.decompile.util.OS;

public class Environment {
    private final Map<String, Object> properties = new HashMap<>();
    private int loggingLevel = EnvironmentTaskContext.WARNING;
    private File cacheDir = new File("./cache");
    private File workingDir = new File("./env");

    public Environment() {
        OS os = OS.get();
        setProperty("OS", os);

        String osName = os == null ? "generic" : os.name().toLowerCase();
        setProperty("OSName", osName);
    }

    public Environment(String mcVer) {
        this();
        setProperty("MCVersion", mcVer);
    }

    public String file(String path) {
        path = path.replace('\\', '/');

        Pattern property = Pattern.compile("\\[\\[(.*?)]]");
        Matcher matcher = property.matcher(path);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            Object prop = getProperty(matcher.group(1));
            if (prop != null)
                matcher.appendReplacement(buf, prop.toString());
            else
                matcher.appendReplacement(buf, matcher.group(1));
        }
        matcher.appendTail(buf);
        path = buf.toString();

        if (path.equals("{cacheDir}"))
            return cacheDir.getAbsolutePath() + "";

        if (path.equals("{workingDir}"))
            return workingDir.getAbsolutePath() + "";

        if (path.startsWith("{cacheDir}/"))
            return cacheDir.getAbsolutePath() + "/" + path.substring("{cacheDir}/".length());

        if (path.startsWith("{workingDir}/"))
            return workingDir.getAbsolutePath() + "/" + path.substring("{workingDir}/".length());

        return path;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    public void setLoggingLevel(int loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public int getLoggingLevel() {
        return loggingLevel;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key) {
        return (T) properties.get(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    private void runTask(Task task, TaskRunContext ctx) throws TaskFailException {
        try {
            task.run(ctx);
        } catch (TaskFailException exc) {
            ctx.progress(-1);
            ctx.status("FAILED");
            throw exc;
        } catch (Exception exc) {
            ctx.progress(-1);
            ctx.status("FAILED");
            throw new TaskFailException("Task " + task.getName() + " failed with an exception: " + exc.getMessage(), exc, task);
        }
    }

    private EnvironmentTaskContext createContext(Task task) {
        EnvironmentTaskContext ctx = new EnvironmentTaskContext(this, task);
        ctx.setLoggingLevel(loggingLevel);
        return ctx;
    }

    private void runTask(Task task) throws TaskFailException {
        EnvironmentTaskContext ctx = createContext(task);
        ctx.printProgress();
        runTask(task, ctx);
        System.out.println();
    }

    private int runTasks(Task goal) throws TaskFailException {
        LinkedHashSet<Task> graph = new LinkedHashSet<>();
        goal.collectDependencies(graph);
        graph.add(goal);

        for (Task task : graph) {
            runTask(task);
        }
        return graph.size();
    }

    public void run(Task goal) throws TaskFailException {
        try {
            System.out.println("Running tasks for " + goal.getName() + "...");
            int exec = runTasks(goal);
            System.out.println("Successfully executed " + exec + " tasks!");
        } catch (TaskFailException exc) {
            System.out.println("Task execution failed!");
            throw exc;
        }
    }
}
