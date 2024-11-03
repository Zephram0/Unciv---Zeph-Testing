package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.city.City
import com.unciv.models.ruleset.nation.Personality

object BuildingEvaluator {

    fun determineBuildingToPurchase(city: City, personality: Personality): IConstruction? {
        // Implement logic to determine the best building to purchase based on city needs and personality
        // Example placeholder logic:
        return city.getNextPriorityBuilding(personality)
    }
}
