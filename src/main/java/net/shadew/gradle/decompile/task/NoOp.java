package net.shadew.gradle.decompile.task;

import net.shadew.gradle.decompile.TaskRunContext;

/**
 * A task that does nothing
 */
public class NoOp extends Task {
    public NoOp(String name) {
        super(name);
    }

    @Override
    public void execute(TaskRunContext ctx) {
        ctx.done();
    }
}
