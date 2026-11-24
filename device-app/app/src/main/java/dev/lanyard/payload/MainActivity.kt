package dev.lanyard.payload

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.lanyard.payload.ui.AppShell
import dev.lanyard.payload.ui.runner.RunnerViewModel
import dev.lanyard.payload.ui.theme.LanyardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RunnerViewModel by viewModels {
        RunnerViewModel.factory((application as LanyardApp).runEngine)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LanyardTheme {
                AppShell(
                    viewModel = viewModel,
                    onCopyTrigger = ::copyTrigger,
                    onCopyDeviceFacts = ::copyDeviceFacts,
                )
            }
        }
    }

    private fun copyDeviceFacts(facts: String) {
        copyToClipboard("Lanyard device facts", facts)
        Toast.makeText(this, getString(R.string.copied_device_facts), Toast.LENGTH_SHORT).show()
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
    }

    private fun copyTrigger() {
        val command = "adb shell setprop ctl.start zzz"
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Lanyard init trigger", command))
        Toast.makeText(this, getString(R.string.copied_trigger, command), Toast.LENGTH_LONG).show()
    }
}
