package net.shadew.decompile.test;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import net.shadew.gradle.decompile.Environment;
import net.shadew.gradle.decompile.TaskFailException;
import net.shadew.gradle.decompile.task.*;
import net.shadew.gradle.decompile.util.Constants;
import net.shadew.gradle.decompile.util.OS;

public class Test1 {
    public static void main(String[] args) throws TaskFailException {
        Task downloadVersionManifest = new SimpleDownload(
            "downloadVersionManifest",
            Constants.VERSION_MANIFEST_URL,
            "{workingDir}/version_manifest.json"
        ).lazy(true);

        Task loadVersionManifest = new LoadVersionManifest(
            "loadVersionManifest",
            "{workingDir}/version_manifest.json"
        );

        Task downloadVersionJson = new DownloadVersionInfo("downloadVersionJson");
        Task loadVersionInfo = new LoadVersionInfo("loadVersionInfo");

        Task downloadClient = new DownloadMinecraft("downloadClient", DownloadMinecraft.Type.CLIENT);
        Task downloadServer = new DownloadMinecraft("downloadServer", DownloadMinecraft.Type.SERVER);

        Task downloadClientMappings = new DownloadMinecraft("downloadClientMappings", DownloadMinecraft.Type.CLIENT_MAPPINGS);
        Task downloadServerMappings = new DownloadMinecraft("downloadServerMappings", DownloadMinecraft.Type.SERVER_MAPPINGS);

        Task stripServerLibs = new StripLibraries("stripServerLibs");

        Task downloadObjects = new NoOp("downloadObjects");

        Task downloadAssetsIndex = new DownloadAssetsIndex("downloadAssetsIndex");
        Task loadAssetsIndex = new LoadAssetsIndex("loadAssetsIndex");
        Task downloadAssets = new DownloadAssets("downloadAssets");

        Task collectLibraries = new CollectLibs("collectLibraries");

        Task joinMappings = new JoinMappings("joinMappings");

        Task remapClient = new RemapMinecraft("remapClient", RemapMinecraft.Type.CLIENT);
        Task remapServer = new RemapMinecraft("remapServer", RemapMinecraft.Type.SERVER);

        Task mergeJars = new MergeJars(
            "mergeJars",
            "{workingDir}/versions/[[MCVersion]]/client_mapped.jar",
            "{workingDir}/versions/[[MCVersion]]/server_mapped.jar",
            new AnnotationSupplier(),
            "{workingDir}/versions/[[MCVersion]]/merged.jar"
        );

        Task renameVars = new RenameVars("renameVars");

        Task decompile = new DecompileMinecraft("decompile");

        Task initializeMinecraft = new NoOp("initializeMinecraft");

        Task extractAssets = new ExtractAssets("extractAssets");

        Task makeAssetsArtifact = new Deploy(
            "makeAssetsArtifact",
            "{workingDir}/versions/[[MCVersion]]/assets.jar",
            "C:\\Users\\Shadew\\.m2\\repository\\",
            "net.minecraft:minecraft-assets"
        );

        Task makeArtifact = new Deploy(
            "makeArtifact",
            "{workingDir}/versions/[[MCVersion]]/merged.jar",
            "C:\\Users\\Shadew\\.m2\\repository\\",
            "net.minecraft:minecraft"
        ).dependencyFile(Constants.VERSION_DIR + "libraries_[[OSName]].json")
         .extraDep("net.minecraft:minecraft-assets:[[MCVersion]]");

        Task setup = new NoOp("setup");

        Task cleanVersion = new RemoveFile("cleanVersion", Constants.VERSION_DIR);
        Task cleanAssets = new RemoveFile("cleanAssets", Constants.ASSETS_DIR);

        Task unzipSources = new Unzip(
            "unzipSources",
            Constants.VERSION_DIR + "decompiled/merged.jar",
            Constants.VERSION_DIR + "sources/"
        );

        loadVersionManifest.dependsOn(downloadVersionManifest);
        downloadVersionJson.dependsOn(loadVersionManifest);
        loadVersionInfo.dependsOn(downloadVersionJson);
        downloadClient.dependsOn(loadVersionInfo);
        downloadServer.dependsOn(loadVersionInfo);
        downloadServer.doLast(stripServerLibs);
        downloadClientMappings.dependsOn(loadVersionInfo);
        downloadServerMappings.dependsOn(loadVersionInfo);
        downloadObjects.dependsOn(downloadClient);
        downloadObjects.dependsOn(downloadServer);
        downloadObjects.dependsOn(downloadClientMappings);
        downloadObjects.dependsOn(downloadServerMappings);
        downloadAssetsIndex.dependsOn(downloadObjects);
        loadAssetsIndex.dependsOn(downloadAssetsIndex);
        downloadAssets.dependsOn(loadAssetsIndex);
        collectLibraries.dependsOn(loadVersionInfo);
        remapClient.dependsOn(joinMappings);
        remapServer.dependsOn(joinMappings);
        joinMappings.dependsOn(downloadObjects);
        mergeJars.dependsOn(remapClient);
        mergeJars.dependsOn(remapServer);
        mergeJars.doLast(renameVars);
        decompile.dependsOn(mergeJars);
        initializeMinecraft.dependsOn(downloadAssets);
        initializeMinecraft.dependsOn(loadAssetsIndex);
        initializeMinecraft.dependsOn(collectLibraries);
        initializeMinecraft.dependsOn(decompile);
        initializeMinecraft.dependsOn(mergeJars);
        initializeMinecraft.dependsOn(extractAssets);
        makeAssetsArtifact.dependsOn(initializeMinecraft);
        makeArtifact.dependsOn(makeAssetsArtifact);
        setup.dependsOn(makeAssetsArtifact);
        setup.dependsOn(makeArtifact);
        unzipSources.dependsOn(decompile);
        setup.dependsOn(unzipSources);

        Environment environment = new Environment();
        environment.setProperty("MCVersion", "20w49a");
        environment.setProperty("OS", OS.get());
        environment.run(setup);
    }

    private static class AnnotationSupplier implements MergeJars.AnnotationSupplier {

        @Override
        public AnnotationNode getSelfExclusiveLeft() {
            return new AnnotationNode("Lnet/shadew/lodemc/sidemarker/ClientOnly;");
        }

        @Override
        public AnnotationNode getSelfExclusiveRight() {
            return new AnnotationNode("Lnet/shadew/lodemc/sidemarker/ServerOnly;");
        }

        @Override
        public AnnotationNode getInterfaceExclusive(Set<String> leftOnly, Set<String> rightOnly) {
            AnnotationNode node = new AnnotationNode("Lnet/shadew/lodemc/sidemarker/ExclusiveInterfaces;");
            node.values = new ArrayList<>();
            if (!leftOnly.isEmpty()) {
                node.values.add("clientOnly");
                node.values.add(leftOnly.stream().map(Type::getObjectType).collect(Collectors.toList()));
            }
            if (!rightOnly.isEmpty()) {
                node.values.add("serverOnly");
                node.values.add(rightOnly.stream().map(Type::getObjectType).collect(Collectors.toList()));
            }
            return node;
        }
    }
}
