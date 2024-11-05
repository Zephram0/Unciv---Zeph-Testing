package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.city.City
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.IConstruction
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.stats.Stat

object BuildingEvaluator {
    fun determineBuildingToPurchase(city: City, personality: Personality): IConstruction? {
        val availableBuildings = city.cityConstructions.getConstructableBuildings()
        return availableBuildings.maxByOrNull { calculateBuildingValue(it, city, personality) }
    }

    fun calculateBuildingValue(building: Building, city: City, personality: Personality): Int {
        var value = 0
        
        // Base stats value
        val stats = building.getStats(city)
        for (stat in Stat.values()) {
            var statValue = (stats.getStatValue(stat) * 10).toInt()
            
            // Apply victory focus modifiers
            if (city.civ.wantsToFocusOn(stat)) {
                statValue *= 2
            }
            
            value += statValue
        }
        
        // Consider maintenance cost
        value -= building.maintenance * 10
        
        // Consider special cases with personality modifiers
        if (building.isWonder) {
            value = (value * (1.5f + personality.wonder * 0.1f)).toInt()
        }
        if (building.uniqueObjects.isNotEmpty()) {
            value = (value * 1.5f).toInt()
        }
        
        return value
    }
}