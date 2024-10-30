package com.unciv.logic.automation.civilization.purchases.decision

import com.unciv.logic.city.City
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.models.stats.Stat
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.stats.Stats
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.tile.TileImprovement

object ValueCalculator {

    /**
     * Calculates the perceived value of a construction (building or unit) based on the AI's personality.
     */
    fun calculatePerceivedConstructionValue(construction: Any, city: City, personality: Personality): Int {
        val stats = getStats(construction, city)
        val dominantStat = getDominantStat(stats) ?: return 0
        val baseValue = stats[Stat.Gold]?.toInt() ?: return 0

        return when (dominantStat) {
            Stat.Science -> calculatePerceivedValueLinear(baseValue, personality.science)
            Stat.Production -> calculatePerceivedValueLinear(baseValue, personality.production)
            Stat.Culture -> calculatePerceivedValueLinear(baseValue, personality.culture)
            Stat.Faith -> calculatePerceivedValueLinear(baseValue, personality.faith)
            Stat.Happiness -> calculatePerceivedValueLinear(baseValue, personality.happiness)
            else -> baseValue
        }
    }

    /**
     * Retrieves all stats for a given construction.
     */
    private fun getStats(construction: Any, city: City): Stats {
        return when (construction) {
            is Building -> construction.getStats(city)
            is UnitType -> construction.getStats(city)
            else -> Stats()
        }
    }

    /**
     * Determines the most significant stat from a map of stats.
     */
    private fun getDominantStat(stats: Stats): Stat? {
        return stats.maxByOrNull { it.value }?.key
    }

    /**
     * Helper function to calculate perceived value linearly based on personality.
     */
    fun calculatePerceivedValueLinear(rawValue: Int, personalityValue: Int): Int {
        return rawValue * personalityValue
    }

    /**
     * Calculates the perceived value of a tile based on its yields and the AI's personality.
     */
    fun calculatePerceivedTileValue(tile: Tile, personality: Personality): Int {
        var value = 0
        value += (tile.yields.science * personality.science).toInt()
        value += (tile.yields.production * personality.production).toInt()
        value += (tile.yields.culture * personality.culture).toInt()
        value += (tile.yields.faith * personality.faith).toInt()
        value += (tile.yields.happiness * personality.happiness).toInt()
        return value
    }

    /**
     * Calculates the perceived value of a tile with a specific improvement based on the AI's personality.
     */
    fun calculatePerceivedValueWithImprovement(tile: Tile, improvement: TileImprovement, personality: Personality): Int {
        // Example implementation
        val baseValue = calculatePerceivedTileValue(tile, personality)
        val improvementValue = when (improvement.type) {
            // Define how different improvements affect the value
            "Farm" -> 10 * personality.production
            "Mine" -> 15 * personality.science
            else -> 5
        }
        return baseValue + improvementValue
    }
}
