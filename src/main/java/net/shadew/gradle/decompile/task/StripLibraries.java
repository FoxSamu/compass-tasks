package net.shadew.gradle.decompile.task;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;

public class StripLibraries extends FilterJar<StripLibraries> {
    private String versionDir = Constants.VERSION_DIR;

    public StripLibraries(String name) {
        super(name);
    }

    @Override
    protected String getInputFile(TaskRunContext ctx) {
        return ctx.file(versionDir + "_tmp.jar");
    }

    @Override
    protected String getOutputFile(TaskRunContext ctx) {
        return ctx.file(versionDir + "server.jar");
    }

    @Override
    protected boolean canKeep(String name) {
        return name.equals("/") || !name.contains("/")
                   || name.startsWith("net/minecraft/")
                   || name.startsWith("assets/")
                   || name.startsWith("data/");
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File file = new File(ctx.file(versionDir + "server.jar"));
        File tmp = new File(ctx.file(versionDir + "_tmp.jar"));

        tmp.getParentFile().mkdirs();
        Files.move(file.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);

        try {
            super.execute(ctx);
        } finally {
            tmp.delete();
        }
    }
}
