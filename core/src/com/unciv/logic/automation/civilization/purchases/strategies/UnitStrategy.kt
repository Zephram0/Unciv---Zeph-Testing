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

            // Calculate strategic value multiplier based on unit position and status
            val strategicMultiplier = when {
                unit.getTile().militaryUnit == unit -> 1.5f  // Frontline units
                unit.getTile().neighbors.any { it.militaryUnit?.civ?.isAtWarWith(civ) == true } -> 1.2f  // Near enemies
                unit.health < 100 -> 0.8f  // Damaged units less valuable to upgrade
                unit.getTile().isWater && unit.baseUnit.isRanged() -> 1.3f  // Naval ranged units
                unit.getTile().neighbors.any { it.isCityCenter() && it.getCity()?.civ?.isAtWarWith(civ) == true } -> 1.4f // Near enemy cities
                else -> 1f
            }

            // Calculate perceived value based on unit stats difference and strategic value
            val statsDifference = (upgradeTo.strength - unit.baseUnit.strength) + 
                                (upgradeTo.rangedStrength - unit.baseUnit.rangedStrength)
            
            val perceivedValue = statsDifference * strategicMultiplier * 
                (1f + personality.preferredVictoryType.getWarmongerPreference() * 0.2f)

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