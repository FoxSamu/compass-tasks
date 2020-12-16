package net.shadew.gradle.decompile.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VersionManifest {
    private final Version latestRelease;
    private final Version latestSnapshot;
    private final Map<String, Version> versions;

    private VersionManifest(String latestRelease, String latestSnapshot, Map<String, Version> versions) {
        this.latestRelease = versions.get(latestRelease);
        this.latestSnapshot = versions.get(latestSnapshot);
        this.versions = versions;
    }

    public static VersionManifest fromJson(JsonObject json) {
        JsonArray versions = json.getAsJsonArray("versions");
        Map<String, Version> versionMap = new HashMap<>();
        for (JsonElement elm : versions) {
            Version ver = Version.fromJson(elm.getAsJsonObject());
            versionMap.put(ver.getId(), ver);
        }
        JsonObject latest = json.getAsJsonObject("latest");
        String latestRelease = latest.get("release").getAsString();
        String latestSnapshot = latest.get("snapshot").getAsString();
        return new VersionManifest(latestRelease, latestSnapshot, Collections.unmodifiableMap(versionMap));
    }

    public static VersionManifest fromFile(File file) throws IOException {
        try(FileReader reader = new FileReader(file)) {
            JsonElement element = JsonParser.parseReader(reader);
            return fromJson(element.getAsJsonObject());
        }
    }

    public Map<String, Version> getVersions() {
        return versions;
    }

    public Version getLatestRelease() {
        return latestRelease;
    }

    public Version getLatestSnapshot() {
        return latestSnapshot;
    }

    public Version getVersion(String id) {
        if (id.equals("snapshot-latest"))
            return latestSnapshot;
        if (id.equals("release-latest"))
            return latestRelease;

        return versions.get(id);
    }

    public static class Version {
        private final String id;
        private final VersionType type;
        private final String url;

        private Version(String id, VersionType type, String url) {
            this.id = id;
            this.type = type;
            this.url = url;
        }

        public String getId() {
            return id;
        }

        public VersionType getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }

        public static Version fromJson(JsonObject json) {
            return new Version(
                json.get("id").getAsString(),
                VersionType.valueOf(json.get("type").getAsString().toUpperCase()),
                json.get("url").getAsString()
            );
        }
    }

    public enum VersionType {
        SNAPSHOT,
        RELEASE,
        OLD_ALPHA,
        OLD_BETA
    }
}
