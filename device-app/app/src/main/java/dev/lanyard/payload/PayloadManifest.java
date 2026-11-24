package dev.lanyard.payload;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The Open Crown page-cache payload manifest, read from the APK assets.
 *
 * <p>Only a payload whose manifest names this exact model, device, build and
 * kernel as {@code verified} may be executed. A specification-only match is
 * displayed as compatible and untested, and the run buttons stay disabled.</p>
 */
public final class PayloadManifest {
    public enum Compatibility {
        VERIFIED,
        COMPATIBLE,
        UNSUPPORTED
    }

    public static final class DeviceRow {
        public final String model;
        public final String device;
        public final String kernelRelease;
        public final String build;
        public final int androidSdk;
        public final String status;

        public DeviceRow(
                String model,
                String device,
                String kernelRelease,
                String build,
                int androidSdk,
                String status) {
            this.model = model;
            this.device = device;
            this.kernelRelease = kernelRelease;
            this.build = build;
            this.androidSdk = androidSdk;
            this.status = status;
        }
    }

    public static final class Match {
        public final Compatibility compatibility;
        public final String reason;

        public Match(Compatibility compatibility, String reason) {
            this.compatibility = compatibility;
            this.reason = reason;
        }

        public boolean canRun() {
            return compatibility == Compatibility.VERIFIED;
        }
    }

    public static final String ID = "dev.lanyard.payload.pagecache-relay";
    public static final String ASSET_DIR = "payloads/" + ID;

    public final String id;
    public final String name;
    public final String version;
    public final String family;
    public final String entry;
    public final int entrySize;
    public final List<String> abis;
    public final int androidMinSdk;
    public final String kernelPattern;
    public final List<String> capabilities;
    public final String persistence;
    public final List<DeviceRow> devices;

    public PayloadManifest(
            String id,
            String name,
            String version,
            String family,
            String entry,
            int entrySize,
            List<String> abis,
            int androidMinSdk,
            String kernelPattern,
            List<String> capabilities,
            String persistence,
            List<DeviceRow> devices) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.family = family;
        this.entry = entry;
        this.entrySize = entrySize;
        this.abis = abis;
        this.androidMinSdk = androidMinSdk;
        this.kernelPattern = kernelPattern;
        this.capabilities = capabilities;
        this.persistence = persistence;
        this.devices = devices;
    }

    public static PayloadManifest load(Context context) throws Exception {
        JSONObject root = new JSONObject(readAsset(context, ASSET_DIR + "/manifest.json"));
        JSONObject requirements = root.getJSONObject("requirements");

        List<String> abis = new ArrayList<>();
        JSONArray abiArray = requirements.getJSONArray("abis");
        for (int i = 0; i < abiArray.length(); i++) {
            abis.add(abiArray.getString(i));
        }

        List<String> capabilities = new ArrayList<>();
        JSONArray capabilityArray = root.optJSONArray("capabilities");
        if (capabilityArray != null) {
            for (int i = 0; i < capabilityArray.length(); i++) {
                capabilities.add(capabilityArray.getString(i));
            }
        }

        List<DeviceRow> devices = new ArrayList<>();
        JSONArray deviceArray = root.optJSONArray("devices");
        if (deviceArray != null) {
            for (int i = 0; i < deviceArray.length(); i++) {
                JSONObject row = deviceArray.getJSONObject(i);
                devices.add(new DeviceRow(
                        row.optString("model", ""),
                        row.optString("device", ""),
                        row.optString("kernel_release", ""),
                        row.optString("build", ""),
                        row.optInt("android_sdk", 0),
                        row.optString("status", "candidate")));
            }
        }

        return new PayloadManifest(
                root.getString("id"),
                root.optString("name", "Payload"),
                root.optString("version", "0"),
                root.optString("family", "unknown"),
                root.optString("entry", "payload.bin"),
                root.optInt("entry_size", 0),
                abis,
                requirements.optInt("android_min_sdk", 0),
                requirements.optString("kernel_pattern", ".*"),
                capabilities,
                root.optString("persistence", "boot"),
                devices);
    }

    public byte[] payloadBytes(Context context, String assetName) throws IOException {
        return readAssetBytes(context, ASSET_DIR + "/" + assetName);
    }

    public Match match(DeviceFacts facts) {
        // Specification limits are checked before any verified row, so an
        // unsupported ABI, SDK or kernel family can never be reported as a
        // verified target.
        if (!abis.contains(facts.abi)) {
            return new Match(
                    Compatibility.UNSUPPORTED,
                    "ABI " + facts.abi + " is not in the manifest");
        }
        if (facts.sdk < androidMinSdk) {
            return new Match(
                    Compatibility.UNSUPPORTED,
                    "Android SDK " + facts.sdk + " is below " + androidMinSdk);
        }
        try {
            if (!Pattern.compile(kernelPattern).matcher(facts.kernel).matches()) {
                return new Match(
                        Compatibility.UNSUPPORTED,
                        "kernel " + facts.kernel + " is outside the supported family");
            }
        } catch (PatternSyntaxException error) {
            return new Match(
                    Compatibility.UNSUPPORTED,
                    "the manifest kernel pattern is not valid");
        }
        for (DeviceRow row : devices) {
            boolean modelMatches = row.model.equalsIgnoreCase(facts.model);
            boolean deviceMatches = row.device.isEmpty()
                    || row.device.equalsIgnoreCase(facts.device);
            boolean buildMatches = row.build.isEmpty()
                    || facts.fingerprint.contains(row.build);
            boolean kernelMatches = row.kernelRelease.isEmpty()
                    || row.kernelRelease.equals(facts.kernel);
            if ("verified".equals(row.status)
                    && modelMatches
                    && deviceMatches
                    && buildMatches
                    && kernelMatches) {
                return new Match(
                        Compatibility.VERIFIED,
                        row.model + " / " + row.kernelRelease + " is a verified target");
            }
        }

        return new Match(
                Compatibility.COMPATIBLE,
                "Specification matches, but " + facts.model
                        + " has no verified device row. Refusing to run.");
    }

    private static String readAsset(Context context, String name) throws IOException {
        return new String(readAssetBytes(context, name), StandardCharsets.UTF_8);
    }

    private static byte[] readAssetBytes(Context context, String name) throws IOException {
        try (InputStream input = context.getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
