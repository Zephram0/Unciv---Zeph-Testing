package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.city.City
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.Victory  // Add this import

object UnitEvaluator {
    fun determineUnitToPurchase(city: City, personality: Personality): BaseUnit? {
        // TODO: Implement proper unit selection logic based on:
        // 1. City's current military needs
        // 2. Civilization's overall strategy
        // 3. Personality traits
        // 4. War status
        val availableUnits = city.cityConstructions.getConstructableUnits()
        return availableUnits.maxByOrNull { calculateUnitValue(it, city, personality) }
    }

    fun calculateUnitValue(unit: BaseUnit, city: City, personality: Personality): Int {
        var value = unit.cost // Base value from production cost

        // Military focus bonus
        if (city.civ.wantsToFocusOn(Victory.Focus.Military)) {
            value = (value * 1.5f).toInt()
        }

        // Personality modifiers for military units
        if (unit.isMilitary) {  // Changed from isMilitary() to isMilitary
            value = (value * (1 + (personality.military - 5) * 0.1f)).toInt()
        }

        return value
    }
}