package com.unciv.logic.aautoexpert.modules.core

import com.unciv.logic.civilization.Civilization

/**
 * Base interface for all AI modules
 */
interface AIModule {
    fun processDecisions(civInfo: Civilization)
}