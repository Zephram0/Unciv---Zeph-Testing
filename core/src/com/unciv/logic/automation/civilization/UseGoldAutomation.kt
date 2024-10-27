package com.unciv.logic.automation.civilization

import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.BFS
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.INonPerpetualConstruction
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.stats.Stat
import com.unciv.models.ruleset.IConstruction
import com.unciv.models.ruleset.tile.TileImprovement
import java.util.*

object UseGoldAutomation {
    fun useGold(civ: Civilization) {
        val personality = civ.getPersonality()

        for (city in civ.cities.sortedByDescending {
            it.cityConstructions.turnsToConstruction(it.cityConstructions.currentConstructionFromQueue)
        }) {
            val construction = city.cityConstructions.getCurrentConstruction() ?: continue
            if (construction !is INonPerpetualConstruction) continue
            val statBuyCost = construction.getStatBuyCost(city, Stat.Gold) ?: continue
            if (!city.cityConstructions.isConstructionPurchaseAllowed(construction, Stat.Gold, statBuyCost)) continue

            val perceivedValue = calculatePerceivedConstructionValue(construction, city, personality)
            if (shouldPurchase(perceivedValue, statBuyCost, civ.gold)) {
                city.cityConstructions.purchaseConstruction(construction, 0, true)
            }
        }

        val unitsCopy = civ.units.getCivUnits().toList()
        for (unit in unitsCopy) {
            if (personality.military > 5)
                UnitAutomation.tryUpgradeUnit(unit)
        }

        if (civ.isMajorCiv())
            useGoldForCityStates(civ, personality)

        maybeBuyCityTiles(civ, personality)
    }

    private fun shouldBuyTile(tile: Tile, city: City, personality: Personality, civInfo: Civilization): Boolean {
        val perceivedValue = calculatePerceivedTileValue(tile, personality)
        val adjustedValue = adjustForVictoryFocus(perceivedValue, civInfo.victoryFocus, personality)
        return shouldPurchase(adjustedValue, city.expansion.getGoldCostOfTile(tile), civInfo.gold)
    }

