package net.shadew.gradle.decompile.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;
import net.shadew.util.misc.IO;

public class ExtractAssets extends Task {
    private String inputDir = Constants.VERSION_DIR;
    private String outputDir = Constants.VERSION_DIR;

    public ExtractAssets(String name) {
        super(name);
    }

    public ExtractAssets inputDir(String inputDir) {
        this.inputDir = inputDir;
        return this;
    }

    public ExtractAssets outputDir(String outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File clientFile = new File(ctx.file(inputDir + "client_mapped.jar"));
        File serverFile = new File(ctx.file(inputDir + "server_mapped.jar"));
        File outFile = new File(ctx.file(outputDir + "assets.jar"));

        if (outFile.exists()) {
            ctx.upToDate();
            return;
        }

        // Ensure output directory exists
        outFile.getParentFile().mkdirs();

        try (ZipFile client = new ZipFile(clientFile);
             ZipFile server = new ZipFile(serverFile);
             JarOutputStream out = new JarOutputStream(new FileOutputStream(outFile))) {

            // All resource names to copy
            List<String> resources = Stream.of(client, server)
                                           .flatMap(zip -> zip.stream().map(ZipEntry::getName))
                                           .distinct()
                                           .filter(name -> !name.endsWith(".class"))
                                           .collect(Collectors.toList());

            int size = resources.size();
            int n = 0;

            for (String res : resources) {
                ZipEntry entry1 = client.getEntry(res);
                ZipEntry entry2 = server.getEntry(res);

                // Get entry to copy
                ZipEntry inEntry = entry1;
                if (inEntry == null) inEntry = entry2;
                if (inEntry == null) continue;

                ZipFile inFile = inEntry == entry1 ? client : server;

                // Copy entry
                out.putNextEntry(new ZipEntry(inEntry.getName()));
                try (InputStream in = inFile.getInputStream(inEntry)) {
                    IO.copy(in, out);
                }
                out.closeEntry();

                ctx.progress(n++, size);
            }
        }

        ctx.done();
    }
}
