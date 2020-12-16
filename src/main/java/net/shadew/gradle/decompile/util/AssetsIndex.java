package net.shadew.gradle.decompile.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public class AssetsIndex implements Iterable<AssetsIndex.Asset> {
    private final Map<String, Asset> assets;
    private final Map<String, Asset> byHash;

    private AssetsIndex(Map<String, Asset> assets, Map<String, Asset> byHash) {
        this.assets = assets;
        this.byHash = byHash;
    }

    public Collection<Asset> getAssets() {
        return assets.values();
    }

    @Override
    public Iterator<Asset> iterator() {
        return assets.values().iterator();
    }

    public Asset getAsset(String name) {
        return assets.get(name);
    }

    public Asset getByHash(String hash) {
        return byHash.get(hash);
    }

    public static AssetsIndex fromJson(JsonObject json) {
        Map<String, Asset> assets = new LinkedHashMap<>();
        Map<String, Asset> byHash = new LinkedHashMap<>();
        for(Map.Entry<String, JsonElement> entry : json.entrySet()) {
            Asset asset = Asset.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
            assets.put(asset.getName(), asset);
            byHash.put(asset.getHash(), asset);
        }
        return new AssetsIndex(Collections.unmodifiableMap(assets), Collections.unmodifiableMap(byHash));
    }

    public static AssetsIndex fromFile(File file) throws Exception {
        try(FileReader reader = new FileReader(file)) {
            JsonElement element = JsonParser.parseReader(reader);
            return fromJson(element.getAsJsonObject().getAsJsonObject("objects"));
        }
    }

    public static class Asset {
        private final String name;
        private final String hash;
        private final long size;

        private Asset(String name, String hash, long size) {
            this.name = name;
            this.hash = hash;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public String getHash() {
            return hash;
        }

        public String getHashPath() {
            return hash.substring(0, 2) + "/" + hash;
        }

        public String getUrl() {
            return Constants.RESOURCES_URL + getHashPath();
        }

        public long getSize() {
            return size;
        }

        public static Asset fromJson(String name, JsonObject object) {
            return new Asset(
                name,
                object.get("hash").getAsString(),
                object.get("size").getAsLong()
            );
        }
    }
}
