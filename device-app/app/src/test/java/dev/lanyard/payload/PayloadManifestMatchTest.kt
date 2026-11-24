package dev.lanyard.payload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadManifestMatchTest {

    private val verifiedRow = PayloadManifest.DeviceRow(
        "SM-A528B",
        "a52sxq",
        "5.4.254-qgki-28575149-abA528BXXSBGYI3",
        "A528BXXSBGYI3",
        34,
        "verified",
    )

    private fun manifest(
        devices: List<PayloadManifest.DeviceRow> = listOf(verifiedRow),
        abis: List<String> = listOf("arm64-v8a"),
        minSdk: Int = 31,
        pattern: String = "^5\\.4\\.[0-9]+-qgki-.*$",
    ) = PayloadManifest(
        "dev.lanyard.payload.pagecache-relay",
        "Page-cache Relay",
        "0.1.0",
        "page-cache",
        "payload.bin",
        576,
        abis,
        minSdk,
        pattern,
        listOf("root", "init-domain-relay"),
        "boot",
        devices,
    )

    private fun facts(
        model: String = "SM-A528B",
        device: String = "a52sxq",
        sdk: Int = 34,
        abi: String = "arm64-v8a",
        kernel: String = "5.4.254-qgki-28575149-abA528BXXSBGYI3",
        fingerprint: String = "samsung/a52sxqeea/a52sxq:14/UP1A.231005.007/A528BXXSBGYI3:user/release-keys",
    ) = DeviceFacts(model, device, sdk, "14", abi, kernel, fingerprint)

    @Test
    fun `exact verified row can run`() {
        val match = manifest().match(facts())
        assertEquals(PayloadManifest.Compatibility.VERIFIED, match.compatibility)
        assertTrue(match.canRun())
    }

    @Test
    fun `same specification without a verified row is only compatible`() {
        val match = manifest(devices = emptyList()).match(facts())
        assertEquals(PayloadManifest.Compatibility.COMPATIBLE, match.compatibility)
        assertFalse(match.canRun())
    }

    @Test
    fun `a verified row for another model is not enough`() {
        val match = manifest().match(facts(model = "SM-G991B"))
        assertEquals(PayloadManifest.Compatibility.COMPATIBLE, match.compatibility)
        assertFalse(match.canRun())
    }

    @Test
    fun `a different kernel on the same model is not a verified target`() {
        val match = manifest().match(facts(kernel = "5.4.254-qgki-28575149-abA528BXXSBGYI4"))
        assertEquals(PayloadManifest.Compatibility.COMPATIBLE, match.compatibility)
    }

    @Test
    fun `a build outside the verified fingerprint is not a verified target`() {
        val match = manifest().match(facts(fingerprint = "samsung/a52sxqeea/a52sxq:14/OTHER/BUILD:user/release-keys"))
        assertEquals(PayloadManifest.Compatibility.COMPATIBLE, match.compatibility)
    }

    @Test
    fun `unsupported abi is refused before any kernel check`() {
        val match = manifest().match(facts(abi = "x86_64"))
        assertEquals(PayloadManifest.Compatibility.UNSUPPORTED, match.compatibility)
        assertFalse(match.canRun())
    }

    @Test
    fun `an sdk below the manifest minimum is unsupported`() {
        val match = manifest().match(facts(sdk = 30))
        assertEquals(PayloadManifest.Compatibility.UNSUPPORTED, match.compatibility)
    }

    @Test
    fun `a kernel outside the manifest family is unsupported`() {
        val match = manifest().match(facts(kernel = "6.1.0-qgki-123456"))
        assertEquals(PayloadManifest.Compatibility.UNSUPPORTED, match.compatibility)
    }

    @Test
    fun `an invalid kernel pattern fails closed`() {
        val match = manifest(pattern = "^(5\\.").match(facts())
        assertEquals(PayloadManifest.Compatibility.UNSUPPORTED, match.compatibility)
        assertFalse(match.canRun())
    }

    @Test
    fun `a row with an empty device matches any codename`() {
        val row = PayloadManifest.DeviceRow(
            "SM-A528B",
            "",
            "5.4.254-qgki-28575149-abA528BXXSBGYI3",
            "A528BXXSBGYI3",
            34,
            "verified",
        )
        val match = manifest(devices = listOf(row)).match(facts(device = "unknown"))
        assertEquals(PayloadManifest.Compatibility.VERIFIED, match.compatibility)
    }
}
