package net.shadew.gradle.decompile.task;

import java.io.*;

import net.shadew.asm.mappings.io.ProguardOutputMappingsIO;
import net.shadew.asm.mappings.io.RMapMappingsIO;
import net.shadew.asm.mappings.visit.FlipMappingsConverter;
import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;

public class JoinMappings extends Task {
    private String inputDir = Constants.VERSION_DIR;
    private String outputDir = Constants.VERSION_DIR;

    public JoinMappings(String name) {
        super(name);
    }

    public JoinMappings inputDir(String inputDir) {
        this.inputDir = inputDir;
        return this;
    }

    public JoinMappings outputDir(String outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File clientFile = new File(ctx.file(inputDir + "client_mappings.txt"));
        File serverFile = new File(ctx.file(inputDir + "server_mappings.txt"));
        File outFile = new File(ctx.file(outputDir + "mappings.rmap"));

        if(outFile.exists()) {
            ctx.upToDate();
            return;
        }

        try (Reader clientReader = new FileReader(clientFile); Reader serverReader = new FileReader(serverFile); Writer writer = new FileWriter(outFile)) {
            FlipMappingsConverter converter = new FlipMappingsConverter();
            ProguardOutputMappingsIO.read(clientReader).convert(converter);
            ProguardOutputMappingsIO.read(serverReader).convert(converter);
            RMapMappingsIO.write(writer, converter.getMappings());
        }

        ctx.done();
    }
}
