package com.unciv.logic.automation.civilization.purchases.core

import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.stats.Stats
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.logic.automation.civilization.purchases.evaluators.ValueCalculator

object PurchaseDecisionEngine {
    /**
     * Calculates the final score for a purchase option
     */
    fun calculateFinalScore(
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
                else -> 1.0f
            }
            PurchaseType.UnitUpgrade -> when {
                civ.wantsToFocusOn(Victory.Focus.Military) -> 1.3f
                personality.military > 6 -> 1.2f
                else -> 1.0f
            }
            PurchaseType.CityState -> when {
                civ.wantsToFocusOn(Victory.Focus.CityStates) -> 1.4f
                personality.diplomatic > 6 -> 1.2f
                else -> 1.0f
            }
            PurchaseType.Tile -> when {
                civ.wantsToFocusOn(Victory.Focus.Culture) -> 1.3f
                personality.expansion > 6 -> 1.2f
                else -> 1.0f
            }
        }

        return baseScore * personalityMultiplier
    }

    /**
     * Determines if a purchase meets the minimum threshold requirements
     */
    fun shouldPurchase(perceivedValue: Int, goldCost: Int, goldAvailable: Int): Boolean {
        return goldCost <= goldAvailable * 0.8f && // Don't spend more than 80% of available gold
                perceivedValue / goldCost.toFloat() >= 1.5f // Minimum value/cost ratio
    }

    /**
     * Determines if a new tile is better than the worst currently worked tile in the city.
     */
    fun isTileBetterThanCurrent(city: City, newTile: Tile, personality: Personality, civ: Civilization): Boolean {
        val currentTiles = city.population.getWorkedTiles()
        
        val worstTile = currentTiles.minByOrNull { 
            ValueCalculator.calculatePerceivedTileValue(it, personality) 
        } ?: return false
            
        val perceivedValue = if (hasAvailableWorkers(civ, newTile) && canBeImproved(newTile, civ)) {
            maxOfPerceivedValuesOfPossibleImprovements(newTile, personality, civ)
        } else {
            ValueCalculator.calculatePerceivedTileValue(newTile, personality)
        }
        
        return perceivedValue > ValueCalculator.calculatePerceivedTileValue(worstTile, personality)
    }

    /**
     * Checks if the civilization has available workers that could improve this tile
     */
    private fun hasAvailableWorkers(civ: Civilization, tile: Tile): Boolean {
        return civ.units.getCivUnits()
            .any { unit -> 
                unit.hasUnique(UniqueType.WorkerActions) &&
                unit.currentMovement > 0 &&
                unit.getTile().aerialDistanceTo(tile) <= 5 
            }
    }

    /**
     * Checks if the tile can be improved with any improvement
     */
    private fun canBeImproved(tile: Tile, civ: Civilization): Boolean {
        if (tile.improvement != null) return false  // Already improved
        
        return when {
            // Has a resource that can be improved
            tile.resource != null && tile.tileResource.resourceType != ResourceType.Bonus -> true
            // Has vegetation that can be cleared
            tile.terrainFeatures.any { it.hasUnique(UniqueType.Vegetation) } -> true
            // Check if any possible improvements from ruleset can be built here
            tile.tileMap.ruleset.tileImprovements.values.any { 
                tile.canBuildImprovement(it, civ) 
            } -> true
            else -> false
        }
    }

    /**
     * Calculates the maximum potential value of the tile after all possible improvements
     */
    private fun maxOfPerceivedValuesOfPossibleImprovements(
        tile: Tile, 
        personality: Personality,
        civ: Civilization
    ): Int {
        // Get valid improvements for this tile from the ruleset
        val possibleImprovements = tile.tileMap.ruleset.tileImprovements.values
            .filter { improvement -> 
                tile.canBuildImprovement(improvement, civ) &&
                !improvement.hasUnique(UniqueType.GreatImprovement)  // Exclude great person improvements
            }

        if (possibleImprovements.isEmpty()) 
            return ValueCalculator.calculatePerceivedTileValue(tile, personality)

        // Calculate value for each possible improvement
        return possibleImprovements.maxOf { improvement ->
            val improvedTile = tile.clone()
            val civEquivalentImprovement = civ.getEquivalentTileImprovement(improvement)
            improvedTile.improvement = civEquivalentImprovement.name
            ValueCalculator.calculatePerceivedTileValue(improvedTile, personality)
        }
    }

    // Add other utility functions as needed
}
