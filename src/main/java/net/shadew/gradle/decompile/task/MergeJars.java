package net.shadew.gradle.decompile.task;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import net.shadew.gradle.decompile.TaskRunContext;

public class MergeJars extends Task {
    private final String jar1;
    private final String jar2;
    private final AnnotationSupplier annotations;
    private final String outFile;

    public MergeJars(String name, String jar1, String jar2, AnnotationSupplier annotations, String out) {
        super(name);
        this.jar1 = jar1;
        this.jar2 = jar2;
        this.annotations = annotations;
        this.outFile = out;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        File in1 = new File(ctx.file(jar1));
        File in2 = new File(ctx.file(jar2));
        File out = new File(ctx.file(outFile));

        if (out.exists()) {
            ctx.upToDate();
            return;
        }

        // Ensure output directory exists
        out.getParentFile().mkdirs();

        try (JarFile jar1 = new JarFile(in1, false);
             JarFile jar2 = new JarFile(in2, false);
             JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(out))) {

            // Collect all classes to merge
            List<String> classes = Stream.of(jar1, jar2)
                                         .flatMap(jar -> jar.stream().map(ZipEntry::getName))
                                         .distinct()
                                         .filter(name -> name.endsWith(".class"))
                                         .collect(Collectors.toList());

            int size = classes.size();
            int n = 0;
            for (String cls : classes) {
                JarEntry entry1 = jar1.getJarEntry(cls);
                JarEntry entry2 = jar2.getJarEntry(cls);

                // Read input classes
                ClassNode cls1 = getClass(jar1, entry1);
                ClassNode cls2 = getClass(jar2, entry2);

                // Merge classes
                ClassNode merged = merge(cls1, cls2);

                // Export merged class
                ClassWriter writer = new ClassWriter(0);
                merged.accept(writer);

                // Write to output
                JarEntry outEntry = new JarEntry(entry1.getName());
                jarOut.putNextEntry(outEntry);
                jarOut.write(writer.toByteArray());
                jarOut.closeEntry();

                ctx.progress(n ++, size);
            }
        }

