package net.shadew.gradle.decompile.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

import net.shadew.gradle.decompile.TaskFailException;
import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.AssetsIndex;
import net.shadew.gradle.decompile.util.Constants;
import net.shadew.util.misc.IO;

public class DownloadAssets extends Task {
    private String assetsIndex = Constants.ASSETS_INDEX;
    private String objectsDir = Constants.OBJECTS_DIR;

    public DownloadAssets(String name) {
        super(name);
    }

    public DownloadAssets index(String assetsIndex) {
        this.assetsIndex = assetsIndex;
        return this;
    }

    public DownloadAssets objectsDir(String assetsDir) {
        this.objectsDir = assetsDir;
        return this;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        AssetsIndex index = ctx.env().getProperty(assetsIndex);
        if (index == null)
            throw new TaskFailException("Assets index was not properly loaded", this);

        Collection<AssetsIndex.Asset> assets = index.getAssets();

        int len = assets.size();
        int i = 0;
        ctx.progress(0);

        String status = "UP-TO-DATE";
        for (AssetsIndex.Asset asset : assets) {
            if (!downloadAsset(ctx, asset)) {
                status = "DONE";
            }

            i++;
            ctx.progress((double) i / len);
        }

        ctx.progress(-1);
        ctx.status(status);
    }

    private boolean downloadAsset(TaskRunContext ctx, AssetsIndex.Asset asset) throws Exception {
        String name = asset.getName();
        String shortName = name.contains("/")
                           ? name.substring(name.lastIndexOf('/') + 1)
                           : name;
        ctx.info(name);

        File out = new File(ctx.file(objectsDir + asset.getHashPath()));
        ctx.debug("Downloading to " + out);

        out.getParentFile().mkdirs();
        if (out.exists()) {
            ctx.debug("Asset up-to-date");
            return true;
        }

        URL url = new URL(asset.getUrl());
        ctx.debug("Downloading from " + url);

        ctx.status(shortName);

        URLConnection connection = url.openConnection();
        try (InputStream from = connection.getInputStream(); FileOutputStream to = new FileOutputStream(out)) {
            IO.copy(from, to);
        }
        return false;
    }
}
