package net.shadew.gradle.decompile.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.util.misc.IO;

public class Unzip extends Task {
    private final String inputFile;
    private final String outputDir;

    public Unzip(String name, String inputFile, String outputDir) {
        super(name);
        this.inputFile = inputFile;
        this.outputDir = outputDir;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File in = new File(ctx.file(inputFile));
        File out = new File(ctx.file(outputDir));

        try (ZipFile zip = new ZipFile(in)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            int size = (int) zip.stream().count();
            int n = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (!entry.isDirectory()) {
                    File entryOut = new File(out, entry.getName());
                    entryOut.getParentFile().mkdirs();

                    try (InputStream inStream = zip.getInputStream(entry);
                         FileOutputStream outStream = new FileOutputStream(entryOut)) {
                        IO.copy(inStream, outStream);
                    }
                }

                ctx.progress(n++, size);
            }
        }

        ctx.done();
    }
}
