package dev.lanyard.payload

import android.app.Application
import dev.lanyard.payload.engine.RunEngine

class LanyardApp : Application() {
    val runEngine: RunEngine by lazy { RunEngine(this) }
}
