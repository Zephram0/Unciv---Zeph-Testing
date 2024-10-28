package com.unciv.logic.automation.civilization

import com.unciv.models.stats.Stat
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.constructions.INonPerpetualConstruction
import com.unciv.logic.automation.civilization.purchases.decision.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.influence.CityStateRelations
import com.unciv.logic.automation.civilization.purchases.items.buildings.BuildingPurchasing
import com.unciv.logic.automation.civilization.purchases.items.units.UnitPurchasing
import com.unciv.logic.automation.civilization.purchases.tiles.TilePurchasing
import com.unciv.logic.automation.civilization.purchases.unit.UnitEnhancements
import com.unciv.logic.civilization.Civilization

object UseGoldAutomation {

    /**
     * Allows the AI to spend gold on purchasing buildings, units, upgrading units,
     * influencing city-states, and buying city tiles, based on its personality and current state.
     */
    fun useGold(civ: Civilization) {
        // Get the personality traits of the civilization
        val personality = civ.getPersonality()

        // Purchase buildings in cities, prioritizing those with longer construction times
        for (city in civ.cities.sortedByDescending {
            // Sort cities by the number of turns left to complete the current construction
            it.cityConstructions.turnsToConstruction(it.cityConstructions.currentConstructionFromQueue)
        }) {
            val construction = city.cityConstructions.getCurrentConstruction() ?: continue
            // Skip if the construction is a perpetual construction (e.g., Wealth, Research)
            if (construction !is INonPerpetualConstruction) continue
            // Get the gold cost to buy the construction immediately
            val statBuyCost = construction.getStatBuyCost(city, Stat.Gold) ?: continue
            // Check if the construction can be purchased with gold
            if (!city.cityConstructions.isConstructionPurchaseAllowed(construction, Stat.Gold, statBuyCost)) continue
            if (civ.gold < statBuyCost / 1.1) continue

            // Calculate the perceived value of the construction based on the AI's personality
            val perceivedValue = PurchaseDecisionEngine.calculatePerceivedConstructionValue(construction, city, personality)
            // Decide whether to purchase the construction based on its perceived value and cost
            if (PurchaseDecisionEngine.shouldPurchase(perceivedValue, statBuyCost, civ.gold)) {
                // Purchase the construction
                city.cityConstructions.purchaseConstruction(construction, 0, true)
            }
        }

        // Upgrade military units if the civilization has a militaristic personality
        val unitsCopy = civ.units.getCivUnits().toList()
        for (unit in unitsCopy) {
            if (personality.military > 5)
                UnitEnhancements.tryUpgradeUnit(unit)
        }

        // Spend gold on influencing city-states if the civilization is a major civ
        if (civ.isMajorCiv())
            CityStateRelations.useGoldForCityStates(civ, personality)

        // Purchase city tiles based on the civilization's personality
        TilePurchasing.execute(civ, personality)

        // Spend on city items like buildings or units
        BuildingPurchasing.execute(civ, personality)
        UnitPurchasing.execute(civ, personality)
    }
}
