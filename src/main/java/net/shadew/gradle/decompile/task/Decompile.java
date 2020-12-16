package net.shadew.gradle.decompile.task;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.shadew.gradle.decompile.TaskRunContext;

public abstract class Decompile extends Task {
    public Decompile(String name) {
        super(name);
    }

    protected abstract String getInputFile(TaskRunContext ctx) throws Exception;
    protected abstract String getOutputDir(TaskRunContext ctx) throws Exception;

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File inFile = new File(getInputFile(ctx));
        File outFile = new File(getOutputDir(ctx));

        if(outFile.exists()) {
            ctx.upToDate();
            return;
        }

        outFile.getParentFile().mkdirs();

        Map<String, Object> opts = new HashMap<>();
        opts.put(IFernflowerPreferences.REMOVE_SYNTHETIC, true);
        opts.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, true);
        opts.put(IFernflowerPreferences.HIDE_EMPTY_SUPER, true);
        opts.put(IFernflowerPreferences.HIDE_DEFAULT_CONSTRUCTOR, true);
        ConsoleDecompiler decompiler = new ConsoleDecompiler(outFile, opts);
        decompiler.addSpace(inFile, true);
        decompiler.decompileContext();

//        try (JarFile in = new JarFile(inFile, false);
//             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFile))) {
//
//            List<JarEntry> entries = in.stream()
//                                       .filter(e -> e.getName().endsWith(".class"))
//                                       .filter(e -> !e.getName().contains("$"))
//                                       .collect(Collectors.toList());
//
//            JarTypeResolver typeResolver = new JarTypeResolver(in);
//
//            Map<String, String> opts = new HashMap<>();
//            opts.put(OptionsImpl.DECOMPILER_COMMENTS.getName(), "false");
//            opts.put(OptionsImpl.COMMENT_MONITORS.getName(), "false");
//
//            CfrDriver driver = new CfrDriver.Builder().withClassFileSource(typeResolver)
//                                                      .withOutputSink(new SinkFactory(new PrintStream(out)))
//                                                      .withOptions(opts)
//                                                      .build();
//
//            // Disable java logging, Procyon uses it but it breaks our system
//            Logger rootLogger = LogManager.getLogManager().getLogger("");
//            rootLogger.setLevel(Level.OFF);
//            for (Handler h : rootLogger.getHandlers()) {
//                h.setLevel(Level.OFF);
//            }
//
//            int size = entries.size();
//            int n = 0;
//
//            for(JarEntry e : entries) {
//                String name = e.getName();
//                name = name.substring(0, name.length() - 6);
//                out.putNextEntry(new JarEntry(name + ".java"));
//
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                OutputStreamWriter writer = new OutputStreamWriter(baos);
//                PlainTextOutput textOut = new PlainTextOutput(writer);
//
//                DecompilerSettings settings = DecompilerSettings.javaDefaults();
//                settings.setTypeLoader(typeResolver);
//                settings.setUnicodeOutputEnabled(true);
//                settings.setIncludeErrorDiagnostics(false);
//                settings.setIncludeLineNumbersInBytecode(true);
//                settings.setSimplifyMemberReferences(false);
//                settings.setForceExplicitImports(true);
//
//                try {
//                    Decompiler.decompile(name, textOut, settings);
//                    writer.flush();
//                    baos.writeTo(out);
//                } catch (Throwable exc) {
//                    ctx.warning("Procyon failed on " + name + ", trying CFR");
//                    driver.analyse(Collections.singletonList(name));
//                }
//
//                out.closeEntry();
//
//                n ++;
//                ctx.progress(n, size);
//                ctx.status(n + "/" + size);
//            }
//        }

        ctx.done();
    }

//    private static class JarTypeResolver implements ITypeLoader, ClassFileSource, IBytecodeProvider {
//        private final JarFile jar;
//
//        private JarTypeResolver(JarFile jar) {
//            this.jar = jar;
//        }
//
//        @Override
//        public boolean tryLoadType(String internalName, Buffer buffer) {
//            JarEntry e = jar.getJarEntry(internalName + ".class");
//            if(e != null) {
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                try(InputStream in = jar.getInputStream(e)) {
//                    IO.copy(in, baos);
//
//                } catch (IOException exc) {
//                    return false;
//                }
//                byte[] bytes = baos.toByteArray();
//                buffer.putByteArray(bytes, 0, bytes.length);
//                buffer.flip();
//                return true;
//            }
//            return false;
//        }
//
//        @Override
//        public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
//
//        }
//
//        @Override
//        public Collection<String> addJar(String jarPath) {
//            return Collections.emptyList();
//        }
//
//        @Override
//        public String getPossiblyRenamedPath(String path) {
//            return path;
//        }
//
//        @Override
//        public Pair<byte[], String> getClassFileContent(String path) throws IOException {
//            JarEntry e = jar.getJarEntry(path);
//            if(e != null) {
//                try(InputStream in = jar.getInputStream(e)) {
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    IO.copy(in, baos);
//                    return Pair.make(baos.toByteArray(), path);
//                }
//            }
//            throw new IOException("not found");
//        }
//
//        @Override
//        public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
//            JarEntry e = jar.getJarEntry(internalPath + ".class");
//            if(e != null) {
//                try(InputStream in = jar.getInputStream(e)) {
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    IO.copy(in, baos);
//                    return baos.toByteArray();
//                }
//            }
//            throw new IOException("not found");
//        }
//    }
//
//    private static class FernflowerOut implements IResultSaver {
//        private final ZipOutputStream out;
//
//        private FernflowerOut(ZipOutputStream out) {
//            this.out = out;
//        }
//
//        @Override
//        public void saveFolder(String path) {
//
//        }
//
//        @Override
//        public void copyFile(String source, String path, String entryName) {
//
//        }
//
//        @Override
//        public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
//            try {
//                out.putNextEntry(new ZipEntry(qualifiedName.replace('.', '/') + ".java"));
//                new PrintStream(out).println(content);
//                out.closeEntry();
//            } catch (IOException exc) {
//                throw new RuntimeException(exc);
//            }
//        }
//
//        @Override
//        public void createArchive(String path, String archiveName, Manifest manifest) {
//
//        }
//
//        @Override
//        public void saveDirEntry(String path, String archiveName, String entryName) {
//
//        }
//
//        @Override
//        public void copyEntry(String source, String path, String archiveName, String entry) {
//
//        }
//
//        @Override
//        public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
//
//        }
//
//        @Override
//        public void closeArchive(String path, String archiveName) {
//
//        }
//    }
//
//    private static class SinkFactory implements OutputSinkFactory {
//        private final PrintStream out;
//
//        private SinkFactory(PrintStream out) {
//            this.out = out;
//        }
//
//        @Override
//        public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
//            return Collections.singletonList(SinkClass.STRING);
//        }
//
//        @Override
//        @SuppressWarnings("unchecked")
//        public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
//            if(sinkType != SinkType.JAVA) return (Sink<T>) new EmptySinkImpl();
//            return (Sink<T>) new SinkImpl();
//        }
//
//        private class SinkImpl implements Sink<String> {
//
//            @Override
//            public void write(String sinkable) {
//                out.print(sinkable);
//            }
//        }
//
//        private class EmptySinkImpl implements Sink<String> {
//
//            @Override
//            public void write(String sinkable) {
//            }
//        }
//    }
}
