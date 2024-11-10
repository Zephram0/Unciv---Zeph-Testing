package com.unciv.logic.automation.civilization.purchases.core

import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats

object PurchaseDecisionEngine {
    fun calculateFinalScore(
        option: PurchaseOption,
        civ: Civilization,
        personality: Personality
    ): Float {
        val baseScore = option.baseValue / option.cost

        // Apply personality modifiers
        val personalityMultiplier = when (option.type) {
            PurchaseOption.PurchaseType.Construction -> when {
                civ.wantsToFocusOn(Victory.Focus.Science) -> 1.2f
                civ.wantsToFocusOn(Victory.Focus.Culture) -> 1.1f
                else -> 1.0f
            }

            PurchaseOption.PurchaseType.UnitUpgrade -> when {
                civ.wantsToFocusOn(Victory.Focus.Military) -> 1.3f
                personality.military > 6 -> 1.2f
                else -> 1.0f
            }

            PurchaseOption.PurchaseType.CityState -> when {
                civ.wantsToFocusOn(Victory.Focus.CityStates) -> 1.4f
                personality.diplomacy > 6 -> 1.2f
                else -> 1.0f
            }

            PurchaseOption.PurchaseType.Tile -> when {
                civ.wantsToFocusOn(Victory.Focus.Culture) -> 1.3f
                personality.expansion > 6 -> 1.2f
                else -> 1.0f
            }
        }

        return baseScore * personalityMultiplier
    }

    fun shouldPurchase(perceivedValue: Int, goldCost: Int, goldAvailable: Int, civ: Civilization): Boolean {
        // Don't spend more than available gold
        if (goldCost > goldAvailable) return false

        // Base value/cost ratio threshold
        var requiredRatio = when {
            goldAvailable > 2000 -> 0.4f  // Rich civs can be more liberal
            goldAvailable > 1000 -> 0.6f + (0.4f - 0.6f) * ((goldAvailable - 1000) / 1000)  // Moderate threshold, linear from 0.6f to 0.4f
            goldAvailable > 500 -> 0.8f + (0.6f - 0.8f) * ((goldAvailable - 500) / 500f)  // More conservative with low total gold, linear from 0.8f to 0.6f
            else -> 1.0f                  // Conservative when poor
        }

        // Adjust ratio based on gold income
        val goldPerTurn = civ.stats.statsForNextTurn.gold
        requiredRatio *= when {
            goldPerTurn < 0 -> 1.5f  // More conservative when losing money
            goldPerTurn > 100 -> 0.5f // Very liberal with high income
            goldPerTurn > 0 -> 1.0f - (goldPerTurn * 0.005f) // linear consideration from 1.0f at 0 to 0.5f at 100
            else -> 1.0f
        }

        val actualRatio = perceivedValue.toFloat() / goldCost

        return actualRatio >= requiredRatio
    }
}