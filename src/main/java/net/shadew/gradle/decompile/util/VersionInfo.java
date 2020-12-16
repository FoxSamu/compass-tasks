package net.shadew.gradle.decompile.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public class VersionInfo {
    private final String assets;
    private final Download assetsIndex;
    private final Download clientJar;
    private final Download clientMappings;
    private final Download serverJar;
    private final Download serverMappings;
    private final List<Library> libraries;
    private final Map<OS, List<Library>> librariesByOs;

    private VersionInfo(String assets, Download assetsIndex, Download clientJar, Download clientMappings, Download serverJar, Download serverMappings, List<Library> libraries) {
        this.assets = assets;
        this.assetsIndex = assetsIndex;
        this.clientJar = clientJar;
        this.clientMappings = clientMappings;
        this.serverJar = serverJar;
        this.serverMappings = serverMappings;
        this.libraries = libraries;

        Map<OS, List<Library>> libsByOs = new EnumMap<>(OS.class);
        for (OS os : OS.values()) {
            List<Library> libs = new ArrayList<>();
            for (Library lib : libraries) {
                if (lib.allowedIn(os)) {
                    libs.add(lib);
                }
            }
            libsByOs.put(os, Collections.unmodifiableList(libs));
        }
        librariesByOs = Collections.unmodifiableMap(libsByOs);
    }

    public String getAssets() {
        return assets;
    }

    public Download getAssetsIndex() {
        return assetsIndex;
    }

    public Download getClientJar() {
        return clientJar;
    }

    public Download getClientMappings() {
        return clientMappings;
    }

    public Download getServerJar() {
        return serverJar;
    }

    public Download getServerMappings() {
        return serverMappings;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public List<Library> getLibraries(OS os) {
        return librariesByOs.get(os);
    }

    public static VersionInfo fromJson(JsonObject json) {
        JsonObject downloads = json.getAsJsonObject("downloads");

        List<Library> libraries = new ArrayList<>();
        JsonArray jsonLibraries = json.getAsJsonArray("libraries");
        for (JsonElement element : jsonLibraries) {
            libraries.add(Library.fromJson(element.getAsJsonObject()));
        }

        return new VersionInfo(
            json.get("assets").getAsString(),
            Download.fromJson(json.getAsJsonObject("assetIndex")),
            Download.fromJson(downloads.getAsJsonObject("client")),
            Download.fromJson(downloads.getAsJsonObject("client_mappings")),
            Download.fromJson(downloads.getAsJsonObject("server")),
            Download.fromJson(downloads.getAsJsonObject("server_mappings")),
            Collections.unmodifiableList(libraries)
        );
    }

    public static VersionInfo fromFile(File file) throws Exception {
        try (FileReader reader = new FileReader(file)) {
            JsonElement element = JsonParser.parseReader(reader);
            return fromJson(element.getAsJsonObject());
        }
    }

    public static class Download {
        private final String url;
        private final String sha1;
        private final long size;

        private Download(String url, String sha1, long size) {
            this.url = url;
            this.sha1 = sha1;
            this.size = size;
        }

        public String getUrl() {
            return url;
        }

        public String getSha1() {
            return sha1;
        }

        public long getSize() {
            return size;
        }

        public static Download fromJson(JsonObject json) {
            return new Download(
                json.get("url").getAsString(),
                json.get("sha1").getAsString(),
                json.get("size").getAsLong()
            );
        }
    }

    public static class Library {
        private final LibraryDownload artifact;
        private final Map<String, LibraryDownload> classifiers;
        private final ArtifactName name;
        private final Natives natives;
        private final List<Rule> rules;

        private Library(LibraryDownload artifact, Map<String, LibraryDownload> classifiers, ArtifactName name, Natives natives, List<Rule> rules) {
            this.artifact = artifact;
            this.classifiers = classifiers;
            this.name = name;
            this.natives = natives;
            this.rules = rules;
        }

        public LibraryDownload getArtifactDownload() {
            return artifact;
        }

        public Map<String, LibraryDownload> getClassifierDownloads() {
            return classifiers;
        }

        public Set<String> getClassifiers() {
            return classifiers.keySet();
        }

        public LibraryDownload getClassifierDownload(String classifier) {
            return classifiers.get(classifier);
        }

        public LibraryDownload getNativeDownload(OS os) {
            return classifiers.get(natives.getNativeClassifier(os));
        }

        public LibraryDownload getSourcesDownload() {
            return classifiers.get("sources");
        }

        public LibraryDownload getJavaDocDownload() {
            return classifiers.get("javadoc");
        }

        public ArtifactName getName() {
            return name;
        }

        public Natives getNatives() {
            return natives;
        }

        public List<Rule> getRules() {
            return rules;
        }

        public boolean allowedIn(OS os) {
            if (rules.isEmpty()) return true;
            boolean allowed = false;
            for (Rule rule : rules) {
                if (rule.os == null || rule.os == os) {
                    allowed = rule.action == RuleAction.ALLOW;
                }
            }
            return allowed;
        }

        public static Library fromJson(JsonObject json) {
            JsonObject jsonDownloads = json.getAsJsonObject("downloads");
            JsonObject jsonClassifiers = jsonDownloads.getAsJsonObject("classifiers");

            LibraryDownload artifact = LibraryDownload.fromJson(jsonDownloads.getAsJsonObject("artifact"));
            Map<String, LibraryDownload> classifiers = new HashMap<>();
            if (jsonClassifiers != null) {
                for (Map.Entry<String, JsonElement> entry : jsonClassifiers.entrySet()) {
                    classifiers.put(entry.getKey(), LibraryDownload.fromJson(entry.getValue().getAsJsonObject()));
                }
            }
            classifiers = Collections.unmodifiableMap(classifiers);

            ArtifactName name = ArtifactName.fromString(json.get("name").getAsString());
            Natives natives;
            if (json.has("natives")) {
                natives = Natives.fromJson(json.getAsJsonObject("natives"));
            } else {
                natives = new Natives(Collections.emptyMap());
            }

            List<Rule> rules = new ArrayList<>();
            JsonArray jsonRules = json.getAsJsonArray("rules");
            if (jsonRules != null) {
                for (JsonElement element : jsonRules) {
                    rules.add(Rule.fromJson(element.getAsJsonObject()));
                }
            }
            rules = Collections.unmodifiableList(rules);

            return new Library(artifact, classifiers, name, natives, rules);
        }
    }

    public static class LibraryDownload extends Download {
        private final String path;

        private LibraryDownload(String url, String sha1, long size, String path) {
            super(url, sha1, size);
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public static LibraryDownload fromJson(JsonObject json) {
            return new LibraryDownload(
                json.get("url").getAsString(),
                json.get("sha1").getAsString(),
                json.get("size").getAsLong(),
                json.get("path").getAsString()
            );
        }
    }

    public static class ArtifactName {
        private final String group;
        private final String name;
        private final String version;

        private ArtifactName(String group, String name, String version) {
            this.group = group;
            this.name = name;
            this.version = version;
        }

        public String getGroup() {
            return group;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String toString() {
            return group + ":" + name + ":" + version;
        }

        public static ArtifactName fromString(String name) {
            String[] parts = name.split(":");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Incorrect artifact name: " + name);
            }

            return new ArtifactName(parts[0], parts[1], parts[2]);
        }
    }

    public static class Natives {
        private final Map<OS, String> nativeClassifiers;

        private Natives(Map<OS, String> nativeClassifiers) {
            this.nativeClassifiers = nativeClassifiers;
        }

        public String getNativeClassifier(OS os) {
            return nativeClassifiers.get(os);
        }

        public Map<OS, String> getNativeClassifiers() {
            return nativeClassifiers;
        }

        public static Natives fromJson(JsonObject json) {
            Map<OS, String> nativeClassifiers = new EnumMap<>(OS.class);
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                OS os = OS.valueOf(entry.getKey().toUpperCase());
                nativeClassifiers.put(os, entry.getValue().getAsString());
            }
            return new Natives(Collections.unmodifiableMap(nativeClassifiers));
        }
    }

    public static class Rule {
        private final RuleAction action;
        private final OS os;

        private Rule(RuleAction action, OS os) {
            this.action = action;
            this.os = os;
        }

        public RuleAction getAction() {
            return action;
        }

        public OS getOs() {
            return os;
        }

        public static Rule fromJson(JsonObject json) {
            return new Rule(
                RuleAction.valueOf(json.get("action").getAsString().toUpperCase()),
                json.has("os")
                ? OS.valueOf(json.getAsJsonObject("os").get("name").getAsString().toUpperCase())
                : null
            );
        }
    }

    public enum RuleAction {
        ALLOW,
        DISALLOW
    }
}
