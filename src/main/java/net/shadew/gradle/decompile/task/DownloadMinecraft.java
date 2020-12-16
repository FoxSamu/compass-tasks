package net.shadew.gradle.decompile.task;

import java.io.File;

import net.shadew.gradle.decompile.TaskFailException;
import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;
import net.shadew.gradle.decompile.util.VersionInfo;
import net.shadew.util.contract.Validate;

public class DownloadMinecraft extends Download {
    private final Type type;
    private String versionInfo = Constants.VERSION_INFO;
    private String outputDir = Constants.VERSION_DIR;

    public DownloadMinecraft(String name, Type type) {
        super(name);
        this.type = type;
    }

    public DownloadMinecraft versionInfo(String info) {
        this.versionInfo = info;
        return this;
    }

    public DownloadMinecraft outputDir(String outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    @Override
    protected String getDownloadUrl(TaskRunContext ctx) throws Exception {
        VersionInfo info = ctx.env().getProperty(versionInfo);
        if (info == null)
            throw new TaskFailException("Version info was not properly loaded", this);

        VersionInfo.Download download;
        switch (type) {
            default:
                return Validate.illegalState();
            case CLIENT:
                download = info.getClientJar();
                break;
            case SERVER:
                download = info.getServerJar();
                break;
            case CLIENT_MAPPINGS:
                download = info.getClientMappings();
                break;
            case SERVER_MAPPINGS:
                download = info.getServerMappings();
                break;
        }
        return download.getUrl();
    }

    @Override
    protected String getOutputFile(TaskRunContext ctx) {
        return ctx.file(outputDir + type.filename());
    }

    @Override
    protected boolean shouldDownload(TaskRunContext ctx, File file) {
        return !file.exists();
    }

    public enum Type {
        CLIENT("jar"),
        CLIENT_MAPPINGS("txt"),
        SERVER("jar"),
        SERVER_MAPPINGS("txt");

        private final String type;

        Type(String type) {
            this.type = type;
        }

        String filename() {
            return name().toLowerCase() + "." + type;
        }
    }
}
