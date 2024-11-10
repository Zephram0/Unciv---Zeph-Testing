package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.Constants
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.diplomacy.DiplomacyFlags
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.nation.PersonalityValue
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.tile.TileResource
import com.unciv.models.ruleset.unique.StateForConditionals
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.ruleset.Victory
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats
import com.unciv.logic.map.AStar
import com.unciv.logic.map.BFS
import com.unciv.logic.map.MapPathing
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.unciv.models.ruleset.tile.TerrainType
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.city.City

object TileEvaluator {
    // Cache key composed of tile position and civilization name for efficient lookup
    private data class TileEvalKey(val position: Vector2, val civName: String)

    // Extension function to check if a civilization is a major civilization
    private fun Civilization.isMajor(): Boolean = !isCityState && !isBarbarian
    
    // Extension function to check if a city is a major city
    private fun City.isMajor(): Boolean = population.population >= 5 && !isPuppet
    
    // Caches for different evaluation aspects
    private val rankCache = mutableMapOf<TileEvalKey, Int>()
    private val resourceValueCache = mutableMapOf<TileEvalKey, Float>()
    private val strategicPositionCache = mutableMapOf<TileEvalKey, Float>()
    private val improvementModifierCache = mutableMapOf<TileEvalKey, Float>()
    private val connectivityCache = mutableMapOf<Pair<Tile, Tile>, Boolean>()
    
    // Cached key regions with turn-based invalidation
    private data class KeyRegionsData(
        val regions: Array<Tile>,
        val turnCached: Int
    )
    private var keyRegionsCache: KeyRegionsData? = null

    /**
     * Checks if a list of tiles has reached a destination tile
     */
    private fun List<Tile>.hasReachedDestination(destination: Tile): Boolean {
        return isNotEmpty() && last() == destination
    }

    /**
     * Clears all caches. Should be called at the start of each turn or when major map changes occur.
     */
    fun clearCaches() {
        rankCache.clear()
        resourceValueCache.clear()
        strategicPositionCache.clear()
        improvementModifierCache.clear()
        keyRegionsCache = null
    }

    /**
     * Creates a cache key for a tile and civilization combination
     */
    private fun createCacheKey(tile: Tile, civ: Civilization) =
        TileEvalKey(tile.position, civ.civName)

    /**
     * Updates the cache of key regions for the given civilization.
     * Only updates if the cache is invalid or from a different turn.
     */
    fun updateKeyRegions(civ: Civilization) {
        val currentTurn = civ.gameInfo.turns
        if (keyRegionsCache?.turnCached != currentTurn) {
            keyRegionsCache = KeyRegionsData(
                regions = civ.cities
                    .filter { it.isCapital() || it.isMajor() }
                    .map { it.getCenterTile() }
                    .toTypedArray(),
                turnCached = currentTurn
            )
        }
    }

    /**
     * Retrieves the cached key regions, updating if necessary
     */
    fun getKeyRegions(): List<Tile> = 
        keyRegionsCache?.regions?.toList() ?: emptyList()

    /**
     * Ranks a tile based on its base value, resources, and personality modifiers.
     * Uses caching to avoid recalculating values for the same tile-civ combination.
     */
    fun rankTile(tile: Tile, civ: Civilization, personality: Personality): Int {
        val cacheKey = createCacheKey(tile, civ)
        return rankCache.getOrPut(cacheKey) {
            var value = calculateBaseValue(tile, civ)
            
            val strategicMultiplier = when {
                tile.naturalWonder != null -> 2.0f
                tile.hasResource() -> getResourceStrategicModifier(tile, civ)
                else -> 1.0f
            }
            value = (value * strategicMultiplier).toInt()
            
            applyPersonalityModifiers(value, personality)
        }
    }

        /**
     * Gets cached strategic modifier or calculates it
     */
    private fun getResourceStrategicModifier(tile: Tile, civ: Civilization): Float {
        val cacheKey = createCacheKey(tile, civ)
        return resourceValueCache.getOrPut(cacheKey) {
            resourceStrategicModifier(tile, civ)
        }
    }
    

