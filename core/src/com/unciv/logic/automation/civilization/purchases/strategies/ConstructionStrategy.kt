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
 * Similar to ConstructionAutomation.kt, this strategy considers:
 * - City production levels
 * - Civilization's current needs (gold, happiness, culture)
 * - Victory condition requirements
 * - Personality traits and focus areas
 */
object ConstructionStrategy : IPurchasingStrategy {


    /**
     * Retrieves a set of constructions that are disabled for auto-assignment based on civilization settings.
     */
    private fun getDisabledAutoAssignConstructions(civ: Civilization): Set<String> =
        if (civ.isHuman()) GUI.getSettings().disabledAutoAssignConstructions
        else emptySet()

    /**
     * Determines whether a construction should be avoided based on personality traits.
     *
     * @param construction The construction to evaluate.
     * @param city The city where the construction would be built.
     * @param personality The civilization's personality traits.
     * @return True if the construction should be avoided, false otherwise.
     */
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

    /**
     * Evaluates potential purchase options for constructions (buildings and units) in cities.
     *
     * @param civ The civilization making the purchases.
     * @param personality The civilization's personality traits.
     * @return A list of viable PurchaseOption objects.
     */
    override fun evaluatePurchases(civ: Civilization, personality: Personality): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        // Start debug session for this strategy
        //println("\nEvaluating construction purchases for ${civ.civName}:")

        for (city in civ.cities.filter { !it.isPuppet && !it.isBeingRazed }) {
            //println("Evaluating city: ${city.name}")    

            // Retrieve all valid buildings for purchase
            val constructableBuildings = city.cityConstructions.getBuildableBuildings()
                .filter { building ->
                    building.cost > 0 &&
                    building.cost < civ.gold &&
                    building.name !in getDisabledAutoAssignConstructions(civ) &&
                    !shouldAvoidConstruction(building, city, personality) &&
                    city.cityConstructions.isConstructionPurchaseAllowed(building, Stat.Gold, building.cost)
                }

            // Retrieve all valid units for purchase
            val constructableUnits = city.cityConstructions.getConstructableUnits()
                .filter { unit ->
                    unit.cost > 0 &&
                    unit.cost < civ.gold &&
                    unit.name !in getDisabledAutoAssignConstructions(civ) &&
                    !shouldAvoidConstruction(unit, city, personality) &&
                    city.cityConstructions.isConstructionPurchaseAllowed(unit, Stat.Gold, unit.cost)
                }

            // Evaluate all buildings and units
            constructableBuildings.forEach { building ->
                //println("Evaluating building: ${building.name}")
                evaluateConstruction(building, city, civ, personality)?.let { option ->
                    purchaseOptions.add(option)
                    //println("Added building purchase option: ${option.description}")
                }
            }

            constructableUnits.forEach { unit ->
                //println("Evaluating unit: ${unit.name}")
                evaluateConstruction(unit, city, civ, personality)?.let { option ->
                    purchaseOptions.add(option)
                    //println("Added unit purchase option: ${option.description}")
                }
            }
        }

        //println("ConstructionStrategy evaluation complete with ${purchaseOptions.size} options")
    
        return purchaseOptions
    }

    /**
     * Evaluates a single construction (building or unit) for potential purchase.
     *
     * @param construction The construction to evaluate.
     * @param city The city where the construction would be built.
     * @param civ The civilization making the purchase.
     * @param personality The civilization's personality traits.
     * @return A PurchaseOption if the construction is viable, null otherwise.
     */
    private fun evaluateConstruction(
        construction: IConstruction,
        city: City,
        civ: Civilization,
        personality: Personality
    ): PurchaseOption? {
        // Early type check and cast
        //println("Evaluating construction: ${construction.name}")

        // Early type check and cast
        val nonPerpetualConstruction = construction as? INonPerpetualConstruction
        if (nonPerpetualConstruction == null) {
            //println("Rejected: Not a non-perpetual construction")
            return null
        }

        // Get the gold cost for the construction
        val goldCost = when (construction) {
            is Building -> construction.getStatBuyCost(city, Stat.Gold) ?: return null
            is BaseUnit -> construction.getStatBuyCost(city, Stat.Gold) ?: return null
            else -> return null
        }

        if (!city.cityConstructions.isConstructionPurchaseAllowed(
                nonPerpetualConstruction,
                Stat.Gold,
                goldCost
            )) {
            //println("Rejected: Purchase not allowed in ${city.name}")
            return null
        }

        val perceivedValue = when (construction) {
            is Building -> {
                val value = BuildingEvaluator.calculateBuildingValue(construction, city, personality)
                //println("Building value calculated: $value")
                value
            }
            is BaseUnit -> {
                val value = UnitEvaluator.calculateUnitValue(construction, city, personality)
                //println("Unit value calculated: $value")
                value
            }
            else -> {
                //println("Rejected: Invalid construction type")
                return null
            }
        }

        //println("${construction.name} value calculated: $perceivedValue")

        if (goldCost > civ.gold) {
            //println("${construction.name} rejected: cost $goldCost exceeds available gold ${civ.gold}")
            return null
        }

        if (!PurchaseDecisionEngine.shouldPurchase(perceivedValue, goldCost, civ.gold, civ)) {
            //println("${construction.name} rejected by PurchaseDecisionEngine: " +
            //                "value $perceivedValue, cost $goldCost")
            return null
        }

        return PurchaseOption(
            type = PurchaseOption.PurchaseType.Construction,
            cost = goldCost,
            baseValue = perceivedValue.toFloat(),
            description = "Purchase ${construction.name} in ${city.name}",
            action = { 
                city.cityConstructions.purchaseConstruction(
                    nonPerpetualConstruction,
                    -1,  // Not from queue
                    false,  // Not automatic
                    Stat.Gold
                )
            }
        )
    }
}