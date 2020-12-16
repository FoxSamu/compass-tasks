package net.shadew.gradle.decompile.task;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;

import net.shadew.gradle.decompile.TaskRunContext;

public abstract class FilterJar<T extends FilterJar<T>> extends JarModify<T> {
    public FilterJar(String name) {
        super(name);
    }

    protected abstract boolean canKeep(String filename);

    @Override
    protected Action getAction(TaskRunContext ctx, JarEntry entry) {
        return canKeep(entry.getName()) ? Action.COPY : Action.IGNORE;
    }

    @Override
    protected String rename(TaskRunContext ctx, JarEntry entry) {
        return null;
    }

    @Override
    protected String modify(TaskRunContext ctx, JarEntry entry, InputStream in, OutputStream out) {
        return null;
    }
}
