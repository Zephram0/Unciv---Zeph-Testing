package com.unciv.logic.automation.civilization.purchases.evaluators

// Standard imports for city, building, and evaluation functionality
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
import com.unciv.logic.automation.Automation
import com.unciv.logic.automation.city.ConstructionAutomation
import com.unciv.models.ruleset.unique.LocalUniqueCache

/**
 * Evaluates buildings for potential purchase by the AI.
 * Works in conjunction with ConstructionStrategy to determine optimal building purchases.
 */
object BuildingEvaluator {
    /**
     * Finds the most valuable building to purchase in a city.
     * Filters buildings by purchase eligibility and ranks them by calculated value.
     */
    fun determineBuildingToPurchase(city: City, personality: Personality): IConstruction? {
        val availableBuildings = city.cityConstructions.getBuildableBuildings()
            .filter { building: Building -> canPurchaseBuilding(building, city) }
        return availableBuildings.maxByOrNull { building: Building -> calculateBuildingValue(building, city, personality) }
    }

    /**
     * Validates if a building can be purchased:
     * - Has valid cost
     * - Is not a wonder
     * - Is allowed to be purchased
     * - City has enough gold
     */
    private fun canPurchaseBuilding(building: Building, city: City): Boolean {
        // Basic validation checks with detailed logging
        if (building.cost < 0) {
            println("${building.name} rejected: negative cost")
            return false
        }
        if (building.isWonder || building.isNationalWonder) {
            println("${building.name} rejected: is wonder")
            return false
        }
        val constructionBuyCost = building.getStatBuyCost(city, Stat.Gold) ?: return false
        if (!city.cityConstructions.isConstructionPurchaseAllowed(building, Stat.Gold, constructionBuyCost)) {
            println("${building.name} rejected: purchase not allowed in ${city.name}")
            return false
        }
        if (constructionBuyCost > city.civ.gold) {
            println("${building.name} rejected: cost $constructionBuyCost exceeds available gold ${city.civ.gold}")
            return false
        }
        println("${building.name} is purchasable in ${city.name} for $constructionBuyCost gold")
        return true
    }

    /**
     * Calculates the strategic value of a building based on multiple factors:
     * 1. Base value from city stat improvements
     * 2. Situational modifiers (food, gold, happiness, culture)
     * 3. Military value during wartime
     * 4. Special building capabilities
     * 5. Victory condition contributions
     */
    fun calculateBuildingValue(building: Building, city: City, personality: Personality): Int {
        var value = 0
        val localUniqueCache = LocalUniqueCache()
        
        // Calculate base value from building stats
        val buildingStats = building.getStats(city, localUniqueCache)
        
        // Calculate base value using personality-scaled stats
        value += (Automation.rankStatsValue(personality.scaleStats(buildingStats, 0.3f), city.civ) * 100).toInt()
        
        // Apply situational modifiers based on city and civilization needs
        for (stat in Stat.values()) {
            when (stat) {
                Stat.Food -> {
                    // Critical priority for food when starving
                    if (city.cityStats.currentCityStats[Stat.Food] < 0) 
                        value = (value * 1.5f).toInt()
                }
                Stat.Gold -> {
                    // Increased priority when treasury is low
                    if (city.civ.stats.statsForNextTurn.gold < 10)
                        value = (value * 1.2f).toInt()
                }
                Stat.Happiness -> {
                    // Significant boost when civilization is unhappy
                    if (city.civ.getHappiness() < 5)
                        value += (building.happiness * 50).toInt()
                }
                Stat.Culture -> {
                    // Priority for culture when borders grow slowly
                    if (city.cityStats.currentCityStats.culture < 2)
                        value = (value * 1.2f).toInt()
                }
                else -> {}
            }
        }
        
        // Enhanced military value for frontier cities during war
        if (city.civ.isAtWar()) {
            var warModifier = 1f
            // Double value for frontier cities
            if (city.civ.getKnownCivs()
                    .mapNotNull { otherCiv -> NextTurnAutomation.getClosestCities(city.civ, otherCiv) }
                    .any { cityDistance -> cityDistance.city1 == city }) {
                warModifier *= 2f
            }
            
            // Add scaled defense values based on personality
            value += (warModifier * building.cityHealth * 10 * 
                personality.inverseModifierFocus(PersonalityValue.Aggressive, 0.3f)).toInt()
            value += (warModifier * building.cityStrength * 15 * 
                personality.inverseModifierFocus(PersonalityValue.Aggressive, 0.3f)).toInt()
        }
        
        // Value special capabilities based on personality
        if (building.hasUnique(UniqueType.EnablesConstructionOfSpaceshipParts)) {
            value += (10 * personality.modifierFocus(PersonalityValue.Science, 0.3f)).toInt()
        }
        
        if (building.hasUnique(UniqueType.EnablesNuclearWeapons) && 
            !city.civ.hasUnique(UniqueType.EnablesNuclearWeapons)) {
            value += (40 * personality.modifierFocus(PersonalityValue.Military, 0.3f)).toInt()
        }
        
        // Triple value for victory-enabling buildings
        if (building.hasUnique(UniqueType.TriggersCulturalVictory) || 
            building.hasUnique(UniqueType.TriggersVictory)) {
            value *= 3
        }
        
        return value
    }
}