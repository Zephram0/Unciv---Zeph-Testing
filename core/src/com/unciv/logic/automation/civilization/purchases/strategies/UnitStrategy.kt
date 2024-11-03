package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality

object UnitStrategy : IPurchasingStrategy {
    override fun evaluatePurchases(civ: Civilization, personality: Personality): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        for (unit in civ.units.getCivUnits()) {
            val upgradeTo = unit.baseUnit.getUpgradeUnits(civ.gameInfo.ruleset).firstOrNull() ?: continue
            val upgradeCost = unit.baseUnit.upgradeGoldCost(civ) ?: continue

            val perceivedValue = (upgradeTo.strength - unit.baseUnit.strength) * 
                when {
                    unit.getTile().militaryUnit == unit -> 1.5f  // Frontline units more valuable
                    unit.getTile().neighbors.any { it.militaryUnit?.civ?.isAtWarWith(civ) == true } -> 1.2f  // Units near enemies more valuable
                    else -> 1f
                }

            if (PurchaseDecisionEngine.shouldPurchase(perceivedValue.toInt(), upgradeCost, civ.gold)) {
                purchaseOptions.add(
                    PurchaseOption(
                        type = PurchaseOption.PurchaseType.UnitUpgrade,
                        cost = upgradeCost,
                        baseValue = perceivedValue,
                        description = "Upgrade ${unit.baseUnit.name} to ${upgradeTo.name}",
                        action = { unit.upgradeUnit(upgradeTo, true) }
                    )
                )
            }
        }

        return purchaseOptions
    }
}