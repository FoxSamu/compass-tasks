package net.shadew.gradle.decompile.task;

import com.google.gson.JsonArray;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.shadew.gradle.decompile.TaskFailException;
import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;
import net.shadew.gradle.decompile.util.OS;
import net.shadew.gradle.decompile.util.VersionInfo;

/**
 * Collects library information from a {@link VersionInfo} and produces a JSON file with all artifact coordinates. This
 * is different per OS.
 */
public class CollectLibs extends Task {
    private String versionInfo = Constants.VERSION_INFO;
    private String outputFile = Constants.VERSION_DIR + "libraries_[[OSName]].json";
    private boolean collectSources = true;

    public CollectLibs(String name) {
        super(name);
    }

    public CollectLibs versionInfo(String info) {
        this.versionInfo = info;
        return this;
    }

    public CollectLibs outputFile(String outputFile) {
        this.outputFile = outputFile;
        return this;
    }

    public CollectLibs collectSources(boolean collectSources) {
        this.collectSources = collectSources;
        return this;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        OS currentOS = ctx.env().getProperty("OS");

        VersionInfo info = ctx.env().getProperty(versionInfo);
        if (info == null)
            throw new TaskFailException("Version info was not properly loaded", this);


        List<VersionInfo.Library> libraries = info.getLibraries(currentOS);

        JsonArray result = new JsonArray();
        Set<String> collected = new HashSet<>();

        for (VersionInfo.Library lib : libraries) {
            // Main library
            String libName = lib.getName().toString();
            if (!collected.contains(libName)) {
                result.add(libName);
                collected.add(libName);
            }

            // Natives
            VersionInfo.Natives natives = lib.getNatives();
            if (currentOS != null) {
                // Current OS only
                String classifier = natives.getNativeClassifier(currentOS);
                if (classifier != null) {
                    String name = lib.getName().toString() + ":" + classifier;
                    if (!collected.contains(name)) {
                        result.add(name);
                        collected.add(name);
                    }
                }
            } else for (OS os : OS.values()) {
                // No current OS found, just collect for all OSes
                String classifier = natives.getNativeClassifier(os);
                if (classifier != null) {
                    String name = lib.getName().toString() + ":" + classifier;
                    if (!collected.contains(name)) {
                        result.add(name);
                        collected.add(name);
                    }
                }
            }

            // Sources
            if (collectSources && lib.getSourcesDownload() != null) {
                String name = lib.getName().toString() + ":sources";
                if (!collected.contains(name)) {
                    result.add(name);
                    collected.add(name);
                }
            }
        }

        // Create JSON file
        File file = new File(ctx.file(outputFile));
        try (FileWriter out = new FileWriter(file)) {
            Streams.write(result, new JsonWriter(out));
        }

        ctx.status("DONE");
    }
}
