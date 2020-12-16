package net.shadew.gradle.decompile.task;

import java.io.File;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;
import net.shadew.gradle.decompile.util.VersionInfo;

public class LoadVersionInfo extends Task {
    private String versionInfo = Constants.VERSION_INFO;
    private String versionDir = Constants.VERSION_DIR;

    public LoadVersionInfo(String name) {
        super(name);
    }

    public LoadVersionInfo versionDir(String versionDir) {
        this.versionDir = versionDir;
        return this;
    }

    public LoadVersionInfo versionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
        return this;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        if (ctx.env().hasProperty(versionInfo)) {
            ctx.upToDate();
            return;
        }

        File path = new File(ctx.file(versionDir + "version.json"));
        VersionInfo info = VersionInfo.fromFile(path);
        ctx.env().setProperty(versionInfo, info);
        ctx.env().setProperty(Constants.ASSETS_VERSION, info.getAssets());
        ctx.done();
    }
}
