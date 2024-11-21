package com.unciv.logic.aautoexpert

import com.unciv.logic.civilization.Civilization
import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.ui.screens.worldscreen.WorldScreen
import kotlinx.coroutines.Job
import com.unciv.utils.Concurrency
import com.unciv.logic.aautoexpert.modules.military.MilitaryModule

/*
 * Minimum viable Auto Expert AI system.
 * Initially handles basic unit automation and city management
 * while providing hooks for future expansion.
 */
object AAutoExpert {

    // Mirror AutoPlay's state management
    var turnsToAutoPlay: Int = 0
    var autoPlayTurnInProgress: Boolean = false
    var autoPlayJob: Job? = null

    fun startTurn(worldScreen: WorldScreen) {
        val civInfo = worldScreen.viewingCiv
        
        // Similar to AutoPlayMenu's pattern of checking unit count
        if (civInfo.units.getCivUnitsSize() + civInfo.cities.size >= 30) {
            runAutoPlayJobInNewThread("AAutoExpertTurn", worldScreen, false) {
                executeTurn(civInfo)
            }
        } else {
            autoPlayTurnInProgress = true
            executeTurn(civInfo)
            autoPlayTurnInProgress = false
        }
    }

    private val militaryModule = MilitaryModule()
    
    /*
     * Main entry point for the Auto Expert AI system.
     * Currently implements basic automation similar to AutoPlay
     * but with hooks for future enhancement.
     *
     * @param civInfo The civilization being controlled
     */
    private fun executeTurn(civInfo: Civilization) {
        if (civInfo.isDefeated() || civInfo.isSpectator()) return
    
        // Process military decisions first
        militaryModule.processDecisions(civInfo)
        
        // Fall back to basic automation for other aspects for now
        NextTurnAutomation.automateCivMoves(civInfo)
    }

    private fun runAutoPlayJobInNewThread(
        jobName: String,
        worldScreen: WorldScreen,
        setPlayerTurnAfterEnd: Boolean = true,
        job: () -> Unit
    ) {
        autoPlayJob = Concurrency.runOnNonDaemonThreadPool(jobName) {
            job()
            autoPlayTurnInProgress = false
            if (setPlayerTurnAfterEnd)
                worldScreen.isPlayersTurn = true
        }
    }

    // Mirror AutoPlay's state check functions
    fun isAutoPlaying(): Boolean = turnsToAutoPlay > 0 || autoPlayTurnInProgress
    fun shouldContinueAutoPlaying(): Boolean = !autoPlayTurnInProgress && turnsToAutoPlay > 0
}