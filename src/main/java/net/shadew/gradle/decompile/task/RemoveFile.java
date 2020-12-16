package net.shadew.gradle.decompile.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import net.shadew.gradle.decompile.TaskRunContext;

public class RemoveFile extends Task {
    private final String remove;

    public RemoveFile(String name, String file) {
        super(name);
        this.remove = file;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File file = new File(ctx.file(remove));

        if (!file.exists()) {
            ctx.upToDate();
            return;
        }

        if (!file.isDirectory()) {
            Files.delete(file.toPath());
        } else {
            delete(file.toPath());
        }
    }

    private static void delete(Path path) throws IOException {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for(Path p : stream) {
                if(p.toFile().isDirectory()) {
                    delete(p);
                } else {
                    Files.delete(p);
                }
            }
        }
    }
}
