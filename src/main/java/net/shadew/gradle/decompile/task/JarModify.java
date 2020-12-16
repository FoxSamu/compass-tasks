package net.shadew.gradle.decompile.task;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.util.misc.IO;

/**
 * A generic task that can filter, copy and modify files from one JAR file into another JAR file
 */
public abstract class JarModify<T extends JarModify<T>> extends Task {
    private boolean verify = true;

    public JarModify(String name) {
        super(name);
    }

    /**
     * Whether to verify the input jar on opening
     */
    @SuppressWarnings("unchecked")
    public T verify(boolean verify) {
        this.verify = verify;
        return (T) this;
    }

    /**
     * Gets and returns the input file path
     */
    protected abstract String getInputFile(TaskRunContext ctx) throws Exception;

    /**
     * Gets and returns the output file path
     */
    protected abstract String getOutputFile(TaskRunContext ctx) throws Exception;

    /**
     * Returns the action to perform for the given entry:
     * <ul>
     * <li>{@link Action#IGNORE} ignores the file: it will not appear in the output file</li>
     * <li>{@link Action#COPY} copies the file directly: it will appear in the output file exactly as it appears in the
     * input file, with the same name</li>
     * <li>{@link Action#COPY_RENAME} copies the file directly: it will appear in the output file exactly as it appears
     * in the input file, but under the new name returned by {@link #rename}</li>
     * <li>{@link Action#MODIFY} modifies the file via {@link #modify}, which has full control on writing the input
     * data to the output.</li>
     * <li>{@code null} ignores the file, just like {@link Action#IGNORE}</li>
     * </ul>
     */
    protected abstract Action getAction(TaskRunContext ctx, JarEntry entry) throws Exception;

    /**
     * Called to rename a JAR entry, returns the new name. This is called when {@link Action#COPY_RENAME} is returned
     * from {@link #getAction}.
     */
    protected abstract String rename(TaskRunContext ctx, JarEntry entry) throws Exception;

    /**
     * Called to manually copy and maybe modify the contents of a JAR entry, returns the new name. This is called when
     * {@link Action#MODIFY} is returned from {@link #getAction}.
     */
    protected abstract String modify(TaskRunContext ctx, JarEntry entry, InputStream in, OutputStream out) throws Exception;

    /**
     * Returns whether this task is up to date
     */
    protected boolean isUpToDate(TaskRunContext ctx, File file) {
        return file.exists();
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File inputFile = new File(getInputFile(ctx));
        File outputFile = new File(getOutputFile(ctx));

        if (isUpToDate(ctx, outputFile)) {
            ctx.upToDate();
            return;
        }

        // Ensure output directory exists
        outputFile.getParentFile().mkdirs();

        try (JarFile in = new JarFile(inputFile, verify);
             JarOutputStream out = new JarOutputStream(new FileOutputStream(outputFile));

             // Use a copy buffer so we can let the task decide what name to give to an entry after writing
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            int size = (int) in.stream().count();
            int n = 0;

            Enumeration<JarEntry> entries = in.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                Action action = getAction(ctx, entry);
                if (action == null || action == Action.IGNORE) continue;

                String name = entry.getName();

                // Copy or modify, open input and output streams
                try (InputStream entryIn = in.getInputStream(entry)) {
                    baos.reset();
                    switch (action) {
                        case COPY_RENAME:
                            name = rename(ctx, entry);
                        case COPY:
                            IO.copy(entryIn, baos);
                            break;

                        // Modify the jar file, can use ASM or any kind of form
                        case MODIFY:
                            name = modify(ctx, entry, entryIn, baos);
                            break;
                    }
                }

                // Write copy buffer to output file
                JarEntry outEntry = new JarEntry(name);
                out.putNextEntry(outEntry);
                baos.writeTo(out);
                out.closeEntry();

                ctx.progress(n++, size);
            }
        }

        ctx.done();
    }

    public enum Action {
        IGNORE,
        COPY,
        COPY_RENAME,
        MODIFY
    }
}
