package com.unciv.logic.automation.civilization

import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.debug.PurchaseDebugger
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality
import com.unciv.logic.automation.civilization.purchases.strategies.ConstructionStrategy
import com.unciv.logic.automation.civilization.purchases.strategies.UnitStrategy
import com.unciv.logic.automation.civilization.purchases.strategies.TileStrategy
import com.unciv.logic.automation.civilization.purchases.strategies.CityStateStrategy
import com.unciv.models.ruleset.Victory

object UseGoldAutomation {
    /**
     * Orchestrates the AI's gold spending across different purchasing strategies based on personality and victory focus.
     */
    fun useGold(civ: Civilization) {
        if (civ.gold <= 0) return
    
        val personality = civ.getPersonality()
        val debugger = PurchaseDebugger
        
        // Initialize debug session with civilization details
        debugger.startNewSession(
            civName = civ.civName,
            gold = civ.gold,
            goldPerTurn = civ.stats.statsForNextTurn.gold
        )
    
        val purchasingStrategies = listOf<IPurchasingStrategy>(
            ConstructionStrategy,
            UnitStrategy,
            TileStrategy,
            CityStateStrategy
        )
    
        // Collect all purchase options from each strategy and log evaluations
        val allPurchaseOptions = mutableListOf<PurchaseOption>()
        for (strategy in purchasingStrategies) {
            val options = strategy.evaluatePurchases(civ, personality)
            debugger.addStrategyEvaluation(strategy.javaClass.simpleName, options)
            allPurchaseOptions.addAll(options.filter { it.cost <= civ.gold })
        }
    
        // Select the best purchase option based on value/cost ratio
        val selectedPurchase = selectBestPurchase(allPurchaseOptions, civ, personality)
        val finalScore = selectedPurchase?.let { calculateFinalScore(it, civ, personality) }
    
        // Add final decision to debug log
        val reason = if (selectedPurchase == null) {
            "No suitable purchases found within available gold."
        } else {
            "Selected purchase provides the best value for the gold spent."
        }
        debugger.addFinalDecision(selectedPurchase, finalScore, reason)
        
        // Execute the selected purchase if any
        if (selectedPurchase != null) {
            executePurchase(selectedPurchase)
        }
    
        // Output the debug log using the logger
        val log = debugger.getDebugLog()
        println(log) // Or use your preferred logging mechanism
    }

    /**
     * Selects the best purchase option based on the highest value/cost ratio.
     */
    private fun selectBestPurchase(
        purchaseOptions: List<PurchaseOption>,
        civ: Civilization,
        personality: Personality
    ): PurchaseOption? {
        if (purchaseOptions.isEmpty()) return null
        return purchaseOptions.maxByOrNull { calculateFinalScore(it, civ, personality) }
    }

    /**
     * Calculates the final score of a purchase option based on personality and situational multipliers.
     */
    private fun calculateFinalScore(
        option: PurchaseOption,
        civ: Civilization,
        personality: Personality
    ): Float {
        val baseScore = option.baseValue / option.cost

        val personalityMultiplier = when (option.type) {
            PurchaseOption.PurchaseType.Construction -> {
                var multiplier = 1f
                if (civ.wantsToFocusOn(Victory.Focus.Science)) {
                    multiplier += 0.2f
                } else if (civ.wantsToFocusOn(Victory.Focus.Culture)) {
                    multiplier += 0.1f
                }
                multiplier
            }
            PurchaseOption.PurchaseType.UnitUpgrade -> {
                var multiplier = 1f
                if (civ.wantsToFocusOn(Victory.Focus.Military)) {
                    multiplier += 0.3f
                }
                multiplier += (personality.military / 10f) * 0.2f
                multiplier
            }
            PurchaseOption.PurchaseType.CityState -> {
                var multiplier = 1f
                if (civ.wantsToFocusOn(Victory.Focus.CityStates)) {
                    multiplier += 0.4f
                }
                multiplier += (personality.diplomacy / 10f) * 0.2f
                multiplier
            }
            PurchaseOption.PurchaseType.Tile -> {
                var multiplier = 1f
                if (civ.wantsToFocusOn(Victory.Focus.Culture)) {
                    multiplier += 0.3f
                }
                multiplier += (personality.expansion / 10f) * 0.2f
                multiplier
            }
            else -> 1f // Default multiplier for other purchase types
        }

        // Apply situational modifiers
        val situationalMultiplier = when {
            civ.isAtWar() && (option.type == PurchaseOption.PurchaseType.UnitUpgrade || 
                option.type == PurchaseOption.PurchaseType.Construction) -> 1.3f
            civ.gold < 0 && option.type == PurchaseOption.PurchaseType.Construction -> 0.7f
            else -> 1f
        }

        return baseScore * personalityMultiplier * situationalMultiplier
    }

    /**
     * Executes the selected purchase option.
     */
    private fun executePurchase(option: PurchaseOption) {
        try {
            option.action.invoke()
            println("Purchase of '${option.description}' executed successfully.")
        } catch (e: Exception) {
            println("Purchase failed: ${e.message}")
        }
    }
}

