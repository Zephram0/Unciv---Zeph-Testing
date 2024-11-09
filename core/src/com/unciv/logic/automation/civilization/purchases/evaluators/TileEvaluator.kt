package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.diplomacy.DiplomacyFlags
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.tile.TileResource
import com.unciv.models.ruleset.unique.StateForConditionals
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.ruleset.Victory
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats

object TileEvaluator {
    /**
     * Ranks a tile based on its base value, resources, and personality modifiers.
     *
     * @param tile The tile to rank.
     * @param civ The civilization evaluating the tile.
     * @param personality The personality traits of the civilization.
     * @return The ranked value of the tile.
     */
    fun rankTile(tile: Tile, civ: Civilization, personality: Personality): Int {
        var value = calculateBaseValue(tile, civ)
        
        // Apply strategic modifiers
        val strategicMultiplier = when {
            tile.naturalWonder != null -> 2.0f
            tile.hasResource() -> resourceStrategicModifier(tile, civ)
            else -> 1.0f
        }
        value = (value * strategicMultiplier).toInt()

        // Apply personality modifiers
        value = applyPersonalityModifiers(value, personality)

        return value
    }
    

    /**
     * Calculates the base value of a tile from its stats.
     *
     * @param tile The tile to evaluate.
     * @param civ The civilization evaluating the tile.
     * @return The base value of the tile.
     */
    private fun calculateBaseValue(tile: Tile, civ: Civilization): Int {
        // Use tile.stats.getTileStats directly as it's already optimized
        val tileStats = tile.stats.getTileStats(civ)
        var value = 0
        
        // Calculate value from tile stats
        for (stat in Stat.values()) {
            val statValue = tileStats[stat]?.toFloat() ?: 0f
            value += if (civ.wantsToFocusOn(stat)) {
                (statValue * 2f).toInt()
            } else {
                statValue.toInt()
            }
        }
        
        return value
    }

    /**
     * Applies personality-based modifiers to the tile value.
     *
     * @param currentValue The current value of the tile.
     * @param personality The personality traits influencing the modifiers.
     * @return The modified tile value.
     */
    private fun applyPersonalityModifiers(currentValue: Int, personality: Personality): Int {
        var value = currentValue
        // Linear scaling of value based on expansion personality trait (0-10)
        // At expansion=0: multiplier is 0.5
        // At expansion=5: multiplier is 1.0
        // At expansion=10: multiplier is 2.0
        val expansionMultiplier = if (personality.expansion <= 5) {
            // Scale from 0.5 to 1.0 for expansion 0-5
            0.5f + (personality.expansion * 0.1f)
        } else {
            // Scale from 1.0 to 2.0 for expansion 6-10
            1.0f + ((personality.expansion - 5) * 0.2f)
        }
        value = (value * expansionMultiplier).toInt()
        return value
    }

    /**
     * Evaluates resource value considering:
     * - Resource type (Strategic/Luxury/Bonus)
     * - Current civilization needs
     * - Improvement requirements
     * - Trade potential
     */
    fun evaluateResourceValue(tile: Tile, civ: Civilization, personality: Personality): Float {
        val resource = tile.tileResource ?: return 1.0f
        
        // Check if we can improve this resource (using WorkerAutomation's logic)
        if (!tile.improvementFunctions.canAnyImprovementBeBuiltHere(civ)) {
            return 0.75f
        }
        
        // Consider if we have a worker that can reach this tile - only if tech is available
        val workerModifier = if (!techsAvailable) 0.9f else {
            val hasAvailableWorker = civ.units.asSequence()
                .filter { unit -> unit.hasUnique(UniqueType.CanBuildImprovements) && !unit.isBusy() }
                .any { unit -> unit.movement.canReach(tile) }
            if (hasAvailableWorker) 1.2f else 0.9f
        }
        
        //comment
        val improvementModifier = calculateImprovementModifier(tile, civ)
        
        return when (resource.resourceType) {
            ResourceType.Strategic -> evaluateStrategicResource(resource, civ, personality) * 
                workerModifier * improvementModifier
            ResourceType.Luxury -> evaluateLuxuryResource(resource, civ, personality) * 
                workerModifier * improvementModifier
            else -> 1.0f * workerModifier * improvementModifier
        }
    }


