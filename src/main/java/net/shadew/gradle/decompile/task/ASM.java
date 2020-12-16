package net.shadew.gradle.decompile.task;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;

import net.shadew.gradle.decompile.TaskRunContext;

public abstract class ASM extends JarModify<ASM> {
    public ASM(String name) {
        super(name);
    }

    @Override
    protected Action getAction(TaskRunContext ctx, JarEntry entry) throws Exception {
        if (entry.getName().endsWith(".class")) {
            return Action.MODIFY;
        }
        return Action.COPY;
    }

    @Override
    protected String modify(TaskRunContext ctx, JarEntry entry, InputStream in, OutputStream out) throws Exception {
        ClassNode node = new ClassNode();

        ClassReader reader = new ClassReader(in);
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        modify(ctx, entry, node);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);

        out.write(writer.toByteArray());
        return node.name + ".class";
    }

    @Override
    protected String rename(TaskRunContext ctx, JarEntry entry) throws Exception {
        return entry.getName();
    }

    protected abstract ClassNode modify(TaskRunContext ctx, JarEntry entry, ClassNode in) throws Exception;
}
