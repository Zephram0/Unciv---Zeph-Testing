package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.city.City
import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.logic.city.CityConstructions
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.IConstruction
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.nation.PersonalityValue
import com.unciv.models.ruleset.unique.StateForConditionals
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats

object BuildingEvaluator {
    fun determineBuildingToPurchase(city: City, personality: Personality): IConstruction? {
        val availableBuildings = city.cityConstructions.getBuildableBuildings()
            .filter { building: Building -> canPurchaseBuilding(building, city) }
        return availableBuildings.maxByOrNull { building: Building -> calculateBuildingValue(building, city, personality) }
    }

    private fun canPurchaseBuilding(building: Building, city: City): Boolean {
        if (building.cost < 0) return false  // Can't be purchased
        if (building.isWonder || building.isNationalWonder) return false  // Wonders can't be purchased
        val constructionBuyCost = building.getStatBuyCost(city, Stat.Gold) ?: return false
        if (!city.cityConstructions.isConstructionPurchaseAllowed(building, Stat.Gold, constructionBuyCost)) return false
        if (constructionBuyCost > city.civ.gold) return false
        return true
    }

    fun calculateBuildingValue(building: Building, city: City, personality: Personality): Int {
        var value = 0
        
        // Base stats value with personality scaling
        val stats = building.getStats(city)
        val scaledStats = personality.scaleStats(stats.clone(), 0.5f)
        
        // Apply situational modifiers from ConstructionAutomation
        for (stat in Stat.values()) {
            var statValue = (scaledStats[stat] * 10).toInt()
            
            // Situational modifiers
            when (stat) {
                Stat.Food -> {
                    val surplusFood = city.cityStats.currentCityStats[Stat.Food]
                    if (surplusFood < 0) statValue *= 8 // Starving
                    else statValue *= 3
                }
                Stat.Gold -> {
                    if (city.civ.stats.statsForNextTurn.gold < 10) 
                        statValue *= 2 // Gold problems
                }
                Stat.Happiness -> {
                    if (city.civ.getHappiness() < 10 || 
                        city.civ.getHappiness() < city.civ.cities.size)
                        statValue *= 5
                }
                Stat.Culture -> {
                    if (city.cityStats.currentCityStats.culture < 2)
                        statValue *= 2 // Need border growth
                }
                else -> {}
            }
            
            // Victory focus modifiers
            if (city.civ.wantsToFocusOn(stat)) {
                statValue *= 2
            }
            
            value += statValue
        }
        
        // Consider maintenance cost
        value -= building.maintenance * 10
        
        // Military value calculation
        if (city.civ.isAtWar()) {
            var warModifier = 1f
            // Check if city is a frontier city
            if (city.civ.getKnownCivs()
                .mapNotNull { otherCiv -> NextTurnAutomation.getClosestCities(city.civ, otherCiv) }  // Added explicit parameter name
                .any { cityDistance -> cityDistance.city1 == city }) {  // Added explicit parameter name
                warModifier *= 2f
            }
            
            value += (warModifier * building.cityHealth * 10 * 
                personality.inverseModifierFocus(PersonalityValue.Aggressive, 0.3f)).toInt()
            value += (warModifier * building.cityStrength * 15 * 
                personality.inverseModifierFocus(PersonalityValue.Aggressive, 0.3f)).toInt()
        }

        // Special building bonuses
        if (building.hasUnique(UniqueType.EnablesConstructionOfSpaceshipParts)) {
            value += (10 * personality.modifierFocus(PersonalityValue.Science, 0.3f)).toInt()
        }
        
        if (building.hasUnique(UniqueType.EnablesNuclearWeapons) && 
            !city.civ.hasUnique(UniqueType.EnablesNuclearWeapons)) {
            value += (40 * personality.modifierFocus(PersonalityValue.Military, 0.3f)).toInt()
        }

        // Victory-related value
        if (building.hasUnique(UniqueType.TriggersCulturalVictory) || 
            building.hasUnique(UniqueType.TriggersVictory)) {
            value *= 3
        }

        return value
    }
}