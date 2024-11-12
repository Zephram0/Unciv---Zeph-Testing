package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.Constants
import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.automation.civilization.purchases.evaluators.TileEvaluator
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.nation.PersonalityValue
import com.unciv.models.stats.Stats
import com.unciv.logic.city.managers.CityExpansionManager
import com.unciv.models.stats.Stat

/**
 * AI Strategy for purchasing tiles, integrating with CityStats and CityExpansionManager.
 * 
 * Strategic considerations:
 * 1. Resource acquisition (via CityStats resource evaluation):
 *    - Strategic resources for military units (higher priority if pursuing Military victory)
 *    - Luxury resources when happiness is low or for trade
 *    - Bonus resources for growth/production
 * 
 * 2. City development (via CityStats yields):
 *    - Food tiles for growth
 *    - Production tiles for construction
 *    - Gold tiles for economy
 *    - Science/Culture tiles based on victory focus
 * 
 * 3. Territory control:
 *    - Blocking enemy expansion
 *    - Connecting fragmented territory
 *    - Securing strategic positions
 */
object TileStrategy : IPurchasingStrategy {
    // Cache for tile stats to avoid recalculation during a single evaluation cycle
    private val tileStatsCache = mutableMapOf<Pair<Tile, City>, Stats>()
    private val tileValueCache = mutableMapOf<Tile, Float>()
    
    override fun evaluatePurchases(
        civ: Civilization, 
        personality: Personality
    ): List<PurchaseOption> {
        clearCaches()
        val purchaseOptions = civ.cities
            .asSequence()
            .filter { it.isNormalCity() }
            .flatMap { city -> evaluateCityTiles(city, civ, personality) }
            .toList()
    
        // Debug logging before return
        if (purchaseOptions.isNotEmpty()) {
            println("\nTile Purchase Options:")
            val bestOption = purchaseOptions.maxByOrNull { it.baseValue / it.cost }
            val worstOption = purchaseOptions.minByOrNull { it.baseValue / it.cost }
            
            bestOption?.let {
                println("  Best: ${it.description}")
                println("    Value/Cost: ${it.baseValue}/${it.cost} = ${it.baseValue.toFloat()/it.cost}")
            }
            worstOption?.let {
                println("  Worst: ${it.description}")
                println("    Value/Cost: ${it.baseValue}/${it.cost} = ${it.baseValue.toFloat()/it.cost}")
            }
            
            // Group by city for clearer output
            println("\nAll Tile Options (by city):")
            purchaseOptions.groupBy { it.description.substringAfterLast(" at ") }
                .forEach { (location, options) ->
                    println("  Location: $location")
                    options.forEach { option ->
                        println("    ${option.description}")
                        println("      Value/Cost: ${option.baseValue}/${option.cost} = ${option.baseValue.toFloat()/option.cost}")
                    }
                }
        }
    
        return purchaseOptions
    }

    /**
     * Evaluates all purchasable tiles for a specific city.
     * Returns a sequence of purchase options for valid tiles.
     */
    private fun evaluateCityTiles(
        city: City,
        civ: Civilization,
        personality: Personality
    ): Sequence<PurchaseOption> {
        val expansionManager = city.expansion
        println("\nEvaluating tiles for ${city.name}:")
        
        val workableTilesCount = city.getWorkableTiles().count().toFloat()
        val populationPressure = calculatePopulationPressure(city.population.population, workableTilesCount)
        
        return expansionManager.getChoosableTiles()
            .asSequence()
            .filter { expansionManager.canBuyTile(it) }
            .mapNotNull { tile -> 
                createPurchaseOption(
                    tile = tile,
                    city = city,
                    civ = civ,
                    personality = personality,
                    expansionManager = expansionManager,
                    populationPressure = populationPressure
                )
            }
    }

