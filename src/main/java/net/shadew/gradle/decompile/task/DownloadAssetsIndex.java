package net.shadew.gradle.decompile.task;

import java.io.File;

import net.shadew.gradle.decompile.TaskFailException;
import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;
import net.shadew.gradle.decompile.util.VersionInfo;

public class DownloadAssetsIndex extends Download {
    private String versionInfo = Constants.VERSION_INFO;

    public DownloadAssetsIndex(String name) {
        super(name);
    }

    public DownloadAssetsIndex versionInfo(String info) {
        this.versionInfo = info;
        return this;
    }

    @Override
    protected String getDownloadUrl(TaskRunContext ctx) throws Exception {
        VersionInfo info = ctx.env().getProperty(versionInfo);
        if (info == null)
            throw new TaskFailException("Version info was not properly loaded", this);
        return info.getAssetsIndex().getUrl();
    }

    @Override
    protected String getOutputFile(TaskRunContext ctx) throws Exception {
        String version = ctx.env().getProperty(Constants.ASSETS_VERSION);
        if (version == null)
            throw new TaskFailException("Version info was not properly loaded", this);
        return ctx.file(Constants.INDEXES_DIR + version + ".json");
    }

    @Override
    protected boolean lazyWhenOffline(File file) {
        return file.exists();
    }
}
