package com.unciv.logic.automation.civilization.purchases.decision

import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.INonPerpetualConstruction
import com.unciv.models.ruleset.tile.TileImprovement
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.Construction
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.stats.Stat
import com.unciv.logic.automation.civilization.purchases.ValueCalculator
import com.unciv.logic.automation.civilization.purchases.PurchaseDecider

object PurchaseDecisionEngine {

    /**
     * Calculates the perceived value of a construction (building or unit) based on the AI's personality.
     */
    fun calculatePerceivedConstructionValue(construction: Construction, city: City, personality: Personality): Int {
        return ValueCalculator.calculatePerceivedConstructionValue(construction, city, personality)
    }

    /**
     * Calculates the perceived value of a tile based on its yields and the AI's personality.
     */
    fun calculatePerceivedTileValue(tile: Tile, personality: Personality): Int {
        return ValueCalculator.calculatePerceivedTileValue(tile, personality)
    }

    /**
     * Adjusts the perceived value based on the civilization's victory focus.
     */
    fun adjustForVictoryFocus(value: Int, focus: Victory.Focus, personality: Personality): Int {
        return PurchaseDecider.adjustForVictoryFocus(value, focus, personality)
    }

    /**
     * Determines whether the AI should make a purchase based on perceived value, cost, and available gold.
     */
    fun shouldPurchase(perceivedValue: Int, goldCost: Int, goldAvailable: Int): Boolean {
        return PurchaseDecider.shouldPurchase(perceivedValue, goldCost, goldAvailable)
    }

    /**
     * Determines whether the AI should buy a specific tile.
     */
    fun shouldBuyTile(tile: Tile, city: City, personality: Personality, civInfo: Civilization): Boolean {
        val perceivedValue = calculatePerceivedTileValue(tile, personality)
        val adjustedValue = adjustForVictoryFocus(perceivedValue, civInfo.victoryFocus, personality)
        return shouldPurchase(adjustedValue, city.expansion.getGoldCostOfTile(tile), civInfo.gold)
    }

    /**
     * Determines if a new tile is better than the worst currently worked tile in the city.
     */
    fun isTileBetterThanCurrent(city: City, newTile: Tile, personality: Personality, civ: Civilization): Boolean {
        val currentTiles = city.workingTiles
        val worstTile = currentTiles.minByOrNull { calculatePerceivedTileValue(it, personality) } ?: return false
        val perceivedValue = if (hasAvailableWorkers(civ, newTile) && newTile.canBeImproved()) {
            maxOfPerceivedValuesOfPossibleImprovements(newTile, personality, civ)
        } else {
            calculatePerceivedTileValue(newTile, personality)
        }
        return perceivedValue > calculatePerceivedTileValue(worstTile, personality)
    }

    /**
     * Calculates the maximum perceived value of a tile with all possible improvements.
     */
    fun maxOfPerceivedValuesOfPossibleImprovements(tile: Tile, personality: Personality, civ: Civilization): Int {
        val possibleImprovements = getPossibleImprovements(tile, civ)
        val perceivedValues = possibleImprovements.map { improvement ->
            val canClear = tile.feature?.clearingTech?.let { civ.tech.hasResearched(it) } ?: true
            if (canClear) {
                ValueCalculator.calculatePerceivedValueWithImprovement(tile, improvement, personality)
            } else {
                0
            }
        }
        return perceivedValues.maxOrNull() ?: calculatePerceivedTileValue(tile, personality)
    }

    /**
     * Retrieves a list of tile improvements that the civilization can build on a given tile.
     */
    private fun getPossibleImprovements(tile: Tile, civ: Civilization): List<TileImprovement> {
        return civ.gameInfo.ruleSet.tileImprovements.values.filter { improvement ->
            (improvement.uniqueTo == null || civ.matchesFilter(improvement.uniqueTo!!)) &&
            civ.hasRequiredTech(improvement) &&
            tile.improvementFunctions.canBuildImprovement(improvement, civ)
        }
    }

    /**
     * Checks if there are available workers to improve a tile.
     */
    private fun hasAvailableWorkers(civ: Civilization, tile: Tile): Boolean {
        val requiresWorker = tile.resource != null && tile.improvement == null
        if (!requiresWorker) return true
        val idleWorkers = civ.units.getCivUnits().count { it.isWorker && it.isIdle() }
        return idleWorkers > 0
    }
}
