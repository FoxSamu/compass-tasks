package net.shadew.gradle.decompile.task;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import net.shadew.gradle.decompile.TaskRunContext;

public class Compile extends Task {
    private final String sourceDir;
    private final String outJar;

    public Compile(String name, String sourceDir, String outJar) {
        super(name);
        this.sourceDir = sourceDir;
        this.outJar = outJar;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, ctx.file(sourceDir), ctx.file(outJar));
    }
}
