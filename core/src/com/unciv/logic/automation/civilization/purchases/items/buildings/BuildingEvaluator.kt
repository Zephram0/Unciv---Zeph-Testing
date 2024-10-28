package com.unciv.logic.automation.civilization.purchases.items.buildings

import com.unciv.logic.city.City
import com.unciv.models.ruleset.building.Building
import com.unciv.models.ruleset.nation.Personality

object BuildingEvaluator {

    /**
     * Determines which building to purchase based on the city's needs and AI's personality.
     *
     * @param city The city considering the purchase.
     * @param personality The AI personality influencing decision-making.
     * @return The building to purchase or null if no suitable building is found.
     */
    fun determineBuildingToPurchase(city: City, personality: Personality): Building? {
        // Get available buildings that can be purchased
        val availableBuildings = city.getAvailableBuildingsToConstruct().filter {
            city.cityConstructions.isConstructionPurchaseAllowed(it, com.unciv.models.ruleset.Stat.Gold)
        }

        // Evaluate each building and sort by perceived value
        val prioritizedBuildings = availableBuildings.sortedByDescending { building ->
            evaluateBuildingValue(building, city, personality)
        }

        // Return the top building to purchase, if any
        return prioritizedBuildings.firstOrNull()
    }

    /**
     * Evaluates the value of a building based on the AI's personality and city needs.
     *
     * @param building The building to evaluate.
     * @param city The city considering the purchase.
     * @param personality The AI personality influencing decision-making.
     * @return An integer representing the building's perceived value.
     */
    private fun evaluateBuildingValue(building: Building, city: City, personality: Personality): Int {
        // Calculate the perceived value based on building yields and personality weights
        val yields = building.yields
        return yields.food * personality.food +
               yields.production * personality.production +
               yields.gold * personality.gold +
               yields.science * personality.science +
               yields.culture * personality.culture +
               yields.faith * personality.faith +
               yields.happiness * personality.happiness
    }
}
