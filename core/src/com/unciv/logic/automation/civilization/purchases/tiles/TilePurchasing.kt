package com.unciv.logic.automation.civilization.purchases.tiles

import com.unciv.logic.automation.civilization.purchases.decision.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.decision.PurchaseDecisionEngine
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.city.City
import com.unciv.logic.map.BFSx
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.nation.Personality
import java.util.SortedMap
import java.util.TreeMap

object TilePurchasing : IPurchasingStrategy {

    override fun execute(civ: Civilization, personality: Personality) {
        maybeBuyCityTiles(civ, personality)
    }

    /**
     * Determines whether the AI should purchase tiles for its cities based on personality and game state.
     */
    fun maybeBuyCityTiles(civInfo: Civilization, personality: Personality) {
        if (civInfo.gold <= 0) return
        if (civInfo.gameInfo.turns < (civInfo.gameInfo.speed.scienceCostModifier * 20).toInt()) return

        val highlyDesirableTiles = getHighlyDesirableTilesToCityMap(civInfo)

        for ((tile, cities) in highlyDesirableTiles) {
            val cityWithLeastCostToBuy = cities.minByOrNull {
                it.getCenterTile().aerialDistanceTo(tile)
            } ?: continue

            val bfs = BFS(cityWithLeastCostToBuy.getCenterTile()) {
                it.getOwner() == null || it.owningCity == cityWithLeastCostToBuy
            }
            bfs.stepUntilDestination(tile)
            val tilesThatNeedBuying = bfs.getPathTo(tile).filter {
                it.getOwner() == null && PurchaseDecisionEngine.shouldBuyTile(it, cityWithLeastCostToBuy, personality, civInfo)
            }.toList().reversed()

            for (tileThatNeedsBuying in tilesThatNeedBuying) {
                if (!PurchaseDecisionEngine.isTileBetterThanCurrent(cityWithLeastCostToBuy, tileThatNeedsBuying, personality, civInfo)) continue

                val goldCostOfTile = cityWithLeastCostToBuy.expansion.getGoldCostOfTile(tileThatNeedsBuying)
                if (civInfo.gold >= goldCostOfTile) {
                    cityWithLeastCostToBuy.expansion.buyTile(tileThatNeedsBuying)
                    civInfo.gold -= goldCostOfTile
                } else {
                    break
                }
            }
        }
    }

    /**
     * Retrieves a map of highly desirable tiles to the cities that can potentially acquire them.
     */
    private fun getHighlyDesirableTilesToCityMap(civInfo: Civilization): SortedMap<Tile, MutableSet<City>> {
        val highlyDesirableTiles: SortedMap<Tile, MutableSet<City>> = TreeMap(
            compareByDescending<Tile?> { it?.naturalWonder != null }
                .thenByDescending { it?.resource != null && it.tileResource.resourceType == ResourceType.Luxury }
                .thenByDescending { it?.resource != null && it.tileResource.resourceType == ResourceType.Strategic }
                .thenBy { it.hashCode() }
        )

        for (city in civInfo.cities.filter { !it.isPuppet() && !it.isBeingRazed() }) {
            val highlyDesirableTilesInCity = city.tilesInRange.filter {
                TileEvaluator.isHighlyDesirableTile(it, civInfo, city)
            }
            for (tile in highlyDesirableTilesInCity) {
                highlyDesirableTiles.getOrPut(tile) { mutableSetOf() }.add(city)
            }
        }
        return highlyDesirableTiles
    }
}
