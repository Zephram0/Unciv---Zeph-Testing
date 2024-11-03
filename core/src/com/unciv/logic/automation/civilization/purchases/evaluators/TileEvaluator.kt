package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.map.tile.Tile
import com.unciv.logic.civilization.Civilization

object TileEvaluator {

    fun rankTile(tile: Tile, civ: Civilization): Int {
        // Implement logic to rank tile based on its strategic value
        // Example placeholder logic:
        return when {
            tile.naturalWonder != null -> 100
            tile.tileResource != null -> {
                when (tile.tileResource.resourceType) {
                    ResourceType.Luxury -> 80
                    ResourceType.Strategic -> 90
                    else -> 50
                }
            }
            else -> 30
        }
    }
}
