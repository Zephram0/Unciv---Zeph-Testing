package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.automation.civilization.purchases.evaluators.BuildingEvaluator
import com.unciv.logic.automation.civilization.purchases.evaluators.UnitEvaluator
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.stats.Stat
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.IConstruction
import com.unciv.logic.city.City
import com.unciv.models.ruleset.unique.StateForConditionals
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.GUI
import com.unciv.models.ruleset.INonPerpetualConstruction
import com.unciv.logic.automation.Automation

/**
 * Strategy for purchasing both buildings and units in cities.
 * Uses BuildingEvaluator and UnitEvaluator to determine optimal purchases.
 * 
 * Key considerations:
 * 1. Evaluates both buildings and units that can be purchased
 * 2. Uses dedicated evaluators for each construction type
 * 3. Considers city status (non-puppet, non-razed)
 * 4. Evaluates strategic value vs gold cost
 * 
 * Similar to ConstructionAutomation.kt (lines 32-92), this strategy considers:
 * - City production levels
 * - Civilization's current needs (gold, happiness, culture)
 * - Victory condition requirements
 * - Personality traits and focus areas
 */
object ConstructionStrategy : IPurchasingStrategy {

    private fun getDisabledAutoAssignConstructions(civ: Civilization): Set<String> =
        if (civ.isHuman()) GUI.getSettings().disabledAutoAssignConstructions
        else emptySet()

    private fun shouldAvoidConstruction(
        construction: IConstruction,
        city: City,
        personality: Personality
    ): Boolean {
        val constructionsToAvoid = personality.getMatchingUniques(
            UniqueType.WillNotBuild,
            StateForConditionals(city)
        ).map { it.params[0] }

        for (toAvoid in constructionsToAvoid) {
            if (construction is Building && construction.matchesFilter(toAvoid))
                return true
            if (construction is BaseUnit && construction.matchesFilter(toAvoid))
                return true
        }
        return false
    }

    override fun evaluatePurchases(civ: Civilization, personality: Personality): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        // Filter cities similar to how ConstructionAutomation handles city filtering (lines 56-70)
        for (city in civ.cities.filter { !it.isPuppet && !it.isBeingRazed }) {
            // Get recommended constructions from dedicated evaluators
            // BuildingEvaluator (lines 8-12) considers city-specific needs
            val recommendedBuilding = BuildingEvaluator.determineBuildingToPurchase(city, personality)
            val recommendedUnit = UnitEvaluator.determineUnitToPurchase(city, personality)

            // Evaluate recommended building if available
            if (recommendedBuilding is Building) {
                evaluateConstruction(recommendedBuilding, city, civ, personality)?.let { 
                    purchaseOptions.add(it) 
                }
            }

            // Evaluate recommended unit if available
            if (recommendedUnit is BaseUnit) {
                evaluateConstruction(recommendedUnit, city, civ, personality)?.let { 
                    purchaseOptions.add(it) 
                }
            }

            // Filter criteria for buildings matches ConstructionAutomation (lines 58-63):
            // 1. Has positive cost (excludes free buildings)
            // 2. Can be purchased with gold
            // 3. Not the already evaluated recommended building
            val constructableBuildings = city.cityConstructions.getBuildableBuildings()
                .filter { building -> 
                    building.cost > 0 && 
                    building.name !in getDisabledAutoAssignConstructions(civ) &&
                    !shouldAvoidConstruction(building, city, personality) &&
                    city.cityConstructions.isConstructionPurchaseAllowed(building, Stat.Gold, building.cost) &&
                    building != recommendedBuilding
                }

            // Similar filtering for units (lines 67-70)
            val constructableUnits = city.cityConstructions.getConstructableUnits()
                .filter { unit -> 
                    unit.cost > 0 &&
                    unit.name !in getDisabledAutoAssignConstructions(civ) &&
                    !shouldAvoidConstruction(unit, city, personality) &&
                    city.cityConstructions.isConstructionPurchaseAllowed(unit, Stat.Gold, unit.cost) &&
                    (recommendedUnit == null || unit != recommendedUnit)
                }

            // Evaluate all remaining valid constructions
            constructableBuildings.forEach { building ->
                evaluateConstruction(building, city, civ, personality)?.let { 
                    purchaseOptions.add(it) 
                }
            }

            constructableUnits.forEach { unit ->
                evaluateConstruction(unit, city, civ, personality)?.let { 
                    purchaseOptions.add(it) 
                }
            }
        }

        return purchaseOptions
    }

    // TODO: Remove this
//    private fun canBuildConstruction(
//        construction: IConstruction,
//        city: City,
//        civ: Civilization
//    ): Boolean {
//        if (construction !is INonPerpetualConstruction) return true
//        return Automation.allowAutomatedConstruction(civ, city, construction)
//    }

    /**
     * Evaluates a single construction (building or unit) for potential purchase.
     * Uses similar evaluation logic to ConstructionAutomation (lines 363-382).
     * 
     * @param construction The construction to evaluate
     * @param city The city where the construction would be built
     * @param civ The civilization making the purchase
     * @param personality The civilization's personality traits
     * @return PurchaseOption if the construction should be considered, null otherwise
     */
    private fun evaluateConstruction(
        construction: IConstruction,
        city: City,
        civ: Civilization,
        personality: Personality
    ): PurchaseOption? {
    // Get the gold cost for the construction
        val goldCost = when (construction) {
            is Building -> construction.getStatBuyCost(city, Stat.Gold) ?: return null
            is BaseUnit -> construction.getStatBuyCost(city, Stat.Gold) ?: return null
            else -> return null
        }

        if (!city.cityConstructions.isConstructionPurchaseAllowed(
                construction as? INonPerpetualConstruction ?: return null,
                Stat.Gold,
                goldCost
            )) {
            println("${construction.name} rejected: purchase not allowed in ${city.name}")
            return null
        }
        
        val perceivedValue = when (construction) {
            is Building -> BuildingEvaluator.calculateBuildingValue(construction, city, personality)
            is BaseUnit -> UnitEvaluator.calculateUnitValue(construction, city, personality)
            else -> return null
        }
        println("${construction.name} value calculated: $perceivedValue")

        if (goldCost > civ.gold) {
            println("${construction.name} rejected: cost $goldCost exceeds available gold ${civ.gold}")
            return null
        }

        if (!PurchaseDecisionEngine.shouldPurchase(perceivedValue, goldCost, civ.gold, civ)) {
            println("${construction.name} rejected by PurchaseDecisionEngine: " +
                    "value $perceivedValue, cost $goldCost")
            return null
        }

        return PurchaseOption(
            type = PurchaseOption.PurchaseType.Construction,
            cost = goldCost,
            baseValue = perceivedValue.toFloat(),
            description = "Purchase ${construction.name} in ${city.name}",
            action = { 
                when (construction) {
                    is INonPerpetualConstruction -> city.cityConstructions.purchaseConstruction(
                        construction,
                        -1,  // Not from queue
                        false,  // Not automatic
                        Stat.Gold
                    )
                    else -> false
                }
            }
        )
    }
}