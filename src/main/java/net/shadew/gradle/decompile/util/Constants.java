package net.shadew.gradle.decompile.util;

public class Constants {
    public static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String RESOURCES_URL = "http://resources.download.minecraft.net/";

    public static final String VERSION_DIR = "{workingDir}/versions/[[MCVersion]]/";
    public static final String ASSETS_DIR = "{workingDir}/assets/";
    public static final String INDEXES_DIR = ASSETS_DIR + "/indexes/";
    public static final String OBJECTS_DIR = ASSETS_DIR + "/objects/";

    public static final String OS = "OS";
    public static final String OS_NAME = "OSName";
    public static final String MC_VERSION = "MCVersion";
    public static final String ASSETS_VERSION = "AssetsVersion";
    public static final String ASSETS_INDEX = "AssetsIndex";
    public static final String VERSION_MANIFEST = "VersionManifest";
    public static final String VERSION_INFO = "VersionInfo";
}
