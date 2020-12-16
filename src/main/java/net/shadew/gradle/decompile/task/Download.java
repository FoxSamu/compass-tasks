package net.shadew.gradle.decompile.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import net.shadew.gradle.decompile.TaskRunContext;

public abstract class Download extends Task {
    public Download(String name) {
        super(name);
    }

    protected abstract String getDownloadUrl(TaskRunContext ctx) throws Exception;
    protected abstract String getOutputFile(TaskRunContext ctx) throws Exception;

    protected boolean shouldDownload(TaskRunContext ctx, File file) {
        return true;
    }

    protected boolean lazyWhenOffline(File file) {
        return false;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File out = new File(getOutputFile(ctx));
        ctx.debug("Downloading to " + out);

        out.getParentFile().mkdirs();
        if (!shouldDownload(ctx, out)) {
            ctx.info("Download is up to date");
            ctx.upToDate();
            return;
        }

        URL url = new URL(getDownloadUrl(ctx));
        ctx.debug("Downloading from " + url);

        try {
            URLConnection connection = url.openConnection();
            long len = connection.getContentLengthLong();
            long read = 0;
            try (InputStream from = connection.getInputStream();
                 FileOutputStream to = new FileOutputStream(out)) {
                byte[] buf = new byte[1024];
                int r;
                long time = System.nanoTime();
                while ((r = from.read(buf)) != -1) {
                    to.write(buf, 0, r);
                    read += r;

                    // Update status (progress, kB/s)
                    long newTime = System.nanoTime();
                    long diff = newTime - time;
                    double secs = diff / 1000000000d;
                    double bps = r / secs;
                    int kbps = (int) (bps / 1024d);
                    time = newTime;
                    ctx.progress((double) read / len);
                    ctx.status(kbps + " kB/s");
                }
            }
        } catch (UnknownHostException exc) {
            // Failed to download because of internet problems, if lazy then just don't download
            if(lazyWhenOffline(out)) {
                ctx.warning("Failed to download: unknown host");
                ctx.warning("Cached file available, skipping download");
                ctx.skipped();
                return;
            } else {
                throw exc;
            }
        }

        ctx.done();
    }
}
