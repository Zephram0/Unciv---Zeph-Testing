package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.city.City
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.IConstruction
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.unique.StateForConditionals
import com.unciv.models.stats.Stats

object ValueCalculator {
    fun calculatePerceivedConstructionValue(construction: IConstruction, city: City, personality: Personality): Int {
        return when (construction) {
            is Building -> calculateBuildingValue(construction, city, personality)
            else -> calculateDefaultConstructionValue(construction, city, personality)
        }
    }

    fun calculatePerceivedTileValue(tile: Tile, personality: Personality): Int {
        var value = 0
        tile.stats.forEach { stat, amount -> 
            value += (amount * 10).toInt()
        }
        
        // Add strategic value for resources
        if (tile.resource != null) {
            value += when (tile.tileResource.resourceType) {
                ResourceType.Strategic -> 50
                ResourceType.Luxury -> 40
                else -> 20
            }
        }
        
        return value
    }

    private fun calculateBuildingValue(building: Building, city: City, personality: Personality): Int {
        var value = 0
        
        // Base stats value
        val stats = building.getStats(city)
        stats.forEach { stat, amount ->
            value += (amount * 10).toInt()
        }
        
        // Consider maintenance cost
        value -= building.maintenance * 10
        
        // Consider special cases
        if (building.isWonder) value *= 2
        if (building.uniqueObjects.isNotEmpty()) value *= 1.5f.toInt()
        
        return value
    }

    private fun calculateDefaultConstructionValue(construction: IConstruction, city: City, personality: Personality): Int {
        // Basic evaluation for non-Building constructions
        return 10  // Default minimum value
    }
} 