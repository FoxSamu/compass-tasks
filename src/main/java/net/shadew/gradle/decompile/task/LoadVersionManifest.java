package net.shadew.gradle.decompile.task;

import java.io.File;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;
import net.shadew.gradle.decompile.util.VersionManifest;

public class LoadVersionManifest extends Task {
    private final String file;
    private String versionManifest = Constants.VERSION_MANIFEST;

    public LoadVersionManifest(String name, String file) {
        super(name);
        this.file = file;
    }

    public LoadVersionManifest versionManifest(String versionManifest) {
        this.versionManifest = versionManifest;
        return this;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        if(ctx.env().hasProperty(versionManifest)) {
            ctx.upToDate();
            return;
        }

        File path = new File(ctx.file(file));
        VersionManifest manifest = VersionManifest.fromFile(path);
        ctx.env().setProperty(versionManifest, manifest);
        ctx.done();
    }
}
