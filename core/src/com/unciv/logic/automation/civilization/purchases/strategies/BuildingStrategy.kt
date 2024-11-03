package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality

object BuildingStrategy : IPurchasingStrategy {

    override fun evaluatePurchases(civ: Civilization, personality: Personality, victoryFocus: Victory.Focus): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        for (city in civ.cities) {
            val buildingToPurchase = BuildingEvaluator.determineBuildingToPurchase(city, personality)
            if (buildingToPurchase != null) {
                val goldCost = buildingToPurchase.getStatBuyCost(city, Stat.Gold) ?: continue
                if (civ.gold < goldCost) continue

                val perceivedValue = PurchaseDecisionEngine.calculatePerceivedConstructionValue(buildingToPurchase, city, personality)
                val adjustedValue = PurchaseDecisionEngine.adjustForVictoryFocus(perceivedValue, victoryFocus, personality)

                if (PurchaseDecisionEngine.shouldPurchase(adjustedValue, goldCost, civ.gold)) {
                    purchaseOptions.add(
                        PurchaseOption(
                            type = PurchaseOption.PurchaseType.Construction,
                            cost = goldCost,
                            baseValue = adjustedValue.toFloat(),
                            description = "Purchase ${buildingToPurchase.name}",
                            action = { city.cityConstructions.purchaseBuilding(buildingToPurchase) }
                        )
                    )
                }
            }
        }

        return purchaseOptions
    }
}