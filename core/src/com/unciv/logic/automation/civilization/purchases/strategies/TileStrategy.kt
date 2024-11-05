package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.automation.civilization.purchases.evaluators.TileEvaluator
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.city.City
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.map.BFS
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.tile.ResourceType


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
        
        // Skip early game - let cities establish themselves first
        // Uses game speed modifier to adjust timing
        if (civ.gameInfo.turns < (civ.gameInfo.speed.scienceCostModifier * 20).toInt()) 
            return purchaseOptions

        for (city in civ.cities.filter { !it.isPuppet && !it.isBeingRazed }) {
            // Use CityExpansionManager's getChoosableTiles() for valid tile selection
            val desirableTiles = city.expansion.getChoosableTiles()
                .filter { tile -> city.expansion.canBuyTile(tile) }
                .sortedByDescending { tile -> 
                    // Combine TileEvaluator's strategic ranking with city stats-based evaluation
                    TileEvaluator.rankTile(tile, civ, personality) * 
                    calculateTileValueFromStats(tile, city)
                }

            for (tile in desirableTiles) {
                val pathCost = city.expansion.getGoldCostOfTile(tile)
                
                // Economic viability check using PurchaseDecisionEngine
                if (!PurchaseDecisionEngine.shouldPurchase(
                    TileEvaluator.rankTile(tile, civ, personality),
                    pathCost,
                    civ.gold
                )) continue

                // Check if tile would be worth working using PurchaseDecisionEngine
                if (!PurchaseDecisionEngine.isTileBetterThanCurrent(city, tile, personality)) 
                    continue

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
    private fun calculateTileValue(tile: Tile, civ: Civilization, city: City, personality: Personality): Float {
        var value = TileEvaluator.rankTile(tile, civ, personality).toFloat()
        
        // Apply strategic position modifiers
        value *= when {
            // Natural wonders and resources are already handled by TileEvaluator
            isStrategicPosition(tile, civ) -> if (personality.expansion > 6) 1.5f else 1.3f
            else -> 1.0f
        }
    
        // Consider city's growth needs
        if (city.population.population > city.getWorkableTiles().count()) {
            value *= 1.4f  // City needs more workable tiles
        }
    
        // Consider city stats-based evaluation
        value *= calculateTileValueFromStats(tile, city)
        
        return value
    }

    /**
     * Calculates base tile value from CityStats yields
     * Considers food, production, gold, science, and culture outputs
     */
    private fun calculateTileValueFromStats(tile: Tile, city: City): Float {
        return tile.stats.getTileStats(city.civ).values.sum()
    }

    private fun getTileDescription(tile: Tile): String {
        val position = "(${tile.position.x}, ${tile.position.y})"
        return when {
            tile.naturalWonder != null -> "Natural Wonder ${tile.naturalWonder} at $position"
            tile.resource != null -> "${tile.tileResource.resourceType} resource ${tile.resource} at $position"
            else -> "Tile at $position"
        }
    }

    private fun getPathToTile(city: City, targetTile: Tile): List<Tile>? {
        val bfs = BFS(city.getCenterTile()) { it.getOwner() == null || it.owningCity == city }
        bfs.stepUntilDestination(targetTile)
        
        val path = bfs.getPathTo(targetTile)
            .toList()  // Convert Sequence to List
            .filter { it.getOwner() == null }  // Filter unowned tiles
        
        // Only return the path if it's not empty
        return if (path.isNotEmpty()) path.asReversed() else null
    }

    // ... previous code remains the same ...

    /**
     * Determines if a tile is in a strategic position by checking:
     * 1. If it's adjacent to other civilizations (border control)
     * 2. If it connects disconnected parts of our territory
     * 3. If it blocks potential enemy city locations
     * 4. If it controls important chokepoints
     */

    private fun isStrategicPosition(tile: Tile, civ: Civilization): Boolean {
        // Check if tile is adjacent to other civilizations' territory
        val hasEnemyNeighbor = tile.neighbors.any { neighbor -> 
            val owner = neighbor.getOwner()
            owner != null && owner != civ 
        }
        
        // Check if tile connects disconnected territory
        val ownedNeighbors = tile.neighbors.count { neighbor ->
            neighbor.getOwner() == civ
        }
        val connectsTerritory = ownedNeighbors >= 2 && 
            tile.neighbors.any { neighbor -> neighbor.getOwner() != civ }
            
        // Check if tile is a good city location that we want to deny to enemies
        val isGoodCityLocation = tile.neighbors.count { neighbor -> 
            neighbor.hasViewableResource(civ) 
        } >= 2
        
        // Check if tile controls movement through chokepoint
        val isChokepoint = tile.neighbors.count { neighbor -> 
            neighbor.isLand || neighbor.isWater
        } <= 3  // Tiles with few passable neighbors are chokepoints
        
        return hasEnemyNeighbor || connectsTerritory || 
            isGoodCityLocation || isChokepoint
    }
}