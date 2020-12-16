package net.shadew.gradle.decompile.task;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import net.shadew.gradle.decompile.TaskRunContext;

/**
 * Deploys the input JAR file into a local maven repository, generating a POM file and copying the JAR file in the
 *
 */
public class Deploy extends Task {
    private final String inputJar;
    private final String repositoryPath;
    private final String groupId;
    private final String artifactId;
    private String dependencyFile;
    private String classifier;
    private final List<String> extraDeps = new ArrayList<>();

    public Deploy(String name, String jar, String repo, String id) {
        super(name);
        this.inputJar = jar;
        this.repositoryPath = repo;
        String[] ids = id.split(":");
        if(ids.length < 2) {
            throw new IllegalArgumentException("Invalid id");
        }
        this.groupId = ids[0];
        this.artifactId = ids[1];
    }

    public Deploy dependencyFile(String file) {
        dependencyFile = file;
        return this;
    }

    public Deploy extraDep(String dep) {
        extraDeps.add(dep);
        return this;
    }

    public Deploy classifier(String c) {
        classifier = c;
        return this;
    }

    @Override
    public void execute(TaskRunContext ctx) throws Exception {
        String version = ctx.env().getProperty("MCVersion");
        File path = new File(ctx.file(repositoryPath) + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version);
        path.mkdirs();

        // Generate POM file
        File pom = new File(path, artifactId + "-" + version + ".pom");
        try (FileOutputStream fos = new FileOutputStream(pom)) {
            PrintStream out = new PrintStream(fos);
            out.printf("<project " +
                           "xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
                           "mlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                           "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">%n");
            out.printf("    <modelVersion>4.0.0</modelVersion>%n");

            // Artifact coords
            out.printf("    <groupId>%s</groupId>%n", groupId);
            out.printf("    <artifactId>%s</artifactId>%n", artifactId);
            out.printf("    <version>%s</version>%n", version);

            if(classifier != null)
                out.printf("    <classifier>%s</classifier>%n", classifier);

            // Dependencies
            out.printf("    <dependencies>%n");

            // Deps from file
            if (dependencyFile != null) {
                File depsFile = new File(ctx.file(dependencyFile));

                try (FileReader depsReader = new FileReader(depsFile)) {
                    JsonElement deps = JsonParser.parseReader(depsReader);

                    for (JsonElement el : deps.getAsJsonArray()) {
                        printDep(ctx, out, el.getAsString());
                    }
                }
            }

            // Extra dependencies
            for (String str : extraDeps) {
                printDep(ctx, out, str.replace("[[MCVersion]]", version));
            }

            out.printf("    </dependencies>%n");
            out.printf("</project>%n");
        }

        // Copy JAR file
        File jar = new File(path, artifactId + "-" + version + ".jar");
        File in = new File(ctx.file(inputJar));
        Files.copy(in.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ctx.status("DONE");
    }

    private static void printDep(TaskRunContext ctx, PrintStream out, String str) {
        String[] parts = str.split(":");
        if (parts.length < 3) {
            ctx.error(str + ": invalid dependency");
            return;
        }

        out.printf("        <dependency>%n");
        out.printf("            <groupId>%s</groupId>%n", parts[0]);
        out.printf("            <artifactId>%s</artifactId>%n", parts[1]);
        out.printf("            <version>%s</version>%n", parts[2]);

        if (parts.length > 3)
            out.printf("            <classifier>%s</classifier>%n", parts[3]);

        out.printf("            <scope>compile</scope>%n");
        out.printf("        </dependency>%n");
    }
}
