package com.unciv.logic.automation.civilization.purchases.items.buildings

import com.unciv.logic.automation.civilization.purchases.decision.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.decision.PurchaseDecisionEngine
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality

object BuildingPurchasing : IPurchasingStrategy {

    override fun execute(civ: Civilization, personality: Personality) {
        purchaseBuildings(civ, personality)
    }

    /**
     * Determines and purchases buildings based on the AI's personality and city needs.
     */
    fun purchaseBuildings(civ: Civilization, personality: Personality) {
        for (city in civ.cities) {
            val buildingToPurchase = BuildingEvaluator.determineBuildingToPurchase(city, personality)
            if (buildingToPurchase != null) {
                val goldCost = buildingToPurchase.getStatBuyCost(city, com.unciv.models.ruleset.Stat.Gold) ?: continue
                if (civ.gold >= goldCost) {
                    val perceivedValue = PurchaseDecisionEngine.calculatePerceivedConstructionValue(buildingToPurchase, city, personality)
                    if (PurchaseDecisionEngine.shouldPurchase(perceivedValue, goldCost, civ.gold)) {
                        city.cityConstructions.purchaseBuilding(buildingToPurchase)
                        civ.gold -= goldCost
                    }
                }
            }
        }
    }
}
