package net.shadew.gradle.decompile.task;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;

public class RemapMinecraft extends Remap {
    private final Type type;

    private String versionDir = Constants.VERSION_DIR;

    public RemapMinecraft(String name, Type type) {
        super(name);
        this.type = type;
    }

    public RemapMinecraft versionDir(String versionDir) {
        this.versionDir = versionDir;
        return this;
    }

    @Override
    protected String mappings(TaskRunContext ctx) {
        return ctx.file(versionDir + "mappings.rmap");
    }

    @Override
    protected String inJar(TaskRunContext ctx) {
        return ctx.file(versionDir + type.name().toLowerCase() + ".jar");
    }

    @Override
    protected String outJar(TaskRunContext ctx) {
        return ctx.file(versionDir + type.name().toLowerCase() + "_mapped.jar");
    }

    public enum Type {
        CLIENT,
        SERVER
    }
}
