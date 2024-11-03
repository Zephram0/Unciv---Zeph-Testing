package com.unciv.logic.automation.civilization

import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption.PurchaseType
import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality
import com.unciv.logic.automation.civilization.purchases.strategies.BuildingStrategy
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
            BuildingStrategy,
            UnitStrategy,
            TileStrategy,
            CityStateStrategy
        )

        // Collect all possible purchases from each strategy
        val allPurchaseOptions = purchasingStrategies.flatMap { strategy ->
            strategy.evaluatePurchases(civ, personality)
        }

        // Select and execute the best purchase
        selectBestPurchase(allPurchaseOptions, civ, personality)?.let { selectedOption ->
            try {
                selectedOption.action.invoke()
            } catch (e: Exception) {
                println("Failed to execute purchase: ${selectedOption.description}")
            }
        }
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
            civ.gold < 0 -> 500  // Save more when losing money
            else -> 250
        }
        return option.cost <= (civ.gold - minimumReserve)
    }
}
