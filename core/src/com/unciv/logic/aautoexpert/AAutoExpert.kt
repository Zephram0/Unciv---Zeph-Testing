package com.unciv.logic.aautoexpert

import com.unciv.logic.civilization.Civilization
import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.ui.screens.worldscreen.WorldScreen
import kotlinx.coroutines.Job
import com.unciv.utils.Concurrency
import com.unciv.logic.aautoexpert.expertmodules.expertmilitary.ExpertMilitaryModule
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger
import com.unciv.logic.aautoexpert.expertmodules.expertcore.AIModule
import com.unciv.logic.GameInfo

/**
 * Main coordinator for the Auto Expert AI system.
 * Handles turn processing and module coordination while providing
 * error handling and logging.
 * 
 * The execution flow is:
 * 1. Configure logger at initialization
 * 2. Start turn with configureForTurn to set current turn number
 * 3. Process AI modules sequentially
 * 4. Fall back to basic automation for unhandled aspects
 */
object AAutoExpert {
    // State management for auto-play functionality
    var turnsToAutoPlay: Int = 0
    var autoPlayTurnInProgress: Boolean = false
    var autoPlayJob: Job? = null

    init {
        // Configure logger when AAutoExpert is first accessed
        // This only needs to happen once, not per turn
        ExpertStateLogger.configure(enable = true, verbose = false)
    }

    /**
     * Creates the list of AI modules to be executed each turn
     * New modules should be added to this list as they're implemented
     */
    private fun createModules(gameInfo: GameInfo): List<AIModule> = listOf(
        ExpertMilitaryModule(gameInfo)
        // Add other modules here as they're implemented
    )

    /**
     * Initiates AI turn processing for a civilization
     * Handles threading for large civilizations and ensures proper logging setup
     *
     * @param worldScreen The current world screen instance
     */
    fun startTurn(worldScreen: WorldScreen) {
        val civInfo = worldScreen.viewingCiv
        
        // Configure logger with current game state to enable turn numbering in logs
        ExpertStateLogger.configureForTurn(civInfo.gameInfo)
        ExpertStateLogger.logDecision("Starting turn for ${civInfo.civName}")

        try {
            // Initialize modules with current game info
            val modules = createModules(civInfo.gameInfo)

            // Use threading for larger civilizations to prevent UI freezing
            if (civInfo.units.getCivUnitsSize() + civInfo.cities.size >= 30) {
                runAutoPlayJobInNewThread("AAutoExpertTurn", worldScreen, false) {
                    executeTurn(civInfo, modules)
                }
            } else {
                autoPlayTurnInProgress = true
                executeTurn(civInfo, modules)
                autoPlayTurnInProgress = false
            }
        } catch (e: Exception) {
            ExpertStateLogger.logError("Error during turn execution", e)
            autoPlayTurnInProgress = false
            throw e
        }
    }

    /**
     * Executes a single turn for the given civilization
     * Processes each AI module in sequence, then falls back to basic automation
     */
    private fun executeTurn(civInfo: Civilization, modules: List<AIModule>) {
        if (civInfo.isDefeated() || civInfo.isSpectator()) {
            ExpertStateLogger.logDecision("Skipping turn - civ is defeated or spectating")
            return
        }

        try {
            // Process each AI module in sequence
            for (module in modules) {
                ExpertStateLogger.logDecision("Processing module: ${module.javaClass.simpleName}")
                module.processDecisions(civInfo)
            }

            // Fall back to basic automation for unhandled aspects
            ExpertStateLogger.logDecision("Running fallback automation")
            NextTurnAutomation.automateCivMoves(civInfo)

        } catch (e: Exception) {
            ExpertStateLogger.logError("Error in module execution", e)
            throw e
        }
    }

    /**
     * Runs the auto-play job in a separate thread for large civilizations
     * Ensures proper cleanup of state even if the job fails
     */
    private fun runAutoPlayJobInNewThread(
        jobName: String,
        worldScreen: WorldScreen,
        setPlayerTurnAfterEnd: Boolean = true,
        job: () -> Unit
    ) {
        autoPlayJob = Concurrency.runOnNonDaemonThreadPool(jobName) {
            try {
                job()
            } catch (e: Exception) {
                ExpertStateLogger.logError("Thread error in $jobName", e)
                throw e
            } finally {
                autoPlayTurnInProgress = false
                if (setPlayerTurnAfterEnd) {
                    worldScreen.isPlayersTurn = true
                }
            }
        }
    }

    /**
     * Cancels any ongoing auto-play operations and cleans up state
     */
    fun cancelAutoPlay() {
        ExpertStateLogger.logDecision("Cancelling auto-play")
        turnsToAutoPlay = 0
        autoPlayJob?.cancel()
        autoPlayJob = null
        autoPlayTurnInProgress = false
    }

    // State check functions
    fun isAutoPlaying(): Boolean = turnsToAutoPlay > 0 || autoPlayTurnInProgress
    
    fun shouldContinueAutoPlaying(): Boolean = !autoPlayTurnInProgress && turnsToAutoPlay > 0
}