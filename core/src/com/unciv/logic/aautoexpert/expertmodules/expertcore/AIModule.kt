package com.unciv.logic.aautoexpert.expertmodules.expertcore

import com.unciv.logic.civilization.Civilization

/**
 * Base interface for all AI modules.
 * Defines the contract for processing AI decisions.
 */
interface AIModule {
    /**
     * Processes AI decisions based on the current state of the civilization.
     *
     * @param civInfo The civilization being controlled by the AI.
     */
    fun processDecisions(civInfo: Civilization)
}