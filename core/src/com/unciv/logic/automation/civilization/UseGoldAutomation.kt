package com.unciv.logic.automation.civilization

import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.BFS
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.INonPerpetualConstruction
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.Personality
import com.unciv.models.stats.Stat
import java.util.*

object UseGoldAutomation {

    /** 
     * Allows the AI to spend gold on purchasing buildings, units, upgrading units,
     * influencing city-states, and buying city tiles, based on its personality and current state.
     */
    fun useGold(civ: Civilization) {
        // Get the personality traits of the civilization
        val personality = civ.getPersonality()

        // Purchase buildings in cities, prioritizing those with longer construction times
        for (city in civ.cities.sortedByDescending {
            // Sort cities by the number of turns left to complete the current construction
            it.cityConstructions.turnsToConstruction(it.cityConstructions.currentConstructionFromQueue)
        }) {
            // Get the current construction project in the city's queue
            val construction = city.cityConstructions.getCurrentConstruction() ?: continue
            // Skip if the construction is a perpetual construction (e.g., Wealth, Research)
            if (construction !is INonPerpetualConstruction) continue
            // Get the gold cost to buy the construction immediately
            val statBuyCost = construction.getStatBuyCost(city, Stat.Gold) ?: continue
            // Check if the construction can be purchased with gold
            if (!city.cityConstructions.isConstructionPurchaseAllowed(construction, Stat.Gold, statBuyCost)) continue

            // Calculate the perceived value of the construction based on the AI's personality
            val perceivedValue = calculatePerceivedConstructionValue(construction, city, personality)
            // Decide whether to purchase the construction based on its perceived value and cost
            if (shouldPurchase(perceivedValue, statBuyCost, civ.gold)) {
                // Purchase the construction
                city.cityConstructions.purchaseConstruction(construction, 0, true)
            }
        }

        // Upgrade military units if the civilization has a militaristic personality
        val unitsCopy = civ.units.getCivUnits().toList()
        for (unit in unitsCopy) {
            // If the civilization's military trait is above 5
            if (personality.military > 5)
                // Attempt to upgrade the unit
                UnitAutomation.tryUpgradeUnit(unit)
        }

        // Spend gold on influencing city-states if the civilization is a major civ (i.e., not a city-state or barbarian)
        if (civ.isMajorCiv())
            useGoldForCityStates(civ, personality)

        // Purchase city tiles based on the civilization's personality
        maybeBuyCityTiles(civ, personality)
    }

    /**
     * Determines whether the AI should buy a specific tile for a city based on the tile's perceived value,
     * adjusted for the civilization's victory focus and personality, and whether the cost is acceptable.
     *
     * @param tile The tile under consideration.
     * @param city The city that would acquire the tile.
     * @param personality The AI personality influencing decision-making.
     * @param civInfo The civilization information, including gold reserves and victory focus.
     * @return True if the AI decides to purchase the tile; otherwise, false.
     */
    private fun shouldBuyTile(tile: Tile, city: City, personality: Personality, civInfo: Civilization): Boolean {
        // Calculate the perceived value of the tile based on yields and personality
        val perceivedValue = calculatePerceivedTileValue(tile, personality)
        // Adjust the perceived value based on the civilization's victory focus
        val adjustedValue = adjustForVictoryFocus(perceivedValue, civInfo.victoryFocus, personality)
        // Decide whether to purchase the tile based on the adjusted value, cost, and available gold
        return shouldPurchase(adjustedValue, city.expansion.getGoldCostOfTile(tile), civInfo.gold)
    }

