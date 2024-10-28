package com.unciv.logic.automation.civilization.purchases.influence

import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.Personality
import com.unciv.logic.stats.Stat
import com.unciv.models.ruleset.Victory

object InfluenceEvaluator {

    /**
     * Determines whether the AI should invest in influencing a city-state.
     *
     * @param civ The civilization performing the influence.
     * @param cityState The city-state under consideration.
     * @param personality The AI personality influencing decision-making.
     * @return True if the AI should invest in the city-state; otherwise, false.
     */
    fun shouldInvestInCityState(civ: Civilization, cityState: Civilization, personality: Personality): Boolean {
        // Example logic: Invest if the city-state provides a stat that aligns with the victory focus
        val victoryFocus = civ.victoryFocus
        return when (victoryFocus) {
            Victory.Focus.Culture -> cityState.cityStateFunctions.canProvideStat(Stat.Culture)
            Victory.Focus.Science -> cityState.cityStateFunctions.canProvideStat(Stat.Science)
            Victory.Focus.Military -> cityState.cityStateFunctions.canProvideStat(Stat.Military)
            else -> false
        }
    }

    /**
     * Determines the amount of gold to spend on influencing the city-state.
     *
     * @param civ The civilization performing the influence.
     * @param cityState The city-state under consideration.
     * @return The amount of gold to spend.
     */
    fun determineGoldToSpend(civ: Civilization, cityState: Civilization): Int {
        val diploManager = cityState.getDiplomacyManager(civ) ?: return 0
        return when {
            diploManager.getInfluence() >= 20 && civ.gold >= 500 -> 500
            civ.gold >= 250 -> 250
            else -> 0
        }
    }
}
