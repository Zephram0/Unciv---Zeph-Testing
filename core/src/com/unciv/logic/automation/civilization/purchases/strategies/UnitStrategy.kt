package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality

object UnitStrategy : IPurchasingStrategy {

    override fun evaluatePurchases(civ: Civilization, personality: Personality, victoryType: Victory.Type): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        for (unit in civ.units.getCivUnits()) {
            val upgradeTo = unit.getUpgradeUnit() ?: continue
            val upgradeCost = unit.getUpgradeCost() ?: continue

            val perceivedValue = (upgradeTo.baseUnit.strength - unit.baseUnit.strength) * 
                when {
                    unit.getTile().militaryUnit == unit -> 1.5f  // Frontline units more valuable
                    unit.isNearEnemy() -> 1.2f  // Units near enemies more valuable
                    else -> 1f
                }

            val adjustedValue = PurchaseDecisionEngine.adjustForVictoryFocus(perceivedValue.toInt(), victoryType, personality)

            if (PurchaseDecisionEngine.shouldPurchase(adjustedValue, upgradeCost, civ.gold)) {
                purchaseOptions.add(
                    PurchaseOption(
                        type = PurchaseOption.PurchaseType.UnitUpgrade,
                        cost = upgradeCost,
                        baseValue = adjustedValue.toFloat(),
                        description = "Upgrade ${unit.baseUnit.name} to ${upgradeTo.baseUnit.name}",
                        action = { unit.upgrade() }
                    )
                )
            }
        }

        return purchaseOptions
    }
}