    /**
     * Creates a purchase option for a specific tile if it meets all criteria.
     * Returns null if the tile should not be purchased.
     */
    private fun createPurchaseOption(
        tile: Tile,
        city: City,
        civ: Civilization,
        personality: Personality,
        expansionManager: CityExpansionManager,
        populationPressure: Float
    ): PurchaseOption? {
        val pathCost = expansionManager.getGoldCostOfTile(tile)
        val tileRank = TileEvaluator.rankTile(tile, civ, personality)
        
        if (!isTileBetterThanCurrentWorked(city, tile)) {
            println("    Rejected: Not better than current worked tile")
            return null
        }
        
        val baseValue = calculateTileValue(
            tile = tile,
            civ = civ,
            city = city,
            personality = personality,
            populationPressure = populationPressure
        ) * tileRank

        println("    Final value calculated: $baseValue")

        if (!PurchaseDecisionEngine.shouldPurchase(baseValue.toInt(), pathCost, civ.gold, civ)) {
            println("    Rejected: Failed purchase decision check")
            return null
        }
        
        return PurchaseOption(
            type = PurchaseOption.PurchaseType.Tile,
            cost = pathCost,
            baseValue = baseValue,
            description = getTileDescription(tile),
            action = { city.expansion.buyTile(tile) }
        )
    }

    /**
     * Calculates the total value of a tile considering multiple factors and applies
     * personality-based modifiers.
     */
    private fun calculateTileValue(
        tile: Tile,
        civ: Civilization,
        city: City,
        personality: Personality,
        populationPressure: Float
    ): Float {
        return tileValueCache.getOrPut(tile) {
            // Base strategic value from TileEvaluator
            var value = TileEvaluator.rankTile(tile, civ, personality).toFloat()
            
            // Get tile stats value
            val tileStats = getTileStats(tile, city)
            val statsValue = calculateStatsValue(tileStats, personality)

            // Calculate city-specific value using cached stats
            val citySpecificValue = calculateCitySpecificValue(
                tile = tile,
                city = city,
                personality = personality,
                tileStats = tileStats  // Pass cached stats
            )
        
            // Apply strategic position value
            val strategicScore = TileEvaluator.evaluateStrategicPosition(tile, civ, personality)
            
            // Combine all components with appropriate weights
            val combinedValue = (value *  
                                citySpecificValue * 
                                statsValue ) * 
                                (1.0f + strategicScore)
            
            // Apply population pressure modifier for growing cities
            if (populationPressure > 0.8f) {
                return@getOrPut combinedValue * (1.0f + ((populationPressure - 0.8f) * 2f))
            }
            
            combinedValue
        }
    }

    /**
     * Calculates the value contribution from tile stats, weighted by personality traits
     */
    private fun calculateStatsValue(stats: Stats, personality: Personality): Float {
        return stats.values.mapIndexed { index, value ->
            val statType = Stat.values()[index]
            (value * 10.0 * personality.scaledFocus(PersonalityValue.valueOf(statType.name))).toFloat()
        }.sum()
    }

    /**
     * Calculates city-specific value components for a tile.
     *
     * @param tile The tile being evaluated.
     * @param city The city evaluating the tile.
     * @param personality The personality traits of the civilization.
     * @return The calculated city-specific value.
     */
    private fun calculateCitySpecificValue(
        tile: Tile,
        city: City,
        personality: Personality,
        tileStats: Stats 
    ): Float {
        var specificValue = 0f
        
        // Distance from city center (closer tiles are more valuable)
        val distanceFromCenter = city.getCenterTile().aerialDistanceTo(tile)
        val expansionFocus = personality.scaledFocus(PersonalityValue.Expansion)
        specificValue += ((6 - distanceFromCenter).coerceAtLeast(0) * expansionFocus).toFloat()
        
        // Use cached stats for work pattern comparison
        specificValue += createsBetterWorkPattern(city, tileStats)
        
        // Consider city's specific resource needs using cached stats
        specificValue += cityNeedsResource(city, personality, tileStats)
        
        return specificValue
    }

