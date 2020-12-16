package net.shadew.gradle.decompile.task;

import java.io.File;

import net.shadew.gradle.decompile.TaskRunContext;

public class SimpleDownload extends Download {
    private final String url;
    private final String to;
    private boolean lazy;
    private boolean doUpToDate = false;

    public SimpleDownload(String name, String url, String to) {
        super(name);
        this.url = url;
        this.to = to;
    }

    public SimpleDownload lazy(boolean lazy) {
        this.lazy = lazy;
        return this;
    }

    public SimpleDownload doUpToDate(boolean utd) {
        this.doUpToDate = utd;
        return this;
    }

    @Override
    protected String getDownloadUrl(TaskRunContext ctx) {
        return url;
    }

    @Override
    protected String getOutputFile(TaskRunContext ctx) {
        return ctx.file(to);
    }

    @Override
    protected boolean shouldDownload(TaskRunContext ctx, File file) {
        return !doUpToDate || !file.exists();
    }

    @Override
    protected boolean lazyWhenOffline(File file) {
        return file.exists() && lazy;
    }
}
