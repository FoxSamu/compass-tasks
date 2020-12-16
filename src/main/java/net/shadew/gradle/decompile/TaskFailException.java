package net.shadew.gradle.decompile;

import net.shadew.gradle.decompile.task.Task;

public class TaskFailException extends Exception {
    private final Task task;

    public TaskFailException(String message, Task task) {
        super(message);
        this.task = task;
    }

    public TaskFailException(String message, Throwable cause, Task task) {
        super(message, cause);
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
