package net.shadew.gradle.decompile.task;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;

public class DecompileMinecraft extends Decompile {
    private String versionDir = Constants.VERSION_DIR;

    public DecompileMinecraft(String name) {
        super(name);
    }

    public DecompileMinecraft versionDir(String versionDir) {
        this.versionDir = versionDir;
        return this;
    }

    @Override
    protected String getInputFile(TaskRunContext ctx) throws Exception {
        return ctx.file(versionDir + "merged.jar");
    }

    @Override
    protected String getOutputDir(TaskRunContext ctx) throws Exception {
        return ctx.file(versionDir + "decompiled/");
    }
}
