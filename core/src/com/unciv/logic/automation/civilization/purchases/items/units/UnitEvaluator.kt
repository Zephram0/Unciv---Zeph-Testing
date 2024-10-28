package com.unciv.logic.automation.civilization.purchases.items.units

import com.unciv.logic.city.City
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.models.ruleset.unit.Unit
import com.unciv.models.stats.Personality

object UnitEvaluator {

    /**
     * Determines which unit to purchase based on the city's needs and AI's personality.
     *
     * @param city The city considering the purchase.
     * @param personality The AI personality influencing decision-making.
     * @return The unit to purchase or null if no suitable unit is found.
     */
    fun determineUnitToPurchase(city: City, personality: Personality): Unit? {
        // Get available units that can be purchased
        val availableUnits = city.getAvailableUnitsToConstruct().filter {
            city.cityConstructions.isConstructionPurchaseAllowed(it, com.unciv.models.ruleset.Stat.Gold)
        }

        // Evaluate each unit and sort by perceived value
        val prioritizedUnits = availableUnits.sortedByDescending { unit ->
            evaluateUnitValue(unit, personality)
        }

        // Return the top unit to purchase, if any
        return prioritizedUnits.firstOrNull()
    }

    /**
     * Evaluates the value of a unit based on the AI's personality and strategic needs.
     *
     * @param unit The unit to evaluate.
     * @param personality The AI personality influencing decision-making.
     * @return An integer representing the unit's perceived value.
     */
    private fun evaluateUnitValue(unit: Unit, personality: Personality): Int {
        // Example: Prioritize military units if the personality favors military
        val baseValue = unit.baseCombatStrength + unit.baseRangedStrength
        return baseValue * personality.military
    }
}
