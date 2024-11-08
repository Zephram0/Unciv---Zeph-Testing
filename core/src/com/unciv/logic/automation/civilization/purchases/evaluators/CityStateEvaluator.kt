package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.stats.Stat
import com.unciv.models.ruleset.Victory

object CityStateEvaluator {
    fun shouldInvestInCityState(civ: Civilization, cityState: Civilization, personality: Personality): Boolean {
        return calculateCityStateValue(cityState, civ, personality) > 
               determineGoldToSpend(civ, cityState)
    }

    fun calculateCityStateValue(cityState: Civilization, civ: Civilization, personality: Personality): Int {
        var value = 50 // Base value

        // Value based on provided stats
        for (stat in Stat.values()) {
            if (cityState.cityStateFunctions.canProvideStat(stat)) {
                var statValue = 20
                if (civ.wantsToFocusOn(stat)) statValue *= 2
                value += statValue
            }
        }

        // Personality modifiers
        if (personality.diplomacy > 6) value *= 1.3f.toInt()
        
        return value
    }

    fun determineGoldToSpend(civ: Civilization, cityState: Civilization): Int {
        val diploManager = cityState.getDiplomacyManager(civ) ?: return 0
        return when {
            diploManager.getInfluence() >= 20 && civ.gold >= 500 -> 500
            civ.gold >= 250 -> 250
            else -> 0
        }
    }
}