    /**
     * Calculates the perceived value of a construction (building or unit) based on the AI's personality.
     *
     * @param construction The construction under consideration.
     * @param city The city where the construction is being built.
     * @param personality The AI personality influencing decision-making.
     * @return An integer representing the perceived value of the construction.
     */
    private fun calculatePerceivedConstructionValue(construction: Construction, city: City, personality: Personality): Int {
        // Get the base cost in gold to purchase the construction
        val baseValue = construction.getStatBuyCost(city, Stat.Gold) ?: return 0
        // Adjust the base value based on the type of construction and personality traits
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
     *
     * @param tile The tile under consideration.
     * @param personality The AI personality influencing decision-making.
     * @return An integer representing the perceived value of the tile.
     */
    private fun calculatePerceivedTileValue(tile: Tile, personality: Personality): Int {
        // Get the base yields of the tile
        val yields = tile.baseTileInfo.yields
        // Sum up the perceived value of each yield type based on personality weights
        return calculatePerceivedValueLinear(yields.food, personality.food) +
               calculatePerceivedValueLinear(yields.production, personality.production) +
               calculatePerceivedValueLinear(yields.gold, personality.gold) +
               calculatePerceivedValueLinear(yields.science, personality.science) +
               calculatePerceivedValueLinear(yields.culture, personality.culture) +
               calculatePerceivedValueLinear(yields.faith, personality.faith) +
               calculatePerceivedValueLinear(yields.happiness, personality.happiness)
    }

    /**
     * Adjusts the perceived value based on the civilization's victory focus.
     *
     * @param value The original perceived value.
     * @param focus The victory focus of the civilization.
     * @param personality The AI personality influencing decision-making.
     * @return The adjusted perceived value.
     */
    private fun adjustForVictoryFocus(value: Int, focus: Victory.Focus, personality: Personality): Int {
        return when (focus) {
            Victory.Focus.Military -> (value * 1.2).toInt()
            Victory.Focus.Culture -> (value * 1.1).toInt()
            Victory.Focus.Science -> (value * 1.3).toInt()
            Victory.Focus.Balanced -> value
            else -> value
        }
    }

    /**
     * Calculates the perceived value of a raw value adjusted by a personality weight.
     *
     * @param rawValue The base value to be adjusted.
     * @param personalityValue The weight from the AI's personality trait.
     * @return The adjusted perceived value.
     */
    private fun calculatePerceivedValueLinear(rawValue: Int, personalityValue: Int): Int {
        return rawValue * personalityValue
    }

    /**
     * Determines a dynamic threshold for making purchase decisions based on the perceived value.
     *
     * @param perceivedValue The perceived value of the item.
     * @param maxThreshold The maximum threshold value (defaults to 5.0).
     * @return The calculated threshold.
     */
    private fun dynamicThreshold(perceivedValue: Int, maxThreshold: Double = 5.0): Double {
        // The higher the perceived value, the lower the threshold, making it more likely to purchase
        return maxThreshold - (perceivedValue / 100.0) * (maxThreshold - 1.0)
    }

    /**
     * Determines whether the AI should make a purchase based on perceived value, cost, and available gold.
     *
     * @param perceivedValue The perceived value of the item.
     * @param goldCost The cost in gold to purchase the item.
     * @param goldAvailable The amount of gold the civilization currently has.
     * @return True if the AI decides to make the purchase; otherwise, false.
     */
    private fun shouldPurchase(perceivedValue: Int, goldCost: Int, goldAvailable: Int): Boolean {
        // Calculate the dynamic threshold
        val threshold = dynamicThreshold(perceivedValue)
        // Return true if the gold available meets or exceeds the adjusted cost threshold
        return goldAvailable >= goldCost * threshold
    }

    /**
     * Determines if a new tile is better than the worst currently worked tile in the city,
     * considering the AI's personality and whether improvements can be made.
     *
     * @param city The city considering purchasing the tile.
     * @param newTile The tile under consideration.
     * @param personality The AI personality influencing decision-making.
     * @param civ The civilization information.
     * @return True if the new tile is better; otherwise, false.
     */
    private fun isTileBetterThanCurrent(city: City, newTile: Tile, personality: Personality, civ: Civilization): Boolean {
        // Get the tiles currently being worked by the city
        val currentTiles = city.workingTiles
        // Find the worst currently worked tile based on perceived value
        val worstTile = currentTiles.minByOrNull { calculatePerceivedTileValue(it, personality) } ?: return false

        // Determine the perceived value of the new tile
        val perceivedValue = if (hasAvailableWorkers(civ, newTile) && canBeImproved(newTile)) {
            // If workers are available and the tile can be improved, consider the best possible improvements
            maxOfPerceivedValuesOfPossibleImprovements(newTile, personality, civ)
        } else {
            // Otherwise, use the base perceived value of the tile
            calculatePerceivedTileValue(newTile, personality)
        }

        // Return true if the new tile is better than the worst currently worked tile
        return perceivedValue > calculatePerceivedTileValue(worstTile, personality)
    }

    /**
     * Calculates the maximum perceived value of a tile with all possible improvements.
     *
     * @param tile The tile under consideration.
     * @param personality The AI personality influencing decision-making.
     * @param civ The civilization information.
     * @return The maximum perceived value considering all possible improvements.
     */
    private fun maxOfPerceivedValuesOfPossibleImprovements(tile: Tile, personality: Personality, civ: Civilization): Int {
        // Get all possible improvements the civilization can build on the tile
        val possibleImprovements = getPossibleImprovements(tile, civ)
        // Calculate the perceived value for each possible improvement
        val perceivedValues = possibleImprovements.map { improvement ->
            // Check if the tile can be cleared (e.g., removing a forest) before improvement
            val canClear = tile.feature?.clearingTech?.let { civ.hasTech(it) } ?: true
            if (canClear) {
                // Calculate the perceived value of the tile with the improvement
                calculatePerceivedValueWithImprovement(tile, improvement, personality, civ)
            } else {
                0
            }
        }
        // Return the maximum perceived value among all improvements
        return perceivedValues.maxOrNull() ?: calculatePerceivedTileValue(tile, personality)
    }

    /**
     * Retrieves a list of tile improvements that the civilization can build on a given tile.
     *
     * @param tile The tile under consideration.
     * @param civ The civilization information.
     * @return A list of possible tile improvements.
     */
    private fun getPossibleImprovements(tile: Tile, civ: Civilization): List<TileImprovement> {
        // Filter improvements based on the civilization's technology and other requirements
        return civ.gameInfo.ruleset.tileImprovements.values.filter { improvement ->
            // Check if the improvement is available to the civilization (e.g., unique improvements)
            (improvement.uniqueTo == null || civ.matchesFilter(improvement.uniqueTo!!)) &&
            // Check if the civilization has the required technology
            civ.hasRequiredTech(improvement) &&
            // Check if the improvement can be built on the tile
            tile.improvementFunctions.canBuildImprovement(improvement, civ)
        }
    }

    /**
     * Calculates the perceived value of a tile with a specific improvement, including any removal yields.
     *
     * @param tile The tile under consideration.
     * @param improvement The improvement to be considered.
     * @param personality The AI personality influencing decision-making.
     * @param civ The civilization information.
     * @return The perceived value of the tile with the improvement.
     */
    private fun calculatePerceivedValueWithImprovement(tile: Tile, improvement: Improvement, personality: Personality, civ: Civilization): Int {
        // Calculate the combined yields of the tile and the improvement
        val yields = tile.baseTileInfo.yields + improvement.yields
        // Calculate the perceived value based on the yields and personality
        val improvementValue = calculatePerceivedValueLinear(yields.food, personality.food) +
                               calculatePerceivedValueLinear(yields.production, personality.production) +
                               calculatePerceivedValueLinear(yields.gold, personality.gold) +
                               calculatePerceivedValueLinear(yields.science, personality.science) +
                               calculatePerceivedValueLinear(yields.culture, personality.culture) +
                               calculatePerceivedValueLinear(yields.faith, personality.faith) +
                               calculatePerceivedValueLinear(yields.happiness, personality.happiness)
        // Include any production yield from removing features (e.g., chopping down a forest)
        val removalValue = if (tile.feature?.removalYield?.production != null) {
            calculatePerceivedValueLinear(tile.feature!!.removalYield.production, personality.production)
        } else 0
        // Return the total perceived value
        return improvementValue + removalValue
    }

    /**
     * Checks if there are available workers to improve a tile.
     *
     * @param civ The civilization checking for available workers.
     * @param tile The tile that needs improvement.
     * @return True if there are idle workers available; otherwise, false.
     */
    private fun hasAvailableWorkers(civ: Civilization, tile: Tile): Boolean {
        // Determine if the tile requires a worker (e.g., has a resource but no improvement)
        val requiresWorker = tile.resource != null && tile.improvement == null
        if (!requiresWorker) return true
        // Count the number of idle worker units
        val idleWorkers = civ.units.getCivUnits().count { it.isWorker && it.isIdle() }
        // Return true if there is at least one idle worker
        return idleWorkers > 0
    }

    /**
     * Determines whether the AI should purchase tiles for its cities based on personality and game state.
     *
     * @param civInfo The civilization information.
     * @param personality The AI personality influencing decision-making.
     */
    private fun maybeBuyCityTiles(civInfo: Civilization, personality: Personality) {
        // Skip if the civilization has no gold
        if (civInfo.gold <= 0) return
        // Skip if the game is in early turns (e.g., before turn 20 adjusted for game speed)
        if (civInfo.gameInfo.turns < (civInfo.gameInfo.speed.scienceCostModifier * 20).toInt()) return

        // Get a map of highly desirable tiles to the cities that can acquire them
        val highlyDesirableTiles: SortedMap<Tile, MutableSet<City>> = getHighlyDesirableTilesToCityMap(civInfo)

        // Iterate over each highly desirable tile
        for (highlyDesirableTile in highlyDesirableTiles) {
            // Find the city closest to the tile
            val cityWithLeastCostToBuy = highlyDesirableTile.value.minBy {
                it.getCenterTile().aerialDistanceTo(highlyDesirableTile.key)
            }
            // Use BFS to find the path from the city to the tile, considering only unowned tiles
            val bfs = BFS(cityWithLeastCostToBuy.getCenterTile()) {
                it.getOwner() == null || it.owningCity == cityWithLeastCostToBuy
            }
            bfs.stepUntilDestination(highlyDesirableTile.key)
            // Get the list of tiles that need to be purchased to reach the desired tile
            val tilesThatNeedBuying = bfs.getPathTo(highlyDesirableTile.key).filter {
                it.getOwner() == null && shouldBuyTile(it, cityWithLeastCostToBuy, personality, civInfo)
            }.toList().reversed()

            // Attempt to purchase each tile in the path
            for (tileThatNeedsBuying in tilesThatNeedBuying) {
                // Skip if the tile is not better than the city's worst worked tile
                if (!isTileBetterThanCurrent(cityWithLeastCostToBuy, tileThatNeedsBuying, personality, civInfo)) continue

                val goldCostOfTile = cityWithLeastCostToBuy.expansion.getGoldCostOfTile(tileThatNeedsBuying)
                // Purchase the tile if the civilization has enough gold
                if (civInfo.gold >= goldCostOfTile) {
                    cityWithLeastCostToBuy.expansion.buyTile(tileThatNeedsBuying)
                } else {
                    // Stop if not enough gold to purchase the tile
                    break
                }
            }
        }
    }

    /**
     * Retrieves a map of highly desirable tiles to the cities that can potentially acquire them.
     *
     * @param civInfo The civilization information.
     * @return A sorted map where keys are tiles and values are sets of cities.
     */
    private fun getHighlyDesirableTilesToCityMap(civInfo: Civilization): SortedMap<Tile, MutableSet<City>> {
        val highlyDesirableTiles: SortedMap<Tile, MutableSet<City>> = TreeMap(
            // Comparator to sort tiles by desirability
            compareByDescending<Tile?> { it?.naturalWonder != null }
                .thenByDescending { it?.resource != null && it.tileResource.resourceType == ResourceType.Luxury }
                .thenByDescending { it?.resource != null && it.tileResource.resourceType == ResourceType.Strategic }
                .thenBy { it.hashCode() }
        )

        // Iterate over the civilization's cities
        for (city in civInfo.cities.filter { !it.isPuppet && !it.isBeingRazed }) {
            // Find highly desirable tiles within the city's acquisition range
            val highlyDesirableTilesInCity = city.tilesInRange.filter {
                isHighlyDesirableTile(it, civInfo, city)
            }
            // Map each highly desirable tile to the city that can acquire it
            for (highlyDesirableTileInCity in highlyDesirableTilesInCity) {
                highlyDesirableTiles.getOrPut(highlyDesirableTileInCity) { mutableSetOf() }
                    .add(city)
            }
        }
        return highlyDesirableTiles
    }

    /**
     * Determines if a tile is highly desirable for the AI to acquire, based on resources and features.
     *
     * @param it The tile under consideration.
     * @param civInfo The civilization information.
     * @param city The city considering acquiring the tile.
     * @return True if the tile is highly desirable; otherwise, false.
     */
    private fun isHighlyDesirableTile(it: Tile, civInfo: Civilization, city: City): Boolean {
        // Skip tiles that are not visible to the civilization
        if (!it.isVisible(civInfo)) return false
        // Skip tiles that are already owned
        if (it.getOwner() != null) return false
        // Skip tiles that are not adjacent to the city's borders
        if (it.neighbors.none { neighbor -> neighbor.getCity() == city }) return false

        // Check if the tile has a natural wonder
        fun hasNaturalWonder() = it.naturalWonder != null

        // Check if the tile has a luxury resource the civ doesn't own yet
        fun hasLuxuryCivDoesntOwn() =
            it.hasViewableResource(civInfo)
                && it.tileResource.resourceType == ResourceType.Luxury
                && !civInfo.hasResource(it.resource!!)

        // Check if the tile has a strategic resource the civ has little of
        fun hasResourceCivHasNoneOrLittle() =
            it.hasViewableResource(civInfo)
                && it.tileResource.resourceType == ResourceType.Strategic
                && civInfo.getResourceAmount(it.resource!!) <= 3

        // Return true if any of the conditions are met
        return (hasNaturalWonder() || hasLuxuryCivDoesntOwn() || hasResourceCivHasNoneOrLittle())
    }
}
