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
            value = (value * 1.2f).toInt()
        }
        
        return value
    }

    private fun calculateBaseValue(tile: Tile, civ: Civilization): Int {
        var value = 0
        
        // Calculate value from tile stats
        for (stat in Stat.values()) {
            val statValue = when (stat) {
                Stat.Production -> tile.stats.getTileStats(civ)[Stat.Production]
                Stat.Food -> tile.stats.getTileStats(civ)[Stat.Food]
                Stat.Gold -> tile.stats.getTileStats(civ)[Stat.Gold]
                Stat.Science -> tile.stats.getTileStats(civ)[Stat.Science]
                Stat.Culture -> tile.stats.getTileStats(civ)[Stat.Culture]
                Stat.Happiness -> tile.stats.getTileStats(civ)[Stat.Happiness]
                Stat.Faith -> tile.stats.getTileStats(civ)[Stat.Faith]
            } * 10f
    
            if (civ.wantsToFocusOn(stat)) {
                value += (statValue * 2f).toInt()
            } else {
                value += statValue.toInt()
            }
        }
        
        return value
    }

    // Extension function to check if a tile has a resource
    private fun Tile.hasResource(): Boolean = resource != null
}