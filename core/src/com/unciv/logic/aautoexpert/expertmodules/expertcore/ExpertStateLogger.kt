package com.unciv.logic.aautoexpert.expertmodules.expertcore

import com.unciv.utils.Log
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.GameInfo

/**
 * Simple logging utility for tracking AI decisions and debugging settler placement.
 * Focuses on essential logging for MVP implementation.
 * 
 * Usage:
 * - First configure the logger with the current game state: configureForTurn(gameInfo)
 * - Then use the logging functions throughout the AI turn
 */
object ExpertStateLogger {
    private const val TAG = "AAutoExpert"
    private var isEnabled = true
    private var verboseMode = false
    private var currentTurn: Int = -1

    /**
     * Configures the logger for the current AI turn
     * Should be called at the start of each AI turn
     */
    fun configureForTurn(gameInfo: GameInfo) {
        currentTurn = gameInfo.turns
    }

    private fun log(category: String, message: String, isError: Boolean = false, exception: Exception? = null) {
        if (!isEnabled && !isError) return
        if (!verboseMode && category == "City Placement") return

        val turnInfo = if (currentTurn >= 0) "Turn $currentTurn" else "Turn unknown"
        
        val formattedMessage = buildString {
            append("$TAG [$turnInfo] [$category] $message")
            if (exception != null) append("\nStack trace: ${exception.stackTraceToString()}")
        }

        if (isError) {
            Log.error(formattedMessage)
            println("ERROR: $formattedMessage")
        } else {
            Log.debug(formattedMessage)
            println("DEBUG: $formattedMessage")
        }
    }

    fun logDecision(message: String) {
        log("Decision", message)
    }

    fun logSettler(unit: MapUnit, action: String, context: String? = null) {
        val message = buildString {
            append("${unit.id} at ${unit.getTile().position}")
            append("\nAction: $action")
            if (context != null) append("\nContext: $context")
        }
        log("Settler", message)
    }

    fun logCityPlacement(tile: Tile, score: Float) {
        val message = buildString {
            append("${tile.position}")
            append("\nScore: $score")
            append("\nResources: ${tile.tileResource}")
            append("\nTerrain: ${tile.baseTerrain}")
        }
        log("City Placement", message)
    }

    fun logMilitary(unit: MapUnit, action: String) {
        val message = buildString {
            append("${unit.id} at ${unit.getTile().position}")
            append("\nAction: $action")
        }
        log("Military", message)
    }

    fun logError(message: String, e: Exception? = null) {
        log("Error", message, isError = true, exception = e)
    }

    fun logAlert(message: String) {
        log("Alert", message)
    }

    /**
     * Configures the logger's behavior
     * @param enable Whether to show non-error logs
     * @param verbose Whether to show detailed logs like city placement scores
     */
    fun configure(enable: Boolean = true, verbose: Boolean = false) {
        isEnabled = enable
        verboseMode = verbose
        log("Config", "Logger configured - Enabled: $enable, Verbose: $verbose")
    }
}