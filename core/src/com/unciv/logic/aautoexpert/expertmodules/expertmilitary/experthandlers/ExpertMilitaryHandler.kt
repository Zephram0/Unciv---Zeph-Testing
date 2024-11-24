package com.unciv.logic.aautoexpert.expertmodules.expertmilitary.experthandlers

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger
import com.unciv.logic.aautoexpert.expertutils.ExpertMovementHelper
import com.unciv.models.ruleset.unique.UniqueType

/**
 * Handles military unit movement and combat decisions.
 * Currently focuses on protecting settlers and maintaining defensive positions.
 */
class ExpertMilitaryHandler {
    /**
     * Main handler for military units. Prioritizes protecting settlers, then takes defensive positions.
     *
     * @param unit The military unit to process
     */
    fun handleMilitary(unit: MapUnit) {
        // Skip if unit has no movement points left
        if (unit.currentMovement <= 0) return

        ExpertStateLogger.logDecision("Processing military unit ${unit.id}")

        // Look for nearby settlers to protect
        val nearestSettler = findNearestSettler(unit)
        if (nearestSettler != null) {
            protectSettler(unit, nearestSettler)
            return
        }

        // If no settlers to protect, maintain defensive position
        if (!unit.isFortified() && unit.currentMovement.toInt() == unit.getMaxMovement()) {
            unit.fortify()
            ExpertStateLogger.logDecision("Fortifying unit at ${unit.getTile().position}")
        }
    }

    /**
     * Moves military unit to protect a settler.
     *
     * @param unit The military unit providing protection
     * @param settler The settler unit to protect
     */
    private fun protectSettler(unit: MapUnit, settler: MapUnit) {
        ExpertStateLogger.logDecision("Protecting settler at ${settler.getTile().position}")
        ExpertMovementHelper.moveTowardsSafely(unit, settler.getTile())
    }

    /**
     * Finds the nearest settler unit belonging to the same civilization.
     *
     * @param unit The military unit searching for settlers
     * @return The nearest settler unit, or null if none found within search radius
     */
    private fun findNearestSettler(unit: MapUnit): MapUnit? {
        val searchRadius = 5
        return unit.getTile().getTilesInDistance(searchRadius)
            .flatMap { it.getUnits() }
            .filter { it.civ == unit.civ && it.baseUnit.getMatchingUniques(UniqueType.FoundCity).any() }
            .minByOrNull { it.getTile().aerialDistanceTo(unit.getTile()) }
    }
}