    private fun calculatePerceivedConstructionValue(construction: Construction, city: City, personality: Personality): Int {
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

    private fun calculatePerceivedTileValue(tile: Tile, personality: Personality): Int {
        val yields = tile.baseTileInfo.yields
        return calculatePerceivedValueLinear(yields.food, personality.food) +
               calculatePerceivedValueLinear(yields.production, personality.production) +
               calculatePerceivedValueLinear(yields.gold, personality.gold) +
               calculatePerceivedValueLinear(yields.science, personality.science) +
               calculatePerceivedValueLinear(yields.culture, personality.culture) +
               calculatePerceivedValueLinear(yields.faith, personality.faith) +
               calculatePerceivedValueLinear(yields.happiness, personality.happiness)
    }

    private fun adjustForVictoryFocus(value: Int, focus: Victory.Focus, personality: Personality): Int {
        return when (focus) {
            Victory.Focus.Military -> (value * 1.2).toInt()
            Victory.Focus.Culture -> (value * 1.1).toInt()
            Victory.Focus.Science -> (value * 1.3).toInt()
            Victory.Focus.Balanced -> value
            else -> value
        }
    }

    private fun calculatePerceivedValueLinear(rawValue: Int, personalityValue: Int): Int {
        return rawValue * personalityValue
    }

    private fun dynamicThreshold(perceivedValue: Int, maxThreshold: Double = 5.0): Double {
        return maxThreshold - (perceivedValue / 100.0) * (maxThreshold - 1.0)
    }

    private fun shouldPurchase(perceivedValue: Int, goldCost: Int, goldAvailable: Int): Boolean {
        val threshold = dynamicThreshold(perceivedValue)
        return goldAvailable >= goldCost * threshold
    }

    private fun isTileBetterThanCurrent(city: City, newTile: Tile, personality: Personality, civ: Civilization): Boolean {
        val currentTiles = city.workingTiles
        val worstTile = currentTiles.minByOrNull { calculatePerceivedTileValue(it, personality) } ?: return false

        val perceivedValue = if (hasAvailableWorkers(civ, newTile) && canBeImproved(newTile)) {
            maxOfPerceivedValuesOfPossibleImprovements(newTile, personality, civ)
        } else {
            calculatePerceivedTileValue(newTile, personality)
        }

        return perceivedValue > calculatePerceivedTileValue(worstTile, personality)
    }

    private fun maxOfPerceivedValuesOfPossibleImprovements(tile: Tile, personality: Personality, civ: Civilization): Int {
        val possibleImprovements = getPossibleImprovements(tile, civ)
        val perceivedValues = possibleImprovements.map { improvement ->
            val canClear = tile.feature?.clearingTech?.let { civ.hasTech(it) } ?: true
            if (canClear) {
                calculatePerceivedValueWithImprovement(tile, improvement, personality, civ)
            } else {
                0
            }
        }
        return perceivedValues.maxOrNull() ?: calculatePerceivedTileValue(tile, personality)
    }

    private fun getPossibleImprovements(tile: Tile, civ: Civilization): List<TileImprovement> {
        return civ.gameInfo.ruleset.tileImprovements.values.filter { improvement ->
            (improvement.uniqueTo == null || civ.matchesFilter(improvement.uniqueTo!!)) &&
            civ.hasRequiredTech(improvement) &&
            tile.improvementFunctions.canBuildImprovement(improvement, civ)
        }
    }

    private fun calculatePerceivedValueWithImprovement(tile: Tile, improvement: Improvement, personality: Personality, civ: Civilization): Int {
        val yields = tile.baseTileInfo.yields + improvement.yields
        val improvementValue = calculatePerceivedValueLinear(yields.food, personality.food) +
                               calculatePerceivedValueLinear(yields.production, personality.production) +
                               calculatePerceivedValueLinear(yields.gold, personality.gold) +
                               calculatePerceivedValueLinear(yields.science, personality.science) +
                               calculatePerceivedValueLinear(yields.culture, personality.culture) +
                               calculatePerceivedValueLinear(yields.faith, personality.faith) +
                               calculatePerceivedValueLinear(yields.happiness, personality.happiness)
        val removalValue = if (tile.feature?.removalYield?.production != null) {
            calculatePerceivedValueLinear(tile.feature!!.removalYield.production, personality.production)
        } else 0
        return improvementValue + removalValue
    }

    private fun hasAvailableWorkers(civ: Civilization, tile: Tile): Boolean {
        val requiresWorker = tile.resource != null && tile.improvement == null
        if (!requiresWorker) return true
        val idleWorkers = civ.units.getCivUnits().count { it.isWorker && it.isIdle() }
        return idleWorkers > 0
    }

    private fun maybeBuyCityTiles(civInfo: Civilization, personality: Personality) {
        if (civInfo.gold <= 0) return
        if (civInfo.gameInfo.turns < (civInfo.gameInfo.speed.scienceCostModifier * 20).toInt()) return

        val highlyDesirableTiles: SortedMap<Tile, MutableSet<City>> = getHighlyDesirableTilesToCityMap(civInfo)

        for (highlyDesirableTile in highlyDesirableTiles) {
            val cityWithLeastCostToBuy = highlyDesirableTile.value.minBy {
                it.getCenterTile().aerialDistanceTo(highlyDesirableTile.key)
            }
            val bfs = BFS(cityWithLeastCostToBuy.getCenterTile()) {
                it.getOwner() == null || it.owningCity == cityWithLeastCostToBuy
            }
            bfs.stepUntilDestination(highlyDesirableTile.key)
            val tilesThatNeedBuying = bfs.getPathTo(highlyDesirableTile.key).filter {
                it.getOwner() == null && shouldBuyTile(it, cityWithLeastCostToBuy, personality, civInfo)
            }.toList().reversed()

            for (tileThatNeedsBuying in tilesThatNeedBuying) {
                if (!isTileBetterThanCurrent(cityWithLeastCostToBuy, tileThatNeedsBuying, personality, civInfo)) continue

                val goldCostOfTile = cityWithLeastCostToBuy.expansion.getGoldCostOfTile(tileThatNeedsBuying)
                if (civInfo.gold >= goldCostOfTile) {
                    cityWithLeastCostToBuy.expansion.buyTile(tileThatNeedsBuying)
                } else {
                    break
                }
            }
        }
    }

    private fun getHighlyDesirableTilesToCityMap(civInfo: Civilization): SortedMap<Tile, MutableSet<City>> {
        val highlyDesirableTiles: SortedMap<Tile, MutableSet<City>> = TreeMap(
            compareByDescending<Tile?> { it?.naturalWonder != null }
                .thenByDescending { it?.resource != null && it.tileResource.resourceType == ResourceType.Luxury }
                .thenByDescending { it?.resource != null && it.tileResource.resourceType == ResourceType.Strategic }
                .thenBy { it.hashCode() }
        )

        for (city in civInfo.cities.filter { !it.isPuppet && !it.isBeingRazed }) {
            val highlyDesirableTilesInCity = city.tilesInRange.filter {
                isHighlyDesirableTile(it, civInfo, city)
            }
            for (highlyDesirableTileInCity in highlyDesirableTilesInCity) {
                highlyDesirableTiles.getOrPut(highlyDesirableTileInCity) { mutableSetOf() }
                    .add(city)
            }
        }
        return highlyDesirableTiles
    }

    private fun isHighlyDesirableTile(it: Tile, civInfo: Civilization, city: City): Boolean {
        if (!it.isVisible(civInfo)) return false
        if (it.getOwner() != null) return false
        if (it.neighbors.none { neighbor -> neighbor.getCity() == city }) return false

        fun hasNaturalWonder() = it.naturalWonder != null
        fun hasLuxuryCivDoesntOwn() =
            it.hasViewableResource(civInfo)
                && it.tileResource.resourceType == ResourceType.Luxury
                && !civInfo.hasResource(it.resource!!)
        fun hasResourceCivHasNoneOrLittle() =
            it.hasViewableResource(civInfo)
                && it.tileResource.resourceType == ResourceType.Strategic
                && civInfo.getResourceAmount(it.resource!!) <= 3

        return (hasNaturalWonder() || hasLuxuryCivDoesntOwn() || hasResourceCivHasNoneOrLittle())
    }
}
