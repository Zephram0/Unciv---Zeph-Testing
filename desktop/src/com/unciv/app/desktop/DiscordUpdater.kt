package com.unciv.app.desktop

import net.arikia.dev.drpc.DiscordEventHandlers
import net.arikia.dev.drpc.DiscordRPC
import net.arikia.dev.drpc.DiscordRichPresence
import com.unciv.utils.debug
import java.util.Timer
import kotlin.concurrent.timer

class DiscordGameInfo(
    var gameNation: String = "",
    var gameLeader: String = "",
    var gameTurn: Int = 0
)

class DiscordUpdater {
    private var onUpdate: (() -> DiscordGameInfo?)? = null
    private var updateTimer: Timer? = null

    fun setOnUpdate(callback: () -> DiscordGameInfo?) {
        onUpdate = callback
    }

    fun startUpdates() {
        try {
            val handlers = DiscordEventHandlers.Builder().build()
            DiscordRPC.discordInitialize("647066573147996161", handlers, true)

            Runtime.getRuntime().addShutdownHook(Thread { DiscordRPC.discordShutdown() })

            updateTimer = timer(name = "Discord", daemon = true, period = 1000) {
                try {
                    updateRpc()
                } catch (ex: Exception) {
                    debug("Exception while updating Discord Rich Presence", ex)
                }
            }
        } catch (_: Throwable) {
            debug("Could not initialize Discord")
        }
    }

    fun stopUpdates() {
        updateTimer?.cancel()
        try {
            DiscordRPC.discordShutdown()
        } catch (_: Throwable) {
            debug("Error shutting down Discord RPC")
        }
    }

    private fun updateRpc() {
        if (onUpdate == null)
            return

        val info = onUpdate!!.invoke() ?: return

        val presence = DiscordRichPresence()
        presence.details = "${info.gameLeader} of ${info.gameNation}"
        presence.state = "Turn ${info.gameTurn}"
        presence.largeImageKey = "logo"
        presence.largeImageText = "Unciv"

        DiscordRPC.discordUpdatePresence(presence)
    }
}