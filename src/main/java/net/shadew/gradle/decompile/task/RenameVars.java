package net.shadew.gradle.decompile.task;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;

import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.gradle.decompile.util.Constants;

public class RenameVars extends ASM {
    private String versionDir = Constants.VERSION_DIR;

    public RenameVars(String name) {
        super(name);
    }

    public RenameVars versionDir(String versionDir) {
        this.versionDir = versionDir;
        return this;
    }

    @Override
    protected String getInputFile(TaskRunContext ctx) {
        return ctx.file(versionDir + "_tmp.jar");
    }

    @Override
    protected String getOutputFile(TaskRunContext ctx) {
        return ctx.file(versionDir + "merged.jar");
    }

    @Override
    protected ClassNode modify(TaskRunContext ctx, JarEntry entry, ClassNode node) {
        for(MethodNode m : node.methods) {
            modify(m);
        }
        return node;
    }

    private Map<Integer, Integer> idcs = new HashMap<>();

    protected void modify(MethodNode node) {
        int s = node.access & Opcodes.ACC_STATIC;
        boolean isStatic = s != 0;

        int hash = (node.name + node.desc).hashCode();
        int n = idcs.computeIfAbsent(hash, k -> 0);

        if (node.localVariables != null) {
            List<LocalVariableNode> list = node.localVariables;

            for (LocalVariableNode lvn : list) {
                if (!isStatic && lvn.index == 0) {
                    lvn.name = "this";
                } else {
                    lvn.name = "lvt_" + Integer.toHexString(hash) + "_" + n++;
                }
            }
        }

        idcs.put(hash, n);
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File file = new File(ctx.file(versionDir + "merged.jar"));
        File tmp = new File(ctx.file(versionDir + "_tmp.jar"));

        tmp.getParentFile().mkdirs();
        Files.move(file.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);

        try {
            idcs.clear();
            super.execute(ctx);
        } finally {
            tmp.delete();
        }
    }
}
