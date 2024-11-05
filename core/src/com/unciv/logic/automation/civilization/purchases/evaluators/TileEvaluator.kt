package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.map.tile.Tile
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats
import com.unciv.models.ruleset.Victory

object TileEvaluator {
    fun rankTile(tile: Tile, civ: Civilization, personality: Personality): Int {
        var value = calculateBaseValue(tile, civ)
        
        // Apply strategic modifiers
        value = (value * when {
            tile.naturalWonder != null -> 2f
            tile.hasResource() -> when (tile.tileResource.resourceType) {
                ResourceType.Strategic -> if (civ.wantsToFocusOn(Victory.Focus.Military)) 1.5f else 1.3f
                ResourceType.Luxury -> if (civ.getHappiness() < 0) 1.4f else 1.2f
                else -> 1.1f
            }
            else -> 1.0f
        }).toInt()

        // Apply personality modifiers
         if (personality.expansion > 6) {
            value = ((value * 1.2f).toInt())
        }
        
        return value
    }

    private fun calculateBaseValue(tile: Tile, civ: Civilization): Int {
        var value = 0
        
        // Calculate value from tile stats
        for (stat in Stat.values()) {
            val statValueFloat = tile.stats.getStatValue(stat) * 10
            var statValue = statValueFloat.toInt()
            if (civ.wantsToFocusOn(stat)) {
                statValue *= 2
            }
            value += statValue
        }
        
        return value
    }
    
    // Extension function to get stat value
    private fun Stats.getStatValue(stat: Stat): Float {
        return when (stat) {
            Stat.Production -> production
            Stat.Food -> food
            Stat.Gold -> gold
            Stat.Science -> science
            Stat.Culture -> culture
            Stat.Happiness -> happiness
            Stat.Faith -> faith
        }
    }

    // Extension function to check if a tile has a resource
    private fun Tile.hasResource(): Boolean = resource != null

}

