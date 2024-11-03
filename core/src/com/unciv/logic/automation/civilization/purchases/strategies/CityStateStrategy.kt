package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.CivInfo
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality

object CityStateStrategy : IPurchasingStrategy {

    override fun evaluatePurchases(civ: Civilization, personality: Personality, victoryType: Victory.Type): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        if (!civ.isMajorCiv()) return purchaseOptions

        if (civ.gold < 250) return purchaseOptions

        for (cityState in civ.getKnownCivs().filter { 
            it.isCityState() && 
            MotivationToAttackAutomation.hasAtLeastMotivationToAttack(civ, it, 0f) <= 0 
        }) {
            val currentInfluence = cityState.getDiplomacyManager(civ)?.getInfluence() ?: continue
            if (currentInfluence >= 60) continue

            // Calculate perceived value
            val perceivedValue = NextTurnAutomation.valueCityStateAlliance(civ, cityState, true)
            val adjustedValue = PurchaseDecisionEngine.adjustForVictoryFocus(perceivedValue, victoryType, personality)

            // Determine if purchase is warranted
            if (PurchaseDecisionEngine.shouldPurchase(adjustedValue, 500, civ.gold)) {
                purchaseOptions.add(
                    PurchaseOption(
                        type = PurchaseOption.PurchaseType.CityState,
                        cost = 500,
                        baseValue = adjustedValue.toFloat(),
                        description = "Provide gold gift to ${cityState.civName}",
                        action = { cityState.cityStateFunctions.receiveGoldGift(civ, 500) }
                    )
                )
            }
        }

        return purchaseOptions
    }
}