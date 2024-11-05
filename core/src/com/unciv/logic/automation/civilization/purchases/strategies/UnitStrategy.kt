package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.nation.PersonalityValue
import com.unciv.models.ruleset.unique.StateForConditionals
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine

object UnitStrategy : IPurchasingStrategy {
    override fun evaluatePurchases(civ: Civilization, personality: Personality): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        for (unit in civ.units.getCivUnits()) {
            val stateForConditionals = StateForConditionals(civInfo = civ, unit = unit)
            val upgradeTo = unit.baseUnit.getRulesetUpgradeUnits(stateForConditionals).firstOrNull() ?: continue
            
            val upgradeCost = unit.upgrade.getCostOfUpgrade(upgradeTo)
            if (upgradeCost == 0) continue

            val strategicMultiplier = when {
                unit.getTile().militaryUnit == unit -> 1.5f
                unit.getTile().neighbors.any { it.militaryUnit?.civ?.isAtWarWith(civ) == true } -> 1.2f
                unit.health < 100 -> 0.8f
                unit.getTile().isWater && unit.baseUnit.isRanged() -> 1.3f
                unit.getTile().neighbors.any { it.isCityCenter() && it.getCity()?.civ?.isAtWarWith(civ) == true } -> 1.4f
                else -> 1f
            }

            // Calculate perceived value based on unit stats difference and strategic value
            val statsDifference = (upgradeTo.strength - unit.baseUnit.strength) + 
                                (upgradeTo.rangedStrength - unit.baseUnit.rangedStrength)
            
            // Use personality's aggressive and military values for warmonger preference
            val warPreference = (personality[PersonalityValue.Aggressive] + personality[PersonalityValue.Military]) / 10f
            val perceivedValue = statsDifference * strategicMultiplier * (1f + warPreference)

            if (PurchaseDecisionEngine.shouldPurchase(perceivedValue.toInt(), upgradeCost, civ.gold)) {
                purchaseOptions.add(
                    PurchaseOption(
                        type = PurchaseOption.PurchaseType.UnitUpgrade,
                        cost = upgradeCost,
                        baseValue = perceivedValue,
                        description = "Upgrade ${unit.baseUnit.name} to ${upgradeTo.name}",
                        action = { unit.upgrade.performUpgrade(upgradeTo, false, upgradeCost) }
                    )
                )
            }
        }

        return purchaseOptions
    }
}