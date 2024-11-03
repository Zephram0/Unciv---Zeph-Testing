package com.unciv.logic.automation.civilization.purchases.core

import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.unique.UniqueType

object PurchaseDecisionEngine {
    fun calculateFinalScore(
        option: PurchaseOption,
        civ: Civilization,
        personality: Personality
    ): Float {
        val baseScore = option.baseValue / option.cost
        
        // Apply personality modifiers
        val personalityMultiplier = when (option.type) {
            PurchaseOption.PurchaseType.Construction -> when {
                civ.wantsToFocusOn(Victory.Focus.Science) -> 1.2f
                civ.wantsToFocusOn(Victory.Focus.Culture) -> 1.1f
                else -> 1.0f
            }
            PurchaseOption.PurchaseType.UnitUpgrade -> when {
                civ.wantsToFocusOn(Victory.Focus.Military) -> 1.3f
                personality.military > 6 -> 1.2f
                else -> 1.0f
            }
            PurchaseOption.PurchaseType.CityState -> when {
                civ.wantsToFocusOn(Victory.Focus.CityStates) -> 1.4f
                personality.diplomacy > 6 -> 1.2f
                else -> 1.0f
            }
            PurchaseOption.PurchaseType.Tile -> when {
                civ.wantsToFocusOn(Victory.Focus.Culture) -> 1.3f
                personality.expansion > 6 -> 1.2f
                else -> 1.0f
            }
        }

        return baseScore * personalityMultiplier
    }

    fun shouldPurchase(perceivedValue: Int, goldCost: Int, goldAvailable: Int): Boolean {
        return goldCost <= goldAvailable * 0.8f && // Don't spend more than 80% of available gold
                perceivedValue / goldCost.toFloat() >= 1.5f // Minimum value/cost ratio
    }

    fun isTileBetterThanCurrent(city: City, newTile: Tile, personality: Personality): Boolean {
        val currentTiles = city.workedTiles.map { city.tileMap[it] }
        if (currentTiles.isEmpty()) return true
        
        val worstTile = currentTiles.minByOrNull { 
            calculateTileValue(it) 
        } ?: return false
            
        return calculateTileValue(newTile) > calculateTileValue(worstTile)
    }

    private fun calculateTileValue(tile: Tile): Int {
        var value = 0
        
        // Base value from yields
        tile.stats.stats.values.forEach { value += (it * 10).toInt() }
        
        // Resource value - can use tile.tileResource directly if resource exists
        if (tile.resource != null) {
            value += when (tile.tileResource.resourceType) {
                ResourceType.Strategic -> 50
                ResourceType.Luxury -> 40
                else -> 20
            }
        }
        
        return value
    }
}
