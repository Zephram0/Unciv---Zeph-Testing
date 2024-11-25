package com.unciv.logic.aautoexpert.expertmodules.expertcore

import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger
import com.unciv.logic.aautoexpert.rulevalidator_patch.input.unit.UnitSettlerValidator

/**
 * Part of the Input Refinement Layer.
 * Transforms raw game state into AI-optimized formats for decision making.
 * Does not directly validate game rules - that's handled by the Raw Input Layer.
 */
class ExpertEvaluationModule(private val ruleset: Ruleset) {
    
    /**
     * Evaluates and refines tile data for AI city placement decisions.
     * Uses validated raw inputs to create AI-specific metrics.
     */
    fun evaluateSettlementLocation(tile: Tile): RefinedSettlementData {
        // Create refined data structure for AI use
        return RefinedSettlementData(
            baseScore = calculateBaseScore(tile),
            resourceValue = calculateResourceValue(tile),
            strategicValue = calculateStrategicValue(tile),
            growthPotential = calculateGrowthPotential(tile),
            defensiveValue = calculateDefensiveValue(tile)
        )
    }

    /**
     * Refined data structure for AI decision making
     */
    data class RefinedSettlementData(
        val baseScore: Float,
        val resourceValue: Float,
        val strategicValue: Float,
        val growthPotential: Float,
        val defensiveValue: Float
    ) {
        /**
         * Weighted total score for location quality
         */
        fun getTotalScore(): Float = 
            (baseScore * 1.0f) +
            (resourceValue * 1.5f) +
            (strategicValue * 1.2f) +
            (growthPotential * 1.3f) +
            (defensiveValue * 1.1f)

        /**
         * Determines if this is an exceptional location worth waiting for
         */
        fun isExceptionalLocation(): Boolean =
            getTotalScore() > 15f || // Very high total score
            resourceValue > 8f ||    // Exceptional resources
            (strategicValue > 6f && defensiveValue > 6f) // Perfect defensive position
    }

    private fun calculateBaseScore(tile: Tile): Float {
        var score = 0f
        
        // Basic terrain scoring
        score += when {
            tile.isHill() -> 2f
            tile.terrainFeatures.contains("Forest") -> 1.5f
            tile.baseTerrain == "Grassland" -> 1f
            else -> 0.5f
        }

        if (tile.isAdjacentToRiver()) score += 1f
        if (tile.isAdjacentTo("Mountain", null)) score += 1f

        return score
    }

    private fun calculateResourceValue(tile: Tile): Float {
        var score = 0f
        
        // Score workable resources
        tile.getTilesInDistance(3).forEach { nearbyTile ->
            val resourceName = nearbyTile.resource
            if (resourceName != null) {
                val tileResource = ruleset.tileResources[resourceName]
                if (tileResource != null) {
                    // Base resource value
                    score += when (tileResource.resourceType) {
                        ResourceType.Strategic -> 4f
                        ResourceType.Luxury -> 5f
                        ResourceType.Bonus -> 1.5f
                    }

                    // Accessibility bonus - resources closer to city center are more valuable
                    val distance = nearbyTile.aerialDistanceTo(tile)
                    score += (3 - distance) * 0.5f

                    // Workability bonus - resources that are immediately workable
                    if (distance == 1) score += 1f

                    // Unique resource bonus - first of its type
                    val owner = tile.getOwner()
                    if (owner != null && !owner.hasResource(resourceName)) score += 2f
                }
            }
        }

        return score
    }

    private fun calculateStrategicValue(tile: Tile): Float {
        var score = 0f

        // Count choke points
        val accessibleTiles = tile.getTilesInDistance(2)
            .count { !it.isImpassible() }
        score += (6 - accessibleTiles) * 0.5f

        // Value coastal positions
        if (tile.getTilesInDistance(1).any { it.isWater }) {
            score += 2f
            
            // Extra value for good harbor positions
            val coastalDefense = tile.getTilesInDistance(2)
                .count { it.isImpassible() || it.isHill() }
            score += coastalDefense * 0.5f
        }

        // Trade route potential
        val nearbyLuxuries = tile.getTilesInDistance(4)
            .filter { it.resource != null && ruleset.tileResources[it.resource]?.resourceType == ResourceType.Luxury }
            .count()
        score += nearbyLuxuries * 0.3f

        return score
    }

    private fun calculateGrowthPotential(tile: Tile): Float {
        var score = 0f

        // Count food-producing tiles with weighted values
        tile.getTilesInDistance(3).forEach { nearbyTile ->
            val foodValue = when {
                nearbyTile.baseTerrain == "Grassland" -> 1f
                nearbyTile.terrainFeatures.contains("Forest") -> 0.8f
                nearbyTile.baseTerrain == "Plains" -> 0.7f
                else -> 0.3f
            }
            
            // Distance weighting - closer tiles are more valuable
            val distance = nearbyTile.aerialDistanceTo(tile)
            score += foodValue * (4 - distance) * 0.5f
        }

        // Fresh water bonus with river adjacency
        if (tile.isAdjacentToRiver()) score += 2.5f

        // Hill bonus for early production
        if (tile.isHill()) score += 1.5f

        return score
    }

    private fun calculateDefensiveValue(tile: Tile): Float {
        var score = 0f

        // Count defensive terrain
        val defensiveTiles = tile.getTilesInDistance(2)
            .count { it.isHill() || it.terrainFeatures.contains("Forest") }
        score += defensiveTiles * 0.5f

        // Hill bonus
        if (tile.isHill()) score += 2f

        // Mountain protection
        val mountainCount = tile.getTilesInDistance(1)
            .count { it.isImpassible() }
        score += mountainCount * 1f

        return score
    }
} 