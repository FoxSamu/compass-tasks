package net.shadew.gradle.decompile.task;

import java.io.File;

import net.shadew.gradle.decompile.TaskFailException;
import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;
import net.shadew.gradle.decompile.util.VersionManifest;

public class DownloadVersionInfo extends Download {
    private String versionManifest = Constants.VERSION_MANIFEST;
    private String outputDir = Constants.VERSION_DIR;

    public DownloadVersionInfo(String name) {
        super(name);
    }

    public DownloadVersionInfo outputDir(String outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    public DownloadVersionInfo versionManifest(String versionManifest) {
        this.versionManifest = versionManifest;
        return this;
    }

    @Override
    protected String getDownloadUrl(TaskRunContext ctx) throws Exception {
        String version = ctx.mcVersion();

        VersionManifest mf = ctx.env().getProperty(versionManifest);
        if (mf == null)
            throw new TaskFailException("Version manifest was not properly loaded", this);

        VersionManifest.Version v = mf.getVersion(version);
        if (v == null)
            throw new TaskFailException("Could not resolve version " + version, this);
        return v.getUrl();
    }

    @Override
    protected String getOutputFile(TaskRunContext ctx) {
        return ctx.file(outputDir + "version.json");
    }

    @Override
    protected boolean shouldDownload(TaskRunContext ctx, File file) {
        return !file.exists();
    }
}
