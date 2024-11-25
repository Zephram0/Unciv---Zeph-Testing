package com.unciv.logic.aautoexpert.expertmodules.expertmilitary

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.aautoexpert.expertmodules.expertcore.AIModule
import com.unciv.logic.aautoexpert.expertmodules.expertmilitary.experthandlers.ExpertMilitaryHandler
import com.unciv.logic.aautoexpert.expertmodules.expertmilitary.experthandlers.ExpertSettlerHandler
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.logic.GameInfo
/**
 * Module responsible for handling military and settler unit decisions.
 * Coordinates military unit positioning and settler movement/city founding.
 */
class ExpertMilitaryModule(private val gameInfo: GameInfo) : AIModule {

    // Handlers for specific unit types
    private val militaryHandler = ExpertMilitaryHandler()

    /**
     * Main decision processing function for military and settler units.
     * Processes all military units first to establish defensive positions,
     * then handles settler movements with military support.
     *
     * @param civInfo The civilization whose units are being processed
     */
    override fun processDecisions(civInfo: Civilization) {
        ExpertStateLogger.logDecision("ExpertMilitaryModule: Starting military decisions for ${civInfo.civName}")
        ExpertStateLogger.logDecision("Current turn: ${civInfo.gameInfo.turns}")

        // Log all units at start
        civInfo.units.getCivUnits().forEach { unit ->
            ExpertStateLogger.logDecision("Available unit: ${unit.name} (ID: ${unit.id}) at ${unit.getTile().position}, Movement: ${unit.currentMovement}/${unit.getMaxMovement()}")
        }

        // Create settler handler with the civilization's ruleset
        val settlerHandler = ExpertSettlerHandler(gameInfo.ruleset)

        // Process military units
        civInfo.units.getCivUnits()
            .filter { it.isMilitary() && it.currentMovement > 0 }
            .forEach { militaryHandler.handleMilitary(it) }

        // Process settler units
        civInfo.units.getCivUnits()
            .filter { it.baseUnit.getMatchingUniques(UniqueType.FoundCity).any() && it.currentMovement > 0 }
            .forEach { settlerHandler.handleSettler(it) }

        ExpertStateLogger.logDecision("ExpertMilitaryModule: Completed military decisions for ${civInfo.civName}")
    }
}