        ctx.done();
    }

    /**
     * Reads an ASM class from a JAR file
     */
    private static ClassNode getClass(JarFile file, JarEntry entry) throws Exception {
        if (entry == null) return null;
        try (InputStream in = file.getInputStream(entry)) {
            ClassNode out = new ClassNode();
            ClassReader reader = new ClassReader(in);
            reader.accept(out, ClassReader.EXPAND_FRAMES);
            return out;
        }
    }

    /**
     * Adds an invisible annotation to a class node
     */
    private static ClassNode annotate(ClassNode node, AnnotationNode annotation) {
        if (node.invisibleAnnotations == null)
            node.invisibleAnnotations = new ArrayList<>();
        node.invisibleAnnotations.add(annotation);
        return node;
    }

    /**
     * Adds an invisible annotation to a method node
     */
    private static MethodNode annotate(MethodNode node, AnnotationNode annotation) {
        if (node.invisibleAnnotations == null)
            node.invisibleAnnotations = new ArrayList<>();
        node.invisibleAnnotations.add(annotation);
        return node;
    }

    /**
     * Adds an invisible annotation to a field node
     */
    private static FieldNode annotate(FieldNode node, AnnotationNode annotation) {
        if (node.invisibleAnnotations == null)
            node.invisibleAnnotations = new ArrayList<>();
        node.invisibleAnnotations.add(annotation);
        return node;
    }

    /**
     * Merges two classes
     */
    private ClassNode merge(ClassNode cls1, ClassNode cls2) {
        // If both are null, return null
        if (cls1 == null && cls2 == null)
            return null;

        // Class exists on one side, annotate it and return
        if (cls1 == null)
            return annotate(cls2, annotations.getSelfExclusiveRight());
        if (cls2 == null)
            return annotate(cls1, annotations.getSelfExclusiveLeft());

        // Classes exist on both sides here, merge them

        // Some required preconditions
        if (cls1.access != cls2.access) {
            throw new RuntimeException("Can't merge classes with different modifiers");
        }
        if (!Objects.equals(cls1.superName, cls2.superName)) {
            throw new RuntimeException("Can't merge classes with different superclasses");
        }

        // Collect fields and methods in these sets/maps
        Set<String> allFields = new LinkedHashSet<>();
        Map<String, FieldNode> fields1 = new LinkedHashMap<>();
        Map<String, FieldNode> fields2 = new LinkedHashMap<>();

        Set<String> allMethods = new LinkedHashSet<>();
        Map<String, MethodNode> methods1 = new LinkedHashMap<>();
        Map<String, MethodNode> methods2 = new LinkedHashMap<>();

        for (FieldNode f : cls1.fields) {
            fields1.put(f.name, f);
            allFields.add(f.name);
        }
        for (FieldNode f : cls2.fields) {
            fields2.put(f.name, f);
            allFields.add(f.name);
        }
        for (MethodNode m : cls1.methods) {
            methods1.put(m.name + m.desc, m);
            allMethods.add(m.name + m.desc);
        }
        for (MethodNode m : cls2.methods) {
            methods2.put(m.name + m.desc, m);
            allMethods.add(m.name + m.desc);
        }

        // Join fields, annotate those that exist on one side only
        List<FieldNode> sharedFields = new ArrayList<>();
        for (String fieldName : allFields) {
            FieldNode field1 = fields1.get(fieldName);
            FieldNode field2 = fields2.get(fieldName);

            if (field1 != null && field2 != null) {
                if (!field1.desc.equals(field2.desc)) {
                    throw new RuntimeException("Cannot merge fields with different descriptors");
                }
                if (field1.access != field2.access) {
                    throw new RuntimeException("Cannot merge fields with different modifiers");
                }
                sharedFields.add(field1);
            } else if (field1 != null) {
                sharedFields.add(annotate(field1, annotations.getSelfExclusiveLeft()));
            } else if (field2 != null) {
                sharedFields.add(annotate(field2, annotations.getSelfExclusiveRight()));
            }
        }

        // Join methods, annotate those that exist on one side only
        List<MethodNode> sharedMethods = new ArrayList<>();
        for (String methodName : allMethods) {
            MethodNode method1 = methods1.get(methodName);
            MethodNode method2 = methods2.get(methodName);

            if (method1 != null && method2 != null) {
                if (method1.access != method2.access) {
                    throw new RuntimeException("Cannot merge fields with different modifiers");
                }
                sharedMethods.add(method1);
                methods1.remove(methodName);
                methods2.remove(methodName);
            } else if (method1 != null) {
                sharedMethods.add(annotate(method1, annotations.getSelfExclusiveLeft()));
            } else if (method2 != null) {
                sharedMethods.add(annotate(method2, annotations.getSelfExclusiveRight()));
            }
        }

        // Create merged class
        ClassNode node = new ClassNode();
        node.version = cls1.version;
        node.access = cls1.access;
        node.name = cls1.name;
        node.signature = cls1.signature;
        node.superName = cls1.superName;

        // Join interfaces
        List<String> allIfcs = new ArrayList<>();
        Set<String> ifcs1 = new LinkedHashSet<>();
        Set<String> ifcs2 = new LinkedHashSet<>();

        for (String ifc : cls1.interfaces) {
            if (!allIfcs.contains(ifc))
                allIfcs.add(ifc);
            ifcs1.add(ifc);
        }
        for (String ifc : cls2.interfaces) {
            if (!allIfcs.contains(ifc))
                allIfcs.add(ifc);
            ifcs2.add(ifc);
        }

        for (String ifc : allIfcs) {
            if (ifcs1.contains(ifc) && ifcs2.contains(ifc)) {
                ifcs1.remove(ifc);
                ifcs2.remove(ifc);
            }
        }

        node.interfaces = allIfcs;
        node.sourceFile = cls1.sourceFile;
        node.sourceDebug = cls1.sourceDebug;
        node.module = cls1.module;
        node.outerClass = cls1.outerClass;
        node.outerMethod = cls1.outerMethod;
        node.outerMethodDesc = cls1.outerMethodDesc;

        // Merge annotations
        node.visibleAnnotations = merge(cls1.visibleAnnotations, cls2.visibleAnnotations);
        node.invisibleAnnotations = merge(cls1.invisibleAnnotations, cls2.invisibleAnnotations);
        // Ignore type annotations, minecraft doesn't have them

        // Add interfaces annotation if interfaces don't match
        if (!ifcs1.isEmpty() || !ifcs2.isEmpty()) {
            AnnotationNode ifcsAnnotation = annotations.getInterfaceExclusive(ifcs1, ifcs2);
            annotate(node, ifcsAnnotation);
        }


        // Just add all custom attributes, they likely don't make any sense
        node.attrs = new ArrayList<>();
        if(cls1.attrs != null) node.attrs.addAll(cls1.attrs);
        if(cls2.attrs != null) node.attrs.addAll(cls2.attrs);

        // Join inner classes, which get annotated when merging these inner classes itself
        Set<InnerClassNode> innerClassNodes = new LinkedHashSet<>();
        innerClassNodes.addAll(cls1.innerClasses);
        innerClassNodes.addAll(cls2.innerClasses);
        node.innerClasses = new ArrayList<>(innerClassNodes);

        // More members that don't make sense since we use Java 8
        node.nestHostClass = cls1.nestHostClass;
        node.nestMembers = cls1.nestMembers;
        node.recordComponents = cls1.recordComponents;

        // Fields and methods
        node.fields = sharedFields;
        node.methods = sharedMethods;

        return node;
    }

    /**
     * Merges two lists of annotations
     */
    private static List<AnnotationNode> merge(List<AnnotationNode> anns1, List<AnnotationNode> anns2) {
        if (anns1 == null) anns1 = new ArrayList<>();
        if (anns2 == null) anns2 = new ArrayList<>();

        // List with all left-side annotations
        List<AnnotationNode> out = new ArrayList<>(anns1);

        for (AnnotationNode ann : anns2) {
            boolean merged = false;
            for (int i = 0, l = out.size(); i < l; i++) {
                // Try merge annotation
                AnnotationNode merge = merge(out.get(i), ann);
                if (merge != null) {
                    out.set(i, merge);
                    merged = true;
                    break;
                }
            }
            // If not merged, just add it
            if (!merged) {
                out.add(ann);
            }
        }
        return out.isEmpty() ? null : out;
    }

    /**
     * Merges two annotations
     */
    private static AnnotationNode merge(AnnotationNode a, AnnotationNode b) {
        if (!a.desc.equals(b.desc)) return null;
        if (isEmptyOrNull(a.values) && isEmptyOrNull(b.values)) return a;
        if (isEmptyOrNull(a.values) && !isEmptyOrNull(b.values)) return null;
        if (!isEmptyOrNull(a.values) && isEmptyOrNull(b.values)) return null;
        return a.values.equals(b.values) ? a : null;
    }

    /**
     * Returns whether the given list is empty or null
     */
    private static <T> boolean isEmptyOrNull(List<T> l) {
        return l == null || l.isEmpty();
    }

    /**
     * Supplies annotations to be added when classes, methods, fields or implementing interfaces don't match between
     * sides.
     */
    public interface AnnotationSupplier {
        /**
         * Supplies annotation for left-only field, method or class
         */
        AnnotationNode getSelfExclusiveLeft();

        /**
         * Supplies annotation for right-only field, method or class
         */
        AnnotationNode getSelfExclusiveRight();

        /**
         * Supplies annotation that makes a set of interfaces exclusive at a given side
         */
        AnnotationNode getInterfaceExclusive(Set<String> leftOnly, Set<String> rightOnly);
    }
}
