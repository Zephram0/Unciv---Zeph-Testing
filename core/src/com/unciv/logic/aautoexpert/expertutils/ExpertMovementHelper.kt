package com.unciv.logic.aautoexpert.expertutils

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.logic.map.TileMap
import com.unciv.logic.battle.Battle
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger

/**
 * Provides safe movement utilities for AI units.
 */
object ExpertMovementHelper {

    /**
     * Moves a unit towards a target tile safely, ensuring no threats are encountered.
     *
     * @param unit The unit to move.
     * @param target The target tile to move towards.
     */
    fun moveTowardsSafely(unit: MapUnit, target: Tile) {
        if (unit.currentMovement <= 0) return

        val currentTile = unit.getTile()
        if (currentTile == target) return

        // Get next tile in path
        val path = unit.movement.getShortestPath(target)
        val nextTile = path.firstOrNull() ?: return

        // Check if movement is safe
        if (!isMovementSafe(unit, nextTile)) {
            ExpertStateLogger.logError("Unsafe movement detected for ${unit.name} to ${nextTile.position}")
            return
        }

        // Move to next tile
        unit.movement.moveToTile(nextTile)
        ExpertStateLogger.logDecision("Safely moved ${unit.name} to ${nextTile.position}")
    }

    /**
     * Determines if moving a unit to a specific tile is safe.
     *
     * @param unit The unit attempting to move.
     * @param tile The tile to move to.
     * @return True if movement is safe, false otherwise.
     */
    fun isMovementSafe(unit: MapUnit, tile: Tile): Boolean {
        // Basic safety checks
        if (tile.isImpassible()) return false

        // Check for enemy units
        val enemyUnits = tile.getTilesInDistance(1)
            .flatMap { it.getUnits() }
            .filter { it.civ != unit.civ }
            .toList()

        // Settlers should avoid any enemy units
        if (unit.baseUnit.getMatchingUniques(UniqueType.FoundCity).any() && enemyUnits.any()) {
            return false
        }

        // Military units can handle some threats
        if (!unit.baseUnit.getMatchingUniques(UniqueType.FoundCity).any()) {
            val dangerousEnemies = enemyUnits.filter { 
                it.baseUnit.strength > unit.baseUnit.strength 
            }
            if (dangerousEnemies.any()) return false
        }

        return true
    }
}