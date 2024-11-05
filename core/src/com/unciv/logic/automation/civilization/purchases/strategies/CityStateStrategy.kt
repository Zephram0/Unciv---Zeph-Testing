package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.MotivationToAttackAutomation
import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality

object CityStateStrategy : IPurchasingStrategy {
    override fun evaluatePurchases(
        civ: Civilization, 
        personality: Personality
    ): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        if (!civ.isMajorCiv()) return purchaseOptions
        if (civ.gold < 250) return purchaseOptions

        for (cityState in civ.getKnownCivs().filter { 
            it.isCityState && 
            MotivationToAttackAutomation.hasAtLeastMotivationToAttack(civ, it, 0f) <= 0 
        }) {
            val currentInfluence = cityState.getDiplomacyManager(civ)?.getInfluence() ?: continue
            if (currentInfluence >= 60) continue

            // Calculate perceived value using NextTurnAutomation's city state valuation
            val perceivedValue = NextTurnAutomation.valueCityStateAlliance(civ, cityState, true)
            if (perceivedValue <= 0) continue  // Skip if the alliance isn't valuable to us
            
            // Determine if purchase is warranted based on gold cost vs. perceived value
            if (PurchaseDecisionEngine.shouldPurchase(perceivedValue, 500, civ.gold)) {
                purchaseOptions.add(
                    PurchaseOption(
                        type = PurchaseOption.PurchaseType.CityState,
                        cost = 500,
                        baseValue = perceivedValue.toFloat(),  // Convert Int to Float
                        description = "Provide gold gift to ${cityState.civName}",
                        action = { cityState.cityStateFunctions.receiveGoldGift(civ, 500) }
                    )
                )
            }
        }

        return purchaseOptions
    }
}