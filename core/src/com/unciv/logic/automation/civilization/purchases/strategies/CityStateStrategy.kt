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

object CityStateStrategy : IPurchasingStrategy {
    override fun evaluatePurchases(
        civ: Civilization, 
        personality: Personality
    ): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()
        println("\nEvaluating city-state purchases for ${civ.civName}:")

        if (!civ.isMajorCiv()) return purchaseOptions

        if (civ.gold < 250) {
            println("  Rejected: Insufficient gold (${civ.gold} < 250)")
            return purchaseOptions
        }

        for (cityState in civ.getKnownCivs().filter { it.isCityState }) {
            println("\n  Evaluating ${cityState.civName}:")

            val motivationToAttack = MotivationToAttackAutomation.hasAtLeastMotivationToAttack(civ, cityState, 0f)
            if (motivationToAttack > 0) {
                println("    Rejected: Has motivation to attack ($motivationToAttack)")
                continue
            }
    
            val diplomacyManager = cityState.getDiplomacyManager(civ)
            val currentInfluence = diplomacyManager?.getInfluence()
            println("    Current influence: $currentInfluence")
            if (currentInfluence == null) {
                println("    Rejected: No diplomacy manager")
                continue
            }
            if (diplomacyManager.isRelationshipLevelEQ(RelationshipLevel.Ally)) {
                println("    Rejected: Already allied with ${cityState.civName}")
                continue
            }
    
            val perceivedValue = NextTurnAutomation.valueCityStateAlliance(civ, cityState, true)
            println("    Alliance value: $perceivedValue")
            if (perceivedValue <= 0) {
                println("    Rejected: Alliance not valuable")
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
            } else {
                println("    Rejected by PurchaseDecisionEngine")
            }
        }

        // Debug logging before return
        if (purchaseOptions.isNotEmpty()) {
            println("\nCity-State Purchase Options:")
            val bestOption = purchaseOptions.maxByOrNull { it.baseValue / it.cost }
            val worstOption = purchaseOptions.minByOrNull { it.baseValue / it.cost }
            
            bestOption?.let {
                println("  Best: ${it.description}")
                println("    Value/Cost: ${it.baseValue}/${it.cost} = ${it.baseValue.toFloat()/it.cost}")
            }
            worstOption?.let {
                println("  Worst: ${it.description}")
                println("    Value/Cost: ${it.baseValue}/${it.cost} = ${it.baseValue.toFloat()/it.cost}")
            }
            
            println("\nAll City-State Options:")
            purchaseOptions.forEach { option ->
                println("  ${option.description}")
                println("    Value/Cost: ${option.baseValue}/${option.cost} = ${option.baseValue.toFloat()/option.cost}")
            }
        }

        return purchaseOptions
    }
}