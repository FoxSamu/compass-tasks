package net.shadew.gradle.decompile.task;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.shadew.gradle.decompile.TaskFailException;
import net.shadew.gradle.decompile.TaskRunContext;

/**
 * Base of all tasks. A task performs a basic action including some extra, customized actions before and afterwards.
 */
public abstract class Task implements Action {
    private final Set<Task> dependencies = new LinkedHashSet<>();
    private boolean addingDeps = false;

    private final String name;

    private final List<Action> doFirst = new ArrayList<>();
    private final List<Action> doLast = new ArrayList<>();

    public Task(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this task
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the task's dependencies
     */
    public final Set<Task> getDependencies() {
        return dependencies;
    }

    /**
     * Adds a dependency
     */
    public final void dependsOn(Task dep) {
        dependencies.add(dep);
    }

    /**
     * Adds a action to perform before this task executes
     */
    public final void doFirst(Action ctx) {
        doFirst.add(0, ctx);
    }

    /**
     * Adds an action to perform after this task executed
     */
    public final void doLast(Action ctx) {
        doLast.add(ctx);
    }

    /**
     * Returns whether this task depends on the given task
     */
    public final boolean isDependentOn(Task task) {
        return dependencies.contains(task);
    }

    /**
     * Collects all dependency tasks in the given collection, at the order they should execute. To keep the tasks in
     * order, use a {@link LinkedHashSet}.
     *
     * @throws TaskFailException When a circular dependency is found
     */
    public final void collectDependencies(Set<Task> set) throws TaskFailException {
        if (addingDeps)
            throw new TaskFailException("Task " + getName() + " is dependent on itself", this);

        addingDeps = true;
        for (Task task : dependencies) {
            if (set.contains(task))
                continue;
            task.collectDependencies(set);
            set.add(task);
        }
        addingDeps = false;
    }

    /**
     * Runs this task, including all first and last actions, but not it's dependencies
     */
    public final void run(TaskRunContext ctx) throws Exception {
        for (Action first : doFirst) {
            first.execute(ctx);
        }
        execute(ctx);
        for (Action action : doLast) {
            if(!ctx.doesSkipLast()) {
                action.execute(ctx);
            }
        }
    }
}