    /**
     * Calculates the modifier based on improvement potential and time.
     * Considers forest/jungle clearing benefits and technology requirements.
     *
     * @param tile The tile to evaluate
     * @param civ The civilization evaluating
     * @return The improvement modifier value
     */

    private fun calculateImprovementModifier(tile: Tile, civ: Civilization): Float {
        var modifier = 1.0f
        
        // Consider forest/jungle clearing benefits
        if (tile.terrainFeatures.any { it == "Forest" || it == "Jungle" }) {
            val canClear = civ.tech.isResearched("Mining") // For forest
                || civ.tech.isResearched("Bronze Working") // For jungle
            if (canClear) {
                modifier *= 1.1f // Immediate production bonus potential
            } else {
                modifier *= 0.9f // Future potential but requires tech
            }
        }
        
        // Consider improvement technology requirements
        val possibleImprovements = tile.tileResource?.let { resource ->
            resource.getImprovements().mapNotNull { 
                civ.gameInfo.ruleset.tileImprovements[it] 
            }
        } ?: emptyList()

        //TODO: Add linear personality value modifier based on exact improvement time to clear and exact value upon clearing
        //TODO: Consider improvements. Consider value before improved, time to improve, and value after improved.
        //TODO: Further, if we need to clear something and it can be improved, consider tile value before and after clear and after improvement.
        
        if (possibleImprovements.any { improvement ->
            improvement.techRequired == null || civ.tech.isResearched(improvement.techRequired!!)
        }) {
            modifier *= 1.2f // Can be improved immediately
        } else {
            modifier *= 0.8f // Requires future tech
        }
        
        return modifier
    }

    /**
     * Evaluates the value of a strategic resource.
     *
     * @param resource The strategic resource to evaluate.
     * @param civ The civilization evaluating the resource.
     * @param personality The personality traits influencing strategic resource evaluation.
     * @return The value multiplier for the strategic resource.
     */
    private fun evaluateStrategicResource(resource: TileResource, civ: Civilization, personality: Personality): Float {
        // Space resource special case
        if (civ.gameInfo.spaceResources.contains(resource.name) &&
            civ.hasUnique(UniqueType.EnablesConstructionOfSpaceshipParts)
        ) {
            return 2f * (personality[PersonalityValue.Science] / 10f)
        }

        var value = 1.0f
        
        // Scale based on current stockpile - linear decrease as we get more
        val currentAmount = civ.getResourceAmount(resource.name)
        value += (5f - (currentAmount * 0.75f)).coerceAtLeast(0f)
        

        //TODO: Adjust based on need for building/unit completion.
        //TODO: Create centralized building evaluation (cashed?) that can be used here and elsewhere
        // Scale based on number of units/buildings that could use this resource
        val potentialUses = civ.cities.sumOf { city ->
            city.cityConstructions.getBuildableBuildings().count {
                it.getResourceRequirementsPerTurn(StateForConditionals(civ, city))
                    .containsKey(resource.name)
            } +
            city.cityConstructions.getConstructableUnits().count {
                it.getResourceRequirementsPerTurn(StateForConditionals(civ))
                    .containsKey(resource.name)
            }
        }
        value += (potentialUses * 0.1f) * (personality[PersonalityValue.Military] / 5f)

        return value
    }

    /**
     * Evaluates the value of a luxury resource.
     *
     * @param resource The luxury resource to evaluate.
     * @param civ The civilization evaluating the resource.
     * @param personality The personality traits influencing luxury resource evaluation.
     * @return The value multiplier for the luxury resource.
     */
    private fun evaluateLuxuryResource(resource: TileResource, civ: Civilization, personality: Personality): Float {
        var value = 1.0f
        
        // Linear scaling based on happiness needs
        if (!civ.hasResource(resource.name)) {
            val happinessNeed = (-civ.happiness).coerceAtLeast(0)
            value += happinessNeed * 2
        }
        
        //TODO: Is this cheating to know what other civs want?
        // Linear scaling based on trade potential
        val tradingCivs = civ.diplomacy.values.count { 
            it.otherCiv().wantsResource(resource.name) &&
            !it.hasFlag(DiplomacyFlags.ResourceTradesCutShort)
        }
        value += (tradingCivs * 0.15f) * (personality[PersonalityValue.Diplomacy] / 5f)
    
        // Linear scaling for WLTKD potential
        val wltkdCities = civ.cities.count { it.demandedResource == resource.name }
        value += (wltkdCities * 0.2f) * (personality[PersonalityValue.Growth] / 5f)

        return value
    }

