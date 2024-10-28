package com.unciv.logic.automation.civilization.purchases.tiles

import com.unciv.logic.civilization.Civilization
import com.unciv.logic.city.City
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.tile.ResourceType

object TileEvaluator {

    /**
     * Determines if a tile is highly desirable based on resources and features.
     */
    fun isHighlyDesirableTile(tile: Tile, civInfo: Civilization, city: City): Boolean {
        if (!tile.isVisibleTo(civInfo)) return false
        if (tile.getOwner() != null) return false
        if (tile.neighbors.none { neighbor -> neighbor.getCity() == city }) return false

        val hasNaturalWonder = tile.naturalWonder != null
        val hasLuxuryCivDoesntOwn = tile.hasViewableResource(civInfo) &&
                tile.tileResource.resourceType == ResourceType.Luxury &&
                !civInfo.hasResource(tile.resource!!)

        val hasResourceCivHasNoneOrLittle = tile.hasViewableResource(civInfo) &&
                tile.tileResource.resourceType == ResourceType.Strategic &&
                civInfo.getResourceAmount(tile.resource!!) <= 3

        return hasNaturalWonder || hasLuxuryCivDoesntOwn || hasResourceCivHasNoneOrLittle
    }
}
