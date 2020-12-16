package net.shadew.gradle.decompile.task;

import java.io.File;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.AssetsIndex;
import net.shadew.gradle.decompile.util.Constants;

public class LoadAssetsIndex extends Task {
    private String indexesDir = Constants.INDEXES_DIR;
    private String assetsIndex = Constants.ASSETS_INDEX;

    public LoadAssetsIndex(String name) {
        super(name);
    }

    public LoadAssetsIndex indexesDir(String indexesDir) {
        this.indexesDir = indexesDir;
        return this;
    }

    public LoadAssetsIndex assetsIndex(String assetsIndex) {
        this.assetsIndex = assetsIndex;
        return this;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        if (ctx.env().hasProperty(assetsIndex)) {
            ctx.upToDate();
            return;
        }

        File path = new File(ctx.file(indexesDir + ctx.assetsVersion() + ".json"));
        AssetsIndex index = AssetsIndex.fromFile(path);
        ctx.env().setProperty(assetsIndex, index);
        ctx.done();
    }
}