    //Is this needed?

    /**
     * Determines the strategic multiplier based on the tile's resource type and civilization's focus.
     *
     * @param tile The tile being evaluated.
     * @param civ The civilization evaluating the tile.
     * @return The strategic multiplier for the tile.
     */
    private fun resourceStrategicModifier(tile: Tile, civ: Civilization): Float {
        return when (tile.tileResource.resourceType) {
            ResourceType.Strategic -> if (civ.wantsToFocusOn(Victory.Focus.Military)) 1.5f else 1.3f
            ResourceType.Luxury -> if (civ.getHappiness() < 0) 1.4f else 1.2f
            else -> 1.1f
        }
    }

    /*
     * Determines if a tile is in a strategic position by checking:
     * 1. If it's adjacent to other civilizations (border control)
     * 2. If it connects disconnected parts of our territory
     * 3. If it blocks potential enemy city locations
     * 4. If it controls important chokepoints
     *
     * @param tile The tile to evaluate.
     * @param civ The civilization evaluating the tile.
     * @param personality The personality traits influencing strategic considerations.
     * @return The strategic score of the tile.
     */
    fun evaluateStrategicPosition(tile: Tile, civ: Civilization, personality: Personality): Float {
        var strategicScore = 0f
        
        // Cache tile owner checks
        // Use TileMap's neighbor functions for proper tile relationships
        val neighbors = tile.neighbors
        val tileOwners = neighbors.mapNotNull { it.getOwner() }
        val enemyNeighbors = tileOwners.count { it != civ }
        val ownedNeighbors = tileOwners.count { it == civ }
        
        // Enemy neighbor value scaled by military and aggressive focus
        if (enemyNeighbors > 0) {
            strategicScore += (enemyNeighbors * 0.3f) * 
                ((personality.scaledFocus(PersonalityValue.Military) + 
                  personality.scaledFocus(PersonalityValue.Aggressive)) / 2f)
        }
        
        // Territory connection value scaled by expansion focus
        if (ownedNeighbors >= 2) {
            strategicScore += (ownedNeighbors * 0.2f) * 
                personality.scaledFocus(PersonalityValue.Expansion)
        }
        
        // Resource denial value scaled by aggressive focus
        val nearbyResources = tile.neighbors.count { it.hasViewableResource(civ) }
        if (nearbyResources > 0) {
            strategicScore += (nearbyResources * 0.25f) * 
                personality.scaledFocus(PersonalityValue.Aggressive)
        }
        
        // Chokepoint value scaled by military focus
        strategicScore += evaluateChokepoint(tile, personality)

        return strategicScore
    }

    /**
     * Evaluates chokepoint value considering terrain and personality.
     *
     * @param tile The tile to evaluate.
     * @param personality The personality traits influencing chokepoint evaluation.
     * @return The chokepoint score.
     */
    private fun evaluateChokepoint(tile: Tile, personality: Personality): Float {
        val neighbors = tile.neighbors.toList()
        val passableNeighbors = neighbors.count { it.isLand || it.isWater }
        val impassableNeighbors = neighbors.count { it.isImpassible() }
        
        var chokepointScore = 0f
        
        // Linear scaling based on number of passable neighbors
        if (passableNeighbors < 6) {
            chokepointScore += (6 - passableNeighbors) * 0.2f
        }
        
        // Additional value for natural barriers
        chokepointScore += impassableNeighbors * 0.3f
        
        // Scale by military and aggressive focus
        return chokepointScore * 
            ((personality.scaledFocus(PersonalityValue.Military) + 
              personality.scaledFocus(PersonalityValue.Aggressive)) / 2f)
    }

    // Extension function to check if a tile has a resource
    private fun Tile.hasResource(): Boolean = resource != null
}