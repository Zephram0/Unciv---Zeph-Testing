package com.unciv.logic.automation.civilization.purchases.decision

import com.unciv.logic.city.City
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.tile.TileImprovement
import com.unciv.models.stats.Stat
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.Construction

object ValueCalculator {

    /**
     * Calculates the perceived value of a construction (building or unit) based on the AI's personality.
     */
    fun calculatePerceivedConstructionValue(construction: Construction, city: City, personality: Personality): Int {
        val baseValue = construction.getStatBuyCost(city, Stat.Gold) ?: return 0
        return when (construction.getStat()) {
            Stat.Science -> calculatePerceivedValueLinear(baseValue, personality.science)
            Stat.Production -> calculatePerceivedValueLinear(baseValue, personality.production)
            Stat.Culture -> calculatePerceivedValueLinear(baseValue, personality.culture)
            Stat.Faith -> calculatePerceivedValueLinear(baseValue, personality.faith)
            Stat.Happiness -> calculatePerceivedValueLinear(baseValue, personality.happiness)
            else -> baseValue
        }
    }

    /**
     * Calculates the perceived value of a tile based on its yields and the AI's personality.
     */
    fun calculatePerceivedTileValue(tile: Tile, personality: Personality): Int {
        val yields = tile.getBaseTileInfo().yields
        return calculatePerceivedValueLinear(yields.food, personality.food) +
               calculatePerceivedValueLinear(yields.production, personality.production) +
               calculatePerceivedValueLinear(yields.gold, personality.gold) +
               calculatePerceivedValueLinear(yields.science, personality.science) +
               calculatePerceivedValueLinear(yields.culture, personality.culture) +
               calculatePerceivedValueLinear(yields.faith, personality.faith) +
               calculatePerceivedValueLinear(yields.happiness, personality.happiness)
    }

    /**
     * Calculates the perceived value of a tile with a specific improvement.
     */
    fun calculatePerceivedValueWithImprovement(
        tile: Tile,
        improvement: TileImprovement,
        personality: Personality
    ): Int {
        val yields = tile.getBaseTileInfo().yields + improvement.yields
        val improvementValue = calculatePerceivedValueLinear(yields.food, personality.food) +
                               calculatePerceivedValueLinear(yields.production, personality.production) +
                               calculatePerceivedValueLinear(yields.gold, personality.gold) +
                               calculatePerceivedValueLinear(yields.science, personality.science) +
                               calculatePerceivedValueLinear(yields.culture, personality.culture) +
                               calculatePerceivedValueLinear(yields.faith, personality.faith) +
                               calculatePerceivedValueLinear(yields.happiness, personality.happiness)
        val removalValue = tile.feature?.removalYield?.production?.let {
            calculatePerceivedValueLinear(it, personality.production)
        } ?: 0
        return improvementValue + removalValue
    }

    /**
     * Helper function to calculate perceived value linearly based on personality.
     */
    fun calculatePerceivedValueLinear(rawValue: Int, personalityValue: Int): Int {
        return rawValue * personalityValue
    }
}
