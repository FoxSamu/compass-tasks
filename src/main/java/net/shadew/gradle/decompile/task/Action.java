package net.shadew.gradle.decompile.task;

import net.shadew.gradle.decompile.TaskRunContext;

@FunctionalInterface
public interface Action {
    void execute(TaskRunContext ctx) throws Exception;
}
