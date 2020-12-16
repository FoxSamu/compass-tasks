package net.shadew.gradle.decompile.task;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shadew.asm.mappings.io.RMapMappingsIO;
import net.shadew.asm.mappings.model.Mappings;
import net.shadew.asm.mappings.remap.*;
import net.shadew.gradle.decompile.TaskRunContext;
import net.shadew.util.misc.IO;

public abstract class Remap extends Task {
    protected Remap(String name) {
        super(name);
    }

    protected abstract String mappings(TaskRunContext ctx);
    protected abstract String inJar(TaskRunContext ctx);
    protected abstract String outJar(TaskRunContext ctx);

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File mappings = new File(mappings(ctx));
        File inJar = new File(inJar(ctx));
        File outJar = new File(outJar(ctx));

        if (outJar.exists()) {
            ctx.upToDate();
            return;
        }

        outJar.getParentFile().mkdirs();


        try (JarClassSource source = new JarClassSource(inJar);
             FileReader reader = new FileReader(mappings);
             Stream<ClassReference> classes = source.allClasses();
             Stream<ClassReference> classes2 = source.allClasses();
             AsmCache cache = new AsmCache(source, 1024);
             JarClassExport exp = new JarClassExport(outJar)) {

            int size = (int) classes2.count();
            int[] n = {0};

            Mappings maps = RMapMappingsIO.read(reader);
            SuperclassCache supers = new SuperclassCache(cache);
            AsmRemapper remapper = new AsmRemapper(maps, supers);

            classes.forEach(cls -> {
                try {
                    ClassNode node = cache.resolve(cls.name());
                    ClassWriter writer = new ClassWriter(0);
                    ClassRemapper cr = new ClassRemapper(writer, remapper);
                    node.accept(cr);
                    String outName = remapper.map(node.name);
                    try (OutputStream out = exp.export(outName)) {
                        out.write(writer.toByteArray());
                    }

                    n[0] ++;
                    ctx.progress((double) n[0] / size);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            });

            JarFile jarFile = source.getJar();
            List<JarEntry> resources = jarFile.stream()
                                              .filter(e -> !e.getName().endsWith(".class"))
//                                              .filter(e -> !e.getName().endsWith("MANIFEST.MF"))
                                              .collect(Collectors.toList());
            JarOutputStream jarOut = exp.getOut();

            for(JarEntry e : resources) {
                try(InputStream in = jarFile.getInputStream(e)) {
                    jarOut.putNextEntry(e);
                    IO.copy(in, jarOut);
                    jarOut.closeEntry();
                }
            }
        }

        ctx.done();
    }

    private static class JarClassSource implements ClassSource {
        private final JarFile jar;

        JarClassSource(File file) throws IOException {
            this.jar = new JarFile(file);
        }

        public JarFile getJar() {
            return jar;
        }

        @Override
        public ClassReference resolveClass(String internalName) {
            JarEntry entry = jar.getJarEntry(internalName + ".class");
            if (entry == null) return null;
            return new Ref(internalName, jar, entry);
        }

        @Override
        public Stream<ClassReference> allClasses() {
            return jar.stream()
                      .filter(entry -> entry.getName().endsWith(".class"))
                      .map(entry -> {
                          String name = entry.getName();
                          name = name.substring(0, name.length() - 6);
                          return new Ref(name, jar, entry);
                      });
        }

        @Override
        public void close() throws IOException {
            jar.close();
        }

        private static class Ref implements ClassReference {
            private final String name;
            private final JarFile jar;
            private final JarEntry entry;

            private Ref(String name, JarFile jar, JarEntry entry) {
                this.name = name;
                this.jar = jar;
                this.entry = entry;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public InputStream openStream() throws IOException {
                return jar.getInputStream(entry);
            }
        }
    }

    private static class JarClassExport implements ClassExport {
        private final JarOutputStream out;
        private final WrappingStream wrapper = new WrappingStream();

        JarClassExport(File jar) throws IOException {
            jar.getParentFile().mkdirs();
            this.out = new JarOutputStream(new FileOutputStream(jar));
        }

        @Override
        public OutputStream export(String className) throws IOException {
            out.putNextEntry(new JarEntry(className + ".class"));
            return wrapper;
        }

        public JarOutputStream getOut() {
            return out;
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

        private class WrappingStream extends OutputStream {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                out.closeEntry();
            }
        }
    }
}
