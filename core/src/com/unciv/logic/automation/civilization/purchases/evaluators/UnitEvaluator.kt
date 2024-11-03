package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.city.City
import com.unciv.models.ruleset.nation.Personality

object UnitEvaluator {

    fun determineUnitToPurchase(city: City, personality: Personality): Unit? {
        // Implement logic to determine the best unit to purchase based on city needs and personality
        // Example placeholder logic:
        return city.getNextPriorityUnit(personality)
    }
}
