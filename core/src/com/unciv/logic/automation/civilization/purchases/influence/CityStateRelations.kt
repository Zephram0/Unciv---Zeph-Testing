package com.unciv.logic.automation.civilization.purchases.influence

import com.unciv.logic.automation.civilization.purchases.decision.IPurchasingStrategy
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality

object CityStateRelations : IPurchasingStrategy {

    override fun execute(civ: Civilization, personality: Personality) {
        useGoldForCityStates(civ, personality)
    }

    /**
     * Allows the AI to spend gold on influencing city-states based on its personality.
     */
    fun useGoldForCityStates(civ: Civilization, personality: Personality) {
        val knownCityStates = civ.getKnownCivs().filter {
            it.isCityState && !civ.isAtWarWith(it)
        }.toList()

        for (cityState in knownCityStates) {
            if (InfluenceEvaluator.shouldInvestInCityState(civ, cityState, personality)) {
                val goldToSpend = InfluenceEvaluator.determineGoldToSpend(civ, cityState)
                if (goldToSpend > 0 && civ.gold >= goldToSpend) {
                    cityState.cityStateFunctions.receiveGoldGift(civ, goldToSpend)
                    civ.adjustGold(-goldToSpend) // Use a method to adjust gold instead of direct assignment
                }
            }
        }
    }
}
