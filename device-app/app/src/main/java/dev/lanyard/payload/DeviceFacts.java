package dev.lanyard.payload;

import android.os.Build;

import java.io.FileInputStream;

/**
 * The exact device facts the payload manifest is matched against, plus the
 * identifiers the research record needs: manufacturer, board, SoC, build id,
 * incremental, security patch, build type/tags, bootloader and the app's own
 * SELinux domain.
 */
public final class DeviceFacts {
    public final String manufacturer;
    public final String brand;
    public final String model;
    public final String device;
    public final String product;
    public final String board;
    public final String hardware;
    public final String soc;
    public final String socManufacturer;
    public final String bootloader;
    public final int sdk;
    public final String release;
    public final String securityPatch;
    public final String buildId;
    public final String incremental;
    public final String buildType;
    public final String buildTags;
    public final String abi;
    public final String kernel;
    public final String fingerprint;
    public final String appDomain;

    public DeviceFacts(
            String manufacturer,
            String brand,
            String model,
            String device,
            String product,
            String board,
            String hardware,
            String soc,
            String socManufacturer,
            String bootloader,
            int sdk,
            String release,
            String securityPatch,
            String buildId,
            String incremental,
            String buildType,
            String buildTags,
            String abi,
            String kernel,
            String fingerprint,
            String appDomain) {
        this.manufacturer = manufacturer;
        this.brand = brand;
        this.model = model;
        this.device = device;
        this.product = product;
        this.board = board;
        this.hardware = hardware;
        this.soc = soc;
        this.socManufacturer = socManufacturer;
        this.bootloader = bootloader;
        this.sdk = sdk;
        this.release = release;
        this.securityPatch = securityPatch;
        this.buildId = buildId;
        this.incremental = incremental;
        this.buildType = buildType;
        this.buildTags = buildTags;
        this.abi = abi;
        this.kernel = kernel;
        this.fingerprint = fingerprint;
        this.appDomain = appDomain;
    }

    /** Test convenience: the fields the manifest gate reads, nothing more. */
    DeviceFacts(String model, String device, int sdk, String release, String abi, String kernel, String fingerprint) {
        this("unknown", "unknown", model, device, "unknown", "unknown", "unknown",
                "unknown", "unknown", "unknown", sdk, release, "unknown", "unknown",
                "unknown", "unknown", "unknown", abi, kernel, fingerprint, "unknown");
    }

    public static DeviceFacts current() {
        String abi = Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown";
        return new DeviceFacts(
                safe(Build.MANUFACTURER),
                safe(Build.BRAND),
                safe(Build.MODEL),
                safe(Build.DEVICE),
                safe(Build.PRODUCT),
                safe(Build.BOARD),
                safe(Build.HARDWARE),
                safe(Build.SOC_MODEL),
                safe(Build.SOC_MANUFACTURER),
                safe(Build.BOOTLOADER),
                Build.VERSION.SDK_INT,
                safe(Build.VERSION.RELEASE),
                safe(Build.VERSION.SECURITY_PATCH),
                safe(Build.ID),
                safe(Build.VERSION.INCREMENTAL),
                safe(Build.TYPE),
                safe(Build.TAGS),
                abi,
                safe(System.getProperty("os.version")),
                safe(Build.FINGERPRINT),
                appDomain());
    }

    /**
     * The app's own SELinux context. The global enforce flag under
     * /sys/fs/selinux is not readable from untrusted_app on this build, so the
     * app reports the domain it actually runs in and leaves the policy mode to
     * the shell-side capture (getenforce) recorded in docs/DEVICES.md.
     */
    private static String appDomain() {
        try (FileInputStream input = new FileInputStream("/proc/self/attr/current")) {
            byte[] buffer = new byte[256];
            int read = input.read(buffer);
            if (read <= 0) {
                return "unknown";
            }
            return new String(buffer, 0, read, java.nio.charset.StandardCharsets.UTF_8)
                    .trim()
                    .replace("\u0000", "");
        } catch (Throwable error) {
            return "unknown";
        }
    }

    private static String safe(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }
}