    /**
     * Determines if adding a new tile creates a better worked tile pattern for the city.
     */
    private fun createsBetterWorkPattern(
        city: City,
        newTileStats: Stats
    ): Float {
        val currentWorkedTiles = city.workedTiles
            .mapNotNull { city.tileMap[it] }

        if (currentWorkedTiles.isEmpty()) return 1f

        val newTileValue = newTileStats.values.sum()

        val worstWorkedTileValue = currentWorkedTiles
            .minOfOrNull { getTileStats(it, city).values.sum() } ?: return 1f

        return newTileValue - worstWorkedTileValue
    }

    /**
     * Evaluates how much a city needs the resources provided by a tile based on critical city needs.
     * Returns a float value where:
     * - 0.0f means no critical need
     * - Values up to 2.0f indicate increasing levels of need
     *
     * @param city The city evaluating the tile.
     * @param tile The tile to be evaluated.
     * @param personality The personality traits influencing need evaluation.
     * @param tileStats The pre-calculated tile stats to avoid recalculation
     * @return Float value indicating how critically the city needs the tile's yields
     */
    private fun cityNeedsResource(
        city: City,
        personality: Personality,
        tileStats: Stats
    ): Float {
        var needScore = 0f
        val currentStats = city.cityStats.currentCityStats
        
        // Food need - higher scaling when negative, linear otherwise
        val foodPerTurn = city.foodForNextTurn()
        needScore += when {
            foodPerTurn <= 0 -> tileStats.food * (1.5f - foodPerTurn)  // Higher value when starving
            else -> tileStats.food / foodPerTurn  // Linear decrease up to 0.2
        } * personality.scaledFocus(PersonalityValue.Food)
        
        // Production need - linear increase as production decreases
        needScore += (tileStats.production / currentStats.production.coerceAtLeast(0.5f)) * 
            personality.scaledFocus(PersonalityValue.Production)
        
        // Culture need - linear increase as culture decreases
        needScore += (tileStats.culture / currentStats.culture.coerceAtLeast(0.5f)) * 
            personality.scaledFocus(PersonalityValue.Culture)
        
        return needScore
    }

    /**
     * Calculates population pressure on city workable tiles
     */
    private fun calculatePopulationPressure(population: Int, workableTilesCount: Float): Float {
        return if (workableTilesCount > 0) population.toFloat() / workableTilesCount else 1f
    }

    /**
     * Gets cached tile stats or calculates and caches them
     */
    private fun getTileStats(tile: Tile, city: City): Stats {
        return tileStatsCache.getOrPut(tile to city) {
            tile.stats.getTileStats(city.civ)
        }
    }

    /**
     * Determines if a tile would be better than the current worst worked tile
     */
    private fun isTileBetterThanCurrentWorked(city: City, newTile: Tile): Boolean {
        val currentWorkedTiles = city.workedTiles
            .mapNotNull { city.tileMap[it] }
        
        if (currentWorkedTiles.isEmpty()) return true
        
        // Use already cached stats from previous calculations
        val newTileStats = getTileStats(newTile, city)
        val newTileValue = newTileStats.values.sum()
        
        val worstWorkedTileValue = currentWorkedTiles
            .minOfOrNull { getTileStats(it, city).values.sum() } ?: return false
            
        return newTileValue > worstWorkedTileValue
    }

    /**
     * Generates a descriptive string for the tile
     */
    private fun getTileDescription(tile: Tile): String {
        val position = "(${tile.position.x}, ${tile.position.y})"
        return when {
            tile.naturalWonder != null -> "Natural Wonder ${tile.naturalWonder} at $position"
            tile.resource != null -> "${tile.tileResource.resourceType} resource ${tile.resource} at $position"
            else -> "Tile at $position"
        }
    }

    /**
     * Clears all caches at the start of each evaluation cycle
     */
    private fun clearCaches() {
        tileStatsCache.clear()
        tileValueCache.clear()
    }

    /**
     * Extension function to check if a city is a normal (non-puppet, non-razed) city
     */
    private fun City.isNormalCity() = !isPuppet && !isBeingRazed
}