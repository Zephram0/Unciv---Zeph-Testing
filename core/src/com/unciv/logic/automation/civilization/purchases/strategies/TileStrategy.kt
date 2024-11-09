package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.Constants
import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.automation.civilization.purchases.evaluators.TileEvaluator
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.BFS
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.unique.StateForConditionals
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats


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
    override fun evaluatePurchases(civ: Civilization, personality: Personality): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        for (city in civ.cities.filter { !it.isPuppet && !it.isBeingRazed }) {
            // Use CityExpansionManager's getChoosableTiles() for valid tile selection
            val expansionManager = city.expansion
            val purchasableTiles = expansionManager.getChoosableTiles().asSequence()
                .filter { expansionManager.canBuyTile(it) }
                .sortedByDescending { tile ->
                    // Cache the rankTile value to avoid redundant calculations
                    val rank = TileEvaluator.rankTile(tile, civ, personality)
                    val statsValue = calculateTileValueFromStats(tile, city)
                    rank * statsValue
                }

            purchasableTiles.forEach { tile ->
                val pathCost = expansionManager.getGoldCostOfTile(tile)
                
                // Cache the rankTile value
                val tileRank = TileEvaluator.rankTile(tile, civ, personality)
                
                // Economic viability check using PurchaseDecisionEngine
                if (!PurchaseDecisionEngine.shouldPurchase(
                        tileRank,
                        pathCost,
                        civ.gold,
                        civ
                    )) return@forEach

                // Check if tile would be worth working using PurchaseDecisionEngine
                if (!PurchaseDecisionEngine.isTileBetterThanCurrent(city, tile, personality)) 
                    return@forEach

                val baseValue = calculateTileValue(tile, civ, city, personality)
                
                purchaseOptions.add(
                    PurchaseOption(
                        type = PurchaseOption.PurchaseType.Tile,
                        cost = pathCost,
                        baseValue = baseValue,
                        description = getTileDescription(tile),
                        action = { city.expansion.buyTile(tile) }
                    )
                )
            }
        }

        return purchaseOptions
    }

    /** 
     * Calculates tile value considering:
     * - Base yields from CityStats
     * - Strategic value from TileEvaluator
     * - Civilization's victory focus
     * - Leader personality traits
     * - Current situation (happiness, resources needed)
     */
    /** 
     * Calculates tile value considering:
     * - Base yields from CityStats
     * - Strategic value from TileEvaluator
     * - Civilization's victory focus
     * - Leader personality traits
     * - Current situation (happiness, resources needed)
     */
    private fun calculateTileValue(tile: Tile, civ: Civilization, city: City, personality: Personality): Float {
        var value = TileEvaluator.rankTile(tile, civ, personality).toFloat()
    
        // Replace binary personality checks with linear scaling
        val tileStats = tile.stats.getTileStats(civ)
        for ((stat, statValue) in tileStats) {
            // Scale each stat based on personality focus
            val personalityFocus = PersonalityValue.valueOf(stat.name).let { personality.scaledFocus(it) }
            value += statValue * 10 * personalityFocus
        }
    
        // Resource value consideration
        value *= TileEvaluator.evaluateResourceValue(tile, civ, personality)
        
        //TODO: Possibly move expansion value consideration to apply to all tiles
        // Apply strategic position modifiers with linear scaling
        val strategicScore = TileEvaluator.evaluateStrategicPosition(tile, civ, personality)
        value *= (1.0f + strategicScore * 0.2f)  // Scale strategic importance
    
        // Consider city's growth needs with linear scaling
        val workableTilesCount = city.getWorkableTiles().count().toFloat()
        val populationPressure = city.population.population.toFloat() / (if (workableTilesCount > 0) workableTilesCount else 1f)
        if (populationPressure > 0.8f) {  // City needs more workable tiles
            value *= 1.0f + ((populationPressure - 0.8f) * 2f)  // Linear scaling based on population pressure
        }
    
        // Consider city stats-based evaluation
        value *= calculateTileValueFromStats(tile, city)
        
        return value
    }

    /** 
     * Determines if purchasing this tile would be better than the city's current worst worked tile
     * Used as an additional filter before creating purchase options
     * 
     * TODO: Add comments to confusing lines
     * TODO: Make linear modifier based on how much better the new tile is, instead of binary
     */
    private fun isTileBetterThanCurrentWorked(city: City, newTile: Tile): Boolean {
        val currentWorkedTiles = city.workedTiles.map { city.tileMap[it] }.filterNotNull()
        if (currentWorkedTiles.isEmpty()) return true

        val worstWorkedTile = currentWorkedTiles.minByOrNull { 
            it.stats.getTileStats(city.civ).values.sum() 
        } ?: return false

        return newTile.stats.getTileStats(city.civ).values.sum() > 
            worstWorkedTile.stats.getTileStats(city.civ).values.sum()
    }

    /**
     * Calculates base tile value from CityStats yields
     * Considers food, production, gold, science, and culture outputs
     * 
     * EVALUATE: Is this necessary?
     */
    private fun calculateTileValueFromStats(tile: Tile, city: City): Float {
        return tile.stats.getTileStats(city.civ).values.sum().toFloat()
    }

    //TODO: Add comments
    private fun getTileDescription(tile: Tile): String {
        val position = "(${tile.position.x}, ${tile.position.y})"
        return when {
            tile.naturalWonder != null -> "Natural Wonder ${tile.naturalWonder} at $position"
            tile.resource != null -> "${tile.tileResource.resourceType} resource ${tile.resource} at $position"
            else -> "Tile at $position"
        }
    }

    //TODO: Add comments
    private fun getPathToTile(city: City, targetTile: Tile): List<Tile>? {
        val bfs = BFS(city.getCenterTile()) { it.getOwner() == null || it.owningCity == city }
        bfs.stepUntilDestination(targetTile)
        
        val path = bfs.getPathTo(targetTile)
            .toList()  // Convert Sequence to List
            .filter { it.getOwner() == null }  // Filter unowned tiles
        
        // Only return the path if it's not empty
        return if (path.isNotEmpty()) path.asReversed() else null
    }
   
}