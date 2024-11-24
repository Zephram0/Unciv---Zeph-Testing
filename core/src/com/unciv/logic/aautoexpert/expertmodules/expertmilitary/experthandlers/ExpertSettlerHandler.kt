package com.unciv.logic.aautoexpert.expertmodules.expertmilitary.experthandlers

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger
import com.unciv.logic.aautoexpert.expertutils.ExpertMovementHelper
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.Ruleset
import com.unciv.logic.aautoexpert.rulevalidator_patch.ValidRawOutputValidator

/**
 * Handles settler unit movement and city founding decisions.
 */
class ExpertSettlerHandler(private val ruleset: Ruleset) { // Ensure ruleset is injected or accessible
    /**
     * Main handler for settler units. Evaluates potential settlement locations and moves/settles accordingly.
     * 
     * @param unit The settler unit to process
     */
    fun handleSettler(unit: MapUnit) {
        if (unit.currentMovement <= 0) return

        ExpertStateLogger.logDecision("Processing settler ${unit.name}")

        val settlementLocation = findSettlementLocation(unit)
        if (settlementLocation != null) {
            if (settlementLocation == unit.getTile()) {
                // If at target location, found city if valid
                if (ValidRawOutputValidator.UnitCommands.foundCity(unit)) {
                    ExpertStateLogger.logDecision("Founded city at ${settlementLocation.position}")
                }
            } else {
                // Move towards target location safely
                ExpertMovementHelper.moveTowardsSafely(unit, settlementLocation)
                ExpertStateLogger.logDecision("Moving settler towards ${settlementLocation.position}")
            }
        }
    }

    /**
     * Finds the optimal settlement location within search radius.
     * 
     * @param unit The settler unit
     * @return The best tile to settle on, or null if none found
     */
    private fun findSettlementLocation(unit: MapUnit): Tile? {
        val searchRadius = 5
        val currentTile = unit.getTile()
        
        return currentTile.getTilesInDistance(searchRadius)
            .filter { isValidSettleLocation(it) }
            .maxByOrNull { evaluateTile(it) }
    }

    /**
     * Evaluates a tile's suitability for city placement.
     * Considers terrain, features, and resources.
     * 
     * @param tile The tile to evaluate
     * @return A score representing the tile's value for settlement
     */
    private fun evaluateTile(tile: Tile): Float {
        var score = 0f

        // Basic terrain scoring
        score += when {
            tile.isHill() -> 2f            // Hills provide defense and production
            tile.terrainFeatures.contains("Forest") -> 1.5f  // Forests provide production
            tile.baseTerrain == "Grassland" -> 1f   // Grassland provides food
            else -> 0.5f
        }

        // Additional features
        if (tile.isAdjacentToRiver()) score += 1f  // Rivers provide gold and growth

        // Resource scoring
        score += scoreResources(tile)

        return score
    }

    /**
     * Checks if a tile is valid for city settlement.
     * Ensures minimum city spacing and terrain requirements.
     * 
     * @param tile The tile to check
     * @return True if the tile is valid for settlement
     */
    private fun isValidSettleLocation(tile: Tile): Boolean {
        if (tile.isWater || tile.isImpassible()) return false

        // Ensure minimum city distance
        if (tile.getTilesInDistance(3).any { it.isCityCenter() }) return false

        return true
    }

    /**
     * Scores a tile based on nearby resources.
     * Prioritizes strategic > luxury > bonus resources.
     * 
     * @param tile The tile to evaluate resources for
     * @return Resource-based score component
     */
    private fun scoreResources(tile: Tile): Float {
        var score = 0f

        val resourceName = tile.resource

        if (resourceName != null) {
            val tileResource = ruleset.tileResources[resourceName]
            if (tileResource != null) {
                score += when (tileResource.resourceType) {
                    ResourceType.Strategic -> 15f
                    ResourceType.Luxury -> 10f
                    ResourceType.Bonus -> 5f
                }
            } else {
                ExpertStateLogger.logError("Tile at ${tile.position} has undefined resource '$resourceName'.")
            }
        }

        return score
    }
}