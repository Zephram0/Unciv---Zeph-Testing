package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.stats.Stat

object BuildingStrategy : IPurchasingStrategy {
    override fun evaluatePurchases(civ: Civilization, personality: Personality): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        for (city in civ.cities) {
            val constructableBuildings = city.cityConstructions.getConstructableBuildings()
                .filter { building -> 
                    building.goldCost > 0 && 
                    city.cityConstructions.canPurchaseBuilding(building) 
                }
            
            val buildingToPurchase = constructableBuildings
                .maxByOrNull { it.goldCost }
                ?: continue

            val goldCost = buildingToPurchase.goldCost
            if (civ.gold < goldCost) continue

            val perceivedValue = ValueCalculator.calculatePerceivedConstructionValue(buildingToPurchase, city, personality)

            if (PurchaseDecisionEngine.shouldPurchase(perceivedValue, goldCost, civ.gold)) {
                purchaseOptions.add(
                    PurchaseOption(
                        type = PurchaseType.Construction,
                        cost = goldCost,
                        baseValue = perceivedValue.toFloat(),
                        description = "Purchase ${buildingToPurchase.name}",
                        action = { 
                            city.cityConstructions.purchaseBuilding(buildingToPurchase.name)
                        }
                    )
                )
            }
        }

        return purchaseOptions
    }
}