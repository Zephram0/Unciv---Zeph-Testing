package com.unciv.logic.automation.civilization.purchases.items.units

import com.unciv.logic.automation.civilization.purchases.decision.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.decision.PurchaseDecisionEngine
import com.unciv.logic.civilization.Civilization
import com.unciv.models.stats.Personality

object UnitPurchasing : IPurchasingStrategy {

    override fun execute(civ: Civilization, personality: Personality) {
        purchaseUnits(civ, personality)
    }

    /**
     * Determines and purchases units based on the AI's personality and strategic needs.
     */
    fun purchaseUnits(civ: Civilization, personality: Personality) {
        for (city in civ.cities) {
            val unitToPurchase = UnitEvaluator.determineUnitToPurchase(city, personality)
            if (unitToPurchase != null) {
                val goldCost = unitToPurchase.getStatBuyCost(city, com.unciv.models.ruleset.Stat.Gold) ?: continue
                if (civ.gold >= goldCost) {
                    val perceivedValue = PurchaseDecisionEngine.calculatePerceivedConstructionValue(unitToPurchase, city, personality)
                    if (PurchaseDecisionEngine.shouldPurchase(perceivedValue, goldCost, civ.gold)) {
                        city.cityConstructions.purchaseUnit(unitToPurchase)
                        civ.gold -= goldCost
                    }
                }
            }
        }
    }
}
