package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.MotivationToAttackAutomation
import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.diplomacy.DiplomaticStatus
import com.unciv.logic.civilization.diplomacy.RelationshipLevel
import com.unciv.models.ruleset.nation.Personality
import com.unciv.logic.automation.civilization.purchases.debug.PurchaseDebugger

object CityStateStrategy : IPurchasingStrategy {
    override fun evaluatePurchases(
        civ: Civilization, 
        personality: Personality
    ): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()
        val debugger = PurchaseDebugger

        debugger.appendLine("\nEvaluating city-state purchases for ${civ.civName}:")

        if (!civ.isMajorCiv()) return purchaseOptions

        if (civ.gold < 250) {
            debugger.appendLine("  Rejected: Insufficient gold (${civ.gold} < 250)")
            return purchaseOptions
        }

        for (cityState in civ.getKnownCivs().filter { it.isCityState }) {
            debugger.appendLine("\n  Evaluating ${cityState.civName}:")

            val motivationToAttack = MotivationToAttackAutomation.hasAtLeastMotivationToAttack(civ, cityState, 0f)
            if (motivationToAttack > 0) {
                debugger.appendLine("    Rejected: Has motivation to attack ($motivationToAttack)")
                continue
            }

            val diplomacyManager = cityState.getDiplomacyManager(civ)
            val currentInfluence = diplomacyManager?.getInfluence()
            debugger.appendLine("    Current influence: $currentInfluence")
            if (currentInfluence == null) {
                debugger.appendLine("    Rejected: No diplomacy manager")
                continue
            }
            if (diplomacyManager.isRelationshipLevelEQ(RelationshipLevel.Ally)) {
                debugger.appendLine("    Rejected: Already allied with ${cityState.civName}")
                continue
            }

            val perceivedValue = NextTurnAutomation.valueCityStateAlliance(civ, cityState, true)
            debugger.appendLine("    Alliance value: $perceivedValue")
            if (perceivedValue <= 0) {
                debugger.appendLine("    Rejected: Alliance not valuable")
                continue
            }
            
            // Determine if purchase is warranted based on gold cost vs. perceived value
            if (PurchaseDecisionEngine.shouldPurchase(perceivedValue, 500, civ.gold, civ)) {
                purchaseOptions.add(
                    PurchaseOption(
                        type = PurchaseOption.PurchaseType.CityState,
                        cost = 500,
                        baseValue = perceivedValue.toFloat(),  // Convert Int to Float
                        description = "Provide gold gift to ${cityState.civName}",
                        action = { cityState.cityStateFunctions.receiveGoldGift(civ, 500) }
                    )
                )
                debugger.appendLine("    Added to purchase options")
            } else {
                debugger.appendLine("    Rejected by PurchaseDecisionEngine")
            }
        }
        // Example of logging purchase options
        if (purchaseOptions.isNotEmpty()) {
            debugger.appendLine("\nCity-State Purchase Options:")
            val bestOption = purchaseOptions.maxByOrNull { it.baseValue / it.cost }
            val worstOption = purchaseOptions.minByOrNull { it.baseValue / it.cost }
            
            bestOption?.let {
                debugger.appendLine("  Best: ${it.description}")
                debugger.appendLine("    Value/Cost: ${it.baseValue}/${it.cost} = ${it.baseValue / it.cost}")
            }
            worstOption?.let {
                debugger.appendLine("  Worst: ${it.description}")
                debugger.appendLine("    Value/Cost: ${it.baseValue}/${it.cost} = ${it.baseValue / it.cost}")
            }
        }

        return purchaseOptions
    }
}