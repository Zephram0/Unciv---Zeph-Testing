package com.unciv.logic.automation.civilization.purchases.strategies

import com.unciv.logic.automation.civilization.purchases.core.IPurchasingStrategy
import com.unciv.logic.automation.civilization.purchases.core.PurchaseDecisionEngine
import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.logic.map.tile.Tile

object TileStrategy : IPurchasingStrategy {

    override fun evaluatePurchases(civ: Civilization, personality: Personality, victoryType: Victory.Type): List<PurchaseOption> {
        val purchaseOptions = mutableListOf<PurchaseOption>()

        for (city in civ.cities) {
            val tiles = city.getWorkedTiles()
            for (tile in tiles) {
                val resource = tile.resource ?: continue

                if (resource.type == ResourceType.SpecificResource && civ.wantsToFocusOn(Victory.Focus.Science)) {
                    val goldCost = 300
                    if (civ.gold < goldCost) continue

                    val perceivedValue = PurchaseDecisionEngine.calculatePerceivedTileValue(tile, personality)
                    val adjustedValue = PurchaseDecisionEngine.adjustForVictoryFocus(perceivedValue, victoryType, personality)

                    if (PurchaseDecisionEngine.shouldPurchase(adjustedValue, goldCost, civ.gold)) {
                        purchaseOptions.add(
                            PurchaseOption(
                                type = PurchaseOption.PurchaseType.Tile,
                                cost = goldCost,
                                baseValue = adjustedValue.toFloat(),
                                description = "Improve tile at (${tile.x}, ${tile.y})",
                                action = { tile.improve() }
                            )
                        )
                    }
                }
            }
        }

        return purchaseOptions
    }
}