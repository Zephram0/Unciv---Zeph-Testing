package com.unciv.logic.automation.civilization.purchases.core

import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats

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
        // Don't spend more than 80% of available gold
        if (goldCost > goldAvailable * 0.8f) return false
        
        // Base value/cost ratio threshold
        var requiredRatio = when {
            goldAvailable > 1000 -> 0.4f  // Rich civs can be more liberal
            goldAvailable > 500 -> 0.6f   // Moderate threshold
            else -> 1.0f                  // Conservative when poor
        }
        
        // Adjust ratio based on gold income
        val goldPerTurn = city.civ.stats.statsForNextTurn.gold
        requiredRatio *= when {
            goldPerTurn < 0 -> 1.5f  // More conservative when losing money
            goldPerTurn > 20 -> 0.7f // Very liberal with high income
            goldPerTurn > 10 -> 0.8f // More liberal with good income
            goldPerTurn > 5 -> 0.9f  // Slightly liberal when profitable
            else -> 1.0f
        }
        
        val actualRatio = perceivedValue.toFloat() / goldCost
        
        println("Purchase evaluation:")
        println("- Value/Cost ratio: $actualRatio (required: $requiredRatio)")
        println("- Gold available: $goldAvailable")
    
        return actualRatio >= requiredRatio
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
        
        // Base value from yields using Stats class
        val tileStats = tile.stats.getTileStats(null)  // null for observingCiv means civ-agnostic stats
        
        // Use Stats' built-in iteration
        for ((stat, statValue) in tileStats) {
            value += (statValue * 10).toInt()
        }
        
        // Resource value
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