    /**
     * Calculates the base value of a tile based on its stats.
     *
     * @param tile The tile to evaluate.
     * @param civ The civilization evaluating the tile.
     * @return The base value of the tile.
     */
    private fun calculateBaseValue(tile: Tile, civ: Civilization): Int {
        // Retrieve optimized tile stats
        val tileStats = tile.stats.getTileStats(civ)
        var value = 0

        // Accumulate value based on relevant stats
        for (stat in Stat.values()) {
            val statValue = tileStats[stat]
            value += if (civ.wantsToFocusOn(stat)) {
                (statValue * 2f).toInt() // Double the value if civ focuses on this stat
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

        // Linear scaling based on expansion personality trait (0-10)
        // expansionMultiplier scales from 0.5 to 2.0 based on the expansion value
        val expansionMultiplier = when {
            personality.expansion <= 5 -> 0.5f + (personality.expansion * 0.1f) // 0.5 to 1.0
            else -> 1.0f + ((personality.expansion - 5) * 0.2f) // 1.0 to 2.0
        }
        value = (value * expansionMultiplier).toInt()

        // Additional modifiers based on other personality traits can be added here

        return value
    }

    /**
     * Evaluates the value of a resource on the tile.
     *
     * @param tile The tile to evaluate.
     * @param civ The civilization evaluating the tile.
     * @param personality The personality traits influencing resource evaluation.
     * @return The resource value multiplier.
     */
    /**
     * Evaluates the value of a resource on the tile with caching
     */
    fun evaluateResourceValue(tile: Tile, civ: Civilization, personality: Personality): Float {
        if (!tile.hasResource()) return 1.0f
        
        val cacheKey = createCacheKey(tile, civ)
        return resourceValueCache.getOrPut(cacheKey) {
            // Original resource evaluation logic
            calculateResourceValue(tile, civ, personality)
        }
    }

        /**
     * Actual resource value calculation, separated for clarity
     */
    private fun calculateResourceValue(tile: Tile, civ: Civilization, personality: Personality): Float {
        val tileResource = tile.tileResource
        
        // Check improvement possibility
        val firstImprovement = tileResource.getImprovements().firstOrNull()?.let { 
            civ.gameInfo.ruleset.tileImprovements[it] 
        }

        if (firstImprovement == null || !tile.improvementFunctions.canBuildImprovement(firstImprovement, civ)) {
            return 1f
        }

        // Worker availability check
        val hasAvailableWorker = civ.units.getCivUnits().any { unit -> 
            unit.hasUnique(UniqueType.BuildImprovements) && 
            unit.action == null && !unit.automated &&
            unit.movement.canReach(tile)
        }

        val workerModifier = if (!civ.tech.canResearchTech()) 0.9f else {
            if (hasAvailableWorker) 1.2f else 0.9f
        }

        val improvementModifier = getImprovementModifier(tile, civ)

        return when (tileResource.resourceType) {
            ResourceType.Strategic -> evaluateStrategicResource(tileResource, civ, personality)
            ResourceType.Luxury -> evaluateLuxuryResource(tileResource, civ, personality)
            else -> 1.0f
        } * workerModifier * improvementModifier
    }

    /**
     * Gets cached improvement modifier or calculates it
     */
    private fun getImprovementModifier(tile: Tile, civ: Civilization): Float {
        val cacheKey = createCacheKey(tile, civ)
        return improvementModifierCache.getOrPut(cacheKey) {
            calculateImprovementModifier(tile, civ)
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
        
        // Separately check forest and jungle clearing capabilities
        // Forest requires Mining tech, while Jungle requires Bronze Working
        if (tile.terrainFeatures.contains("Forest")) {
            val canClearForest = civ.tech.isResearched("Mining")
            // Higher value if we can clear it now for production bonus
            // Slightly lower if we need to wait for tech
            modifier *= if (canClearForest) 1.1f else 0.9f
        } else if (tile.terrainFeatures.contains("Jungle")) {
            val canClearJungle = civ.tech.isResearched("Bronze Working") 
            // Same modifiers as forest - immediate bonus vs future potential
            modifier *= if (canClearJungle) 1.1f else 0.9f
        }
        
        // Determine possible improvements and adjust modifier based on tech requirements
        val possibleImprovements = tile.tileResource.getImprovements()
            .mapNotNull { civ.gameInfo.ruleset.tileImprovements[it] }

        //TODO: Add linear personality value modifier based on exact improvement time to clear and exact value upon clearing
        //TODO: Consider improvements. Consider value before improved, time to improve, and value after improved.
        //TODO: Further, if we need to clear something and it can be improved, consider tile value before and after clear and after improvement.
        
        //Check, if the tile can be improved, then if it can be improved now, increase modifier,
        //otherwise decrease modifier
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
            return (personality.scaledFocus((PersonalityValue.Science)) / 5f)
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
        value += (potentialUses * 0.1f) * (personality.scaledFocus(PersonalityValue.Military) / 5f)

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
            val happinessNeed = (-civ.stats.happiness).coerceAtLeast(0)
            value += happinessNeed * 2
        }
        
        //TODO: Is this cheating to know what other civs want?
        // Linear scaling based on trade potential
        val tradingCivs = civ.diplomacy.values.count { diplomacy -> 
            diplomacy.otherCiv().cities.any { city -> 
                city.demandedResource == resource.name 
            } &&
            !diplomacy.hasFlag(DiplomacyFlags.ResourceTradesCutShort)
        }
        value += (tradingCivs * 0.15f) * (personality.scaledFocus(PersonalityValue.Diplomacy) / 5f)
    
        // Linear scaling for WLTKD potential
        val wltkdCities = civ.cities.count { it.demandedResource == resource.name }
        value += (wltkdCities * 0.2f) * (personality.scaledFocus(PersonalityValue.Food)) / 5f

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
        
        // Expansion: higher score if owned neighbors are few, encouraging expansion
        if (ownedNeighbors < 3) {
            strategicScore += (3 - ownedNeighbors) * 0.2f * personality.scaledFocus(PersonalityValue.Expansion)
        }
        
        // Resource denial value scaled by aggressive focus
        val nearbyResources = tile.neighbors.count { it.hasViewableResource(civ) }
        if (nearbyResources > 0) {
            strategicScore += (nearbyResources * 0.25f) * 
                personality.scaledFocus(PersonalityValue.Aggressive)
        }
        
        // Evaluate chokepoint asynchronously
        strategicScore += evaluateChokepoint(tile, civ, personality, getKeyRegions())
        
        return strategicScore
    }

    /**
     * Evaluates the chokepoint value of a tile considering terrain and personality traits.
     *
     * @param tile The tile to evaluate.
     * @param civ The civilization evaluating the tile.
     * @param personality The personality traits influencing chokepoint evaluation.
     * @param keyRegions The list of key region tiles for connectivity assessment.
     * @return The chokepoint score.
     */
    private fun evaluateChokepoint(tile: Tile, civ: Civilization, personality: Personality, keyRegions: List<Tile>): Float {
        val neighbors = tile.neighbors.toList()
        val passableNeighbors = neighbors.count { it.isLand || it.isWater }
        val impassableNeighbors = neighbors.count { it.isImpassible() }
    
        var chokepointScore = recognizeBarrierPattern(tile)
    
        // Linear scaling based on number of passable neighbors
        if (passableNeighbors < 6) {
            chokepointScore += (6 - passableNeighbors) * 0.2f
        }
    
        // Additional value for natural barriers
        chokepointScore += impassableNeighbors * 0.3f
    
        // Assess connectivity importance
        val connectivityScore = assessConnectivity(tile, civ, keyRegions)
        chokepointScore += connectivityScore * 0.5f // Weight connectivity influence
        
        // Scale by military and aggressive focus
        return chokepointScore * 
            ((personality.scaledFocus(PersonalityValue.Military) + 
              personality.scaledFocus(PersonalityValue.Aggressive)) / 2f)
    }

        /**
     * Recognizes and scores barrier patterns based on terrain features.
     *
     * @param tile The tile to evaluate.
     * @return The barrier pattern score.
     */
    private fun recognizeBarrierPattern(tile: Tile): Float {
        var patternScore = 0f
    
        // Example Pattern 1: Mountain Range
        if (tile.isImpassible()) {
            patternScore += 0.5f
        }
    
        // Example Pattern 2: River Crossing
        if (tile.isAdjacentToRiver()) {  // Changed from hasRiver() to isAdjacentToRiver()
            patternScore += 0.4f
        }
    
        return patternScore
    }

    /**
     * Asynchronously assesses the connectivity between key regions excluding the chokepoint tile.
     *
     * @param chokepointTile The chokepoint tile to exclude from pathfinding.
     * @param civ The civilization evaluating the connectivity.
     * @param keyRegions The list of key region tiles.
     * @return The connectivity score based on disconnected region pairs.
     */
    private fun assessConnectivity(
        chokepointTile: Tile,
        civ: Civilization,
        keyRegions: List<Tile>
    ): Float {
        val totalPairs = keyRegions.size * (keyRegions.size - 1) / 2
        if (totalPairs == 0) return 1.0f
    
        var disconnectedPairs = 0
    
        // Iterate over all unique pairs of key regions
        for (i in 0 until keyRegions.size) {
            for (j in i + 1 until keyRegions.size) {
                val regionA = keyRegions[i]
                val regionB = keyRegions[j]
                val cacheKey = Pair(regionA, regionB)
    
                // Check if connectivity is already cached
                val isConnected = connectivityCache.getOrPut(cacheKey) {
                    !areConnectedExcludingChokepoint(regionA, regionB, civ, chokepointTile)
                }
    
                if (!isConnected) {
                    disconnectedPairs++
                }
            }
        }
    
        // Calculate connectivity score as the ratio of connected pairs
        return if (totalPairs > 0) {
            1.0f - (disconnectedPairs.toFloat() / totalPairs)
        } else {
            1.0f
        }
    }

    /**
     * Determines if two tiles are connected excluding the chokepoint tile using BFS.
     *
     * @param start The starting tile.
     * @param end The ending tile.
     * @param civ The civilization evaluating connectivity.
     * @param chokepoint The chokepoint tile to exclude.
     * @return True if connected, False otherwise.
     */
    private fun areConnectedExcludingChokepoint(
        start: Tile,
        end: Tile,
        civ: Civilization,
        chokepoint: Tile
    ): Boolean {
        val bfs = BFS(start) { 
            it.canCivPassThrough(civ) && it != chokepoint 
        }
        bfs.stepUntilDestination(end)
        return bfs.hasReachedTile(end)
    }

    /**
     * Determines the destination tile for road construction based on strategic needs.
     *
     * @param civ The civilization.
     * @param chokepointTile The chokepoint tile.
     * @param keyRegions The list of key regions.
     * @return The destination tile for road construction.
     */
    private fun determineDestinationTile(civ: Civilization, chokepointTile: Tile, keyRegions: List<Tile>): Tile {
        fun canMoveThrough(civInfo: Civilization, tile: Tile): Boolean {
            val owner = tile.getOwner()
            return !tile.isImpassible() && (owner == null || owner == civInfo || owner.isAtWarWith(civInfo))
        }
    
        // Find the nearest key region not connected by roads
        return keyRegions.firstOrNull { key ->
            !MapPathing.getConnection(civ, chokepointTile, key, ::canMoveThrough).isNullOrEmpty()
        } ?: keyRegions.first()
    }

    // Extension function to check if a tile has a resource
    private fun Tile.hasResource(): Boolean = resource != null

}