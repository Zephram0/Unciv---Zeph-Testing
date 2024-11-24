package com.unciv.logic.automation.civilization

import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption.PurchaseType
import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
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

        // Initialize purchasing strategies
        val purchasingStrategies = listOf<IPurchasingStrategy>(
            ConstructionStrategy,
            UnitStrategy,
            TileStrategy,
            CityStateStrategy
        )


        //println("=== Purchase Evaluation for ${civ.civName} ===")
        //println("Available Gold: ${civ.gold}")
        //println("Gold per Turn: ${civ.stats.statsForNextTurn.gold}")

        //TODO: For each strategy, print the highest and lowest value options

        // Collect and filter purchase options based on available gold
        val allPurchaseOptions = purchasingStrategies.flatMap { strategy ->
            val options = strategy.evaluatePurchases(civ, personality)
            //println("\n${strategy.javaClass.simpleName} evaluated ${options.size} options:")
            options.forEach { option ->
                //println("  [${option.type}] ${option.description}")
                //println("    Value/Cost: ${option.baseValue}/${option.cost} = ${option.baseValue.toFloat()/option.cost}")
            }
            options
        }.filter { it.cost <= civ.gold }

        //println("\nFiltered to ${allPurchaseOptions.size} affordable options")
        
        // TODO:Sort by perceived value in descending order for allPurchaseOptions
        // Select and execute the best purchase
        selectBestPurchase(allPurchaseOptions, civ, personality)?.let { selectedOption ->
            //println("\nSelected purchase: ${selectedOption.description}")
            //println("  Cost: ${selectedOption.cost}")
            //println("  Value: ${selectedOption.baseValue}")
            
            // Execute purchase
            if (selectedOption.cost <= civ.gold) {
                try {
                    selectedOption.action.invoke()
                    //println("Purchase successful!")
                } catch (e: Exception) {
                    //println("Purchase failed: ${e.message}")
                }
            } else {
                //println("Purchase cancelled: Insufficient gold")
            }
        } //?: println("\nNo suitable purchases found")
        
        //println("=== End of Purchase Evaluation ===\n")
    }

    /**
     * Selects the best purchase option based on final scores.
     */
    private fun selectBestPurchase(
        options: List<PurchaseOption>,
        civ: Civilization,
        personality: Personality
    ): PurchaseOption? {
        return options
            .filter { meetsGoldThreshold(it, civ) }
            .maxByOrNull { 
                calculateFinalScore(it, civ, personality)
            }
    }

    /**
     * Calculates the final score for a purchase option by applying personality, victory focus, and situational modifiers.
     */
    private fun calculateFinalScore(
        option: PurchaseOption,
        civ: Civilization,
        personality: Personality
    ): Float {
        val baseScore = option.baseValue / option.cost

        // Apply personality modifiers
        val personalityMultiplier = when (option.type) {
            PurchaseType.Construction -> when {
                civ.wantsToFocusOn(Victory.Focus.Science) -> 1.2f
                civ.wantsToFocusOn(Victory.Focus.Culture) -> 1.1f
                else -> 1f
            }
            PurchaseType.UnitUpgrade -> when {
                civ.wantsToFocusOn(Victory.Focus.Military) -> 1.3f
                personality.military > 6 -> 1.2f
                personality.military > 4 -> 1.1f
                else -> 0.9f
            }
            PurchaseType.CityState -> when {
                civ.wantsToFocusOn(Victory.Focus.CityStates) -> 1.4f
                personality.diplomacy > 6 -> 1.2f
                else -> 1f
            }
            PurchaseType.Tile -> when {
                civ.wantsToFocusOn(Victory.Focus.Culture) -> 1.3f
                personality.expansion > 6 -> 1.2f
                personality.expansion > 4 -> 1.1f
                else -> 1f
            }
        }

        // Apply situational modifiers
        val situationalMultiplier = when {
            civ.isAtWar() && (option.type == PurchaseType.UnitUpgrade || option.type == PurchaseType.Construction) -> 1.3f
            civ.gold < 0 && option.type == PurchaseType.Construction -> 0.7f  // Be more conservative when losing money
            else -> 1f
        }

        return baseScore * personalityMultiplier * situationalMultiplier
    }

    /**
     * Determines if the purchase meets the gold threshold based on current civilization state.
     */
    private fun meetsGoldThreshold(option: PurchaseOption, civ: Civilization): Boolean {
        val minimumReserve = when {
            civ.isAtWar() -> 100
            civ.gold < 0 -> 500
            else -> (civ.gold * 0.2f).coerceAtMost(250f).toInt() // More dynamic reserve
        }
        val willPurchase = option.cost <= (civ.gold - minimumReserve)
        if (!willPurchase) {
            //println("Rejected purchase of ${option.description}: cost ${option.cost}, " +
            //        "gold ${civ.gold}, minimum reserve $minimumReserve")
        }
        return willPurchase
    }
}
