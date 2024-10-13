package com.unciv.logic.automation.civilization

import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.ruleset.INonPerpetualConstruction
import com.unciv.models.ruleset.ResourceType
import com.unciv.models.ruleset.nation.PersonalityValue
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.stats.Stat
import com.unciv.logic.civilization.NotificationCategory
import com.unciv.logic.civilization.NotificationIcon
import com.unciv.logic.civilization.CityAction

object UseGoldAutomation {

    /** Automate the spending of gold for a given civilization */
    fun useGold(civ: Civilization) {
        // Calculate spending priorities for different categories
        val spendingPriorities = calculateGoldSpendingPriority(civ)

        var remainingGold = civ.gold

        for ((categoryName, _) in spendingPriorities) {
            if (remainingGold <= 0) break

            when (categoryName) {
                "Buildings" -> {
                    val buildingsToPurchase = getBuildingsToPurchase(civ)
                    for (building in buildingsToPurchase) {
                        if (remainingGold >= building.cost) {
                            purchaseBuilding(civ, building)
                            remainingGold -= building.cost
                        }
                        if (remainingGold <= 0) break
                    }
                }
                "Military Units" -> {
                    val militaryUnitsToPurchase = getMilitaryUnitsToPurchase(civ)
                    for (unit in militaryUnitsToPurchase) {
                        if (remainingGold >= unit.cost) {
                            purchaseMilitaryUnit(civ, unit)
                            remainingGold -= unit.cost
                        }
                        if (remainingGold <= 0) break
                    }
                }
                "City-State Influence" -> {
                    val influencePurchases = getCityStateInfluencePurchases(civ)
                    for (influence in influencePurchases) {
                        if (remainingGold >= influence.cost) {
                            purchaseCityStateInfluence(civ, influence)
                            remainingGold -= influence.cost
                        }
                        if (remainingGold <= 0) break
                    }
                }
                "Tile Expansion" -> {
                    val tileExpansions = getTileExpansions(civ)
                    for (expansion in tileExpansions) {
                        if (remainingGold >= expansion.cost) {
                            purchaseTileExpansion(civ, expansion)
                            remainingGold -= expansion.cost
                        }
                        if (remainingGold <= 0) break
                    }
                }
                "Unit Upgrades" -> {
                    val unitsToUpgrade = getUnitsToUpgrade(civ)
                    for (unit in unitsToUpgrade) {
                        val upgradeCost = unit.getUpgradeCost() ?: continue
                        if (remainingGold >= upgradeCost) {
                            if (UnitAutomation.tryUpgradeUnit(unit)) {
                                remainingGold -= upgradeCost
                                civ.addNotification(
                                    "[${unit.baseUnit.name ?: "Unknown Unit"}] has been upgraded for [${upgradeCost}] gold.",
                                    CityAction.withLocation(unit.getTile()?.getCity() ?: civ.capitalCity),
                                    NotificationCategory.Units,
                                    NotificationIcon.Scout
                                )
                            }
                        }
                        if (remainingGold <= 0) break
                    }
                }
            }
        }
    }

    /**
     * Calculates the spending priority for different categories based on the civilization's personality.
     * @param civ The civilization whose gold spending priority is being calculated.
     * @return A list of spending categories and their respective priorities, sorted descendingly.
     */
    private fun calculateGoldSpendingPriority(civ: Civilization): List<Pair<String, Float>> {
        val personality = civ.personality
        val basePriority = mutableMapOf(
            "Military Units" to personality[PersonalityValue.Military],
            "City-State Influence" to ((personality[PersonalityValue.Diplomacy] + personality[PersonalityValue.Commerce]) / 2),
            "Buildings" to ((personality[PersonalityValue.Production] + personality[PersonalityValue.Science] + personality[PersonalityValue.Culture]) / 3),
            "Tile Expansion" to personality[PersonalityValue.Expansion],
            "Unit Upgrades" to personality[PersonalityValue.Military]
        )

        // Adjust Military Units priority based on current and target supply
        val currentSupply = civ.units.getCivUnitsSize().toFloat()
        val maxSupply = civ.stats.getUnitSupply().toFloat()

        val militaryFocus = personality[PersonalityValue.Military] / 10f
        val targetSupplyPercentage = 0.2f + 0.7f * militaryFocus // Scale from 20% to 90% based on personality

        val currentSupplyPercentage = if (maxSupply > 0) currentSupply / maxSupply else 0f

        // Adjust priority multiplier
        val priorityMultiplier = if (currentSupplyPercentage < targetSupplyPercentage) 1.1f else 0.9f

        // Apply priority multiplier to Military Units priority
        basePriority["Military Units"] = (basePriority["Military Units"] ?: 0f) * priorityMultiplier

        return basePriority.entries
            .map { it.key to it.value }
            .sortedByDescending { it.second }
    }

    /**
     * Retrieves a list of buildings that can be purchased with available gold.
     */
    private fun getBuildingsToPurchase(civ: Civilization): List<BuildingPurchase> {
        val purchasableBuildings = mutableListOf<BuildingPurchase>()
        for (city in civ.cities.sortedByDescending { it.population.population }) {
            val constructions = city.cityConstructions.getPurchasableConstructions(Stat.Gold)
            for (construction in constructions) {
                if (construction !is INonPerpetualConstruction) continue
                val statBuyCost = construction.getStatBuyCost(city, Stat.Gold) ?: continue
                if (!city.cityConstructions.isConstructionPurchaseAllowed(construction, Stat.Gold, statBuyCost)) continue
                purchasableBuildings.add(BuildingPurchase(construction, city, statBuyCost))
            }
        }
        return purchasableBuildings.sortedBy { it.cost }
    }

    /**
     * Data class to hold information about a building purchase option.
     */
    private data class BuildingPurchase(val construction: INonPerpetualConstruction, val city: City, val cost: Int)

    /**
     * Attempts to purchase a building.
     */
    private fun purchaseBuilding(civ: Civilization, purchase: BuildingPurchase) {
        if (purchase.city.cityConstructions.purchaseConstruction(purchase.construction, 0, true)) {
            civ.addNotification(
                "[${purchase.city.name ?: "Unknown City"}] has purchased [${purchase.construction.name ?: "Unknown Construction"}] for [${purchase.cost}] gold.",
                CityAction.withLocation(purchase.city),
                NotificationCategory.Production,
                NotificationIcon.Construction
            )
            civ.addGold(-purchase.cost) // Deduct gold
        }
    }

    /**
     * Retrieves a list of military units that can be purchased with available gold.
     */
    private fun getMilitaryUnitsToPurchase(civ: Civilization): List<MilitaryUnitPurchase> {
        val purchasableUnits = mutableListOf<MilitaryUnitPurchase>()
        for (city in civ.cities.sortedByDescending { it.population.population }) {
            val constructions = city.cityConstructions.getPurchasableConstructions(Stat.Gold)
            for (construction in constructions) {
                if (construction !is INonPerpetualConstruction) continue
                val baseUnit = civ.ruleset.units[construction.name] ?: continue
                if (!baseUnit.isMilitaryUnit()) continue // Only military units
                val cost = construction.getStatBuyCost(city, Stat.Gold) ?: continue
                if (!city.cityConstructions.isConstructionPurchaseAllowed(construction, Stat.Gold, cost)) continue
                purchasableUnits.add(MilitaryUnitPurchase(construction, city, cost))
            }
        }
        return purchasableUnits.sortedBy { it.cost }
    }

    /**
     * Data class to hold information about a military unit purchase option.
     */
    private data class MilitaryUnitPurchase(val construction: INonPerpetualConstruction, val city: City, val cost: Int)

    /**
     * Attempts to purchase a military unit.
     */
    private fun purchaseMilitaryUnit(civ: Civilization, purchase: MilitaryUnitPurchase) {
        if (purchase.city.cityConstructions.purchaseConstruction(purchase.construction, 0, true)) {
            civ.addNotification(
                "[${purchase.city.name ?: "Unknown City"}] has purchased [${purchase.construction.name ?: "Unknown Military Unit"}] for [${purchase.cost}] gold.",
                CityAction.withLocation(purchase.city),
                NotificationCategory.Production,
                NotificationIcon.Scout
            )
            civ.addGold(-purchase.cost) // Deduct gold
        }
    }

    /**
     * Retrieves a list of city-state influence purchases based on available gold.
     */
    private fun getCityStateInfluencePurchases(civ: Civilization): List<InfluencePurchase> {
        val influencePurchases = mutableListOf<InfluencePurchase>()
        val knownCityStates = civ.getKnownCivs().filter { it.isCityState }

        for (cityState in knownCityStates) {
            val diploManager = civ.diplomacyManager.getDiplomaticStatus(cityState)
            val currentInfluence = diploManager.influence

            if (currentInfluence < 20 && civ.gold >= 500) {
                influencePurchases.add(InfluencePurchase(cityState, 500)) // Example cost
            } else if (currentInfluence < 40 && civ.gold >= 250) {
                influencePurchases.add(InfluencePurchase(cityState, 250)) // Example cost
            }
        }
        return influencePurchases.sortedBy { it.cost }
    }

    /**
     * Data class to hold information about a city-state influence purchase option.
     */
    private data class InfluencePurchase(val cityState: Civilization, val cost: Int)

    /**
     * Attempts to purchase influence with a city-state.
     */
    private fun purchaseCityStateInfluence(civ: Civilization, purchase: InfluencePurchase) {
        if (civ.gold >= purchase.cost) {
            purchase.cityState.cityStateInfo.receiveGoldGift(civ, purchase.cost)
            civ.addNotification(
                "Gained influence with [${purchase.cityState.name ?: "Unknown City-State"}] via gold gift.",
                CityAction.withLocation(civ.capitalCity),
                NotificationCategory.Diplomacy,
                NotificationIcon.Diplomacy
            )
            civ.addGold(-purchase.cost) // Deduct gold
        }
    }

    /**
     * Retrieves a list of tile expansions that can be purchased with available gold.
     */
    private fun getTileExpansions(civ: Civilization): List<TileExpansion> {
        val tileExpansions = mutableListOf<TileExpansion>()
        for (city in civ.cities) {
            if (shouldSpendOnTileExpansion(civ, city)) {
                val tileToPurchase = getMostDesirableTile(city) ?: continue
                val tileCost = city.expansion.getGoldCostOfTile(tileToPurchase)
                tileExpansions.add(TileExpansion(city, tileToPurchase, tileCost))
            }
        }
        return tileExpansions.sortedBy { it.cost }
    }

    /**
     * Data class to hold information about a tile expansion purchase option.
     */
    private data class TileExpansion(val city: City, val tile: Tile, val cost: Int)

    /**
     * Attempts to purchase a tile expansion.
     */
    private fun purchaseTileExpansion(civ: Civilization, expansion: TileExpansion) {
        if (civ.gold >= expansion.cost) {
            expansion.city.expansion.buyTile(expansion.tile)
            civ.addNotification(
                "[${expansion.city.name ?: "Unknown City"}] has expanded to tile [${expansion.tile.position}] for [${expansion.cost}] gold.",
                CityAction.withLocation(expansion.city),
                NotificationCategory.Cities,
                NotificationIcon.City
            )
            civ.addGold(-expansion.cost) // Deduct gold
        }
    }

    /**
     * Retrieves a list of units that can be upgraded.
     */
    private fun getUnitsToUpgrade(civ: Civilization): List<MapUnit> {
        return civ.units.getCivUnits().filter { it.canUpgrade() }.toList()
    }

    /**
     * Extension function to check if a unit can upgrade.
     */
    private fun MapUnit.canUpgrade(): Boolean {
        return this.getUpgradedUnit() != null
    }

    /**
     * Extension function to get the upgraded unit.
     */
    private fun MapUnit.getUpgradedUnit(): BaseUnit? {
        val upgradeName = this.baseUnit.upgradesTo.firstOrNull() ?: return null
        return this.civ.ruleset.units[upgradeName]
    }

    /**
     * Extension function to get the upgrade cost of a unit.
     */
    private fun MapUnit.getUpgradeCost(): Int? {
        val upgradedUnit = this.getUpgradedUnit() ?: return null
        return upgradedUnit.cost - this.baseUnit.cost
    }

    /**
     * Determines whether the civilization should spend gold on tile expansion for a given city.
     */
    private fun shouldSpendOnTileExpansion(civ: Civilization, city: City): Boolean {
        return city.cityConstructions.canExpand() && civ.gold >= city.expansion.getMinimumGoldCost()
    }

    /**
     * Retrieves the most desirable tile for a city to purchase based on predefined criteria.
     */
    private fun getMostDesirableTile(city: City): Tile? {
        val desirableTiles = city.tilesInRange.filter { it.isDesirableForExpansion(city.civ) }
        return desirableTiles.maxByOrNull { calculateTileValue(it) }
    }

    /**
     * Extension function to determine if a tile is desirable for city expansion.
     */
    private fun Tile.isDesirableForExpansion(civ: Civilization): Boolean {
        if (!isVisible(civ)) return false
        if (getOwner() != null) return false
        return true
    }

    /**
     * Calculates the desirability value of a tile based on existing properties.
     */
    private fun calculateTileValue(tile: Tile): Int {
        var value = 0
        // Assign higher value to natural wonders
        if (tile.naturalWonder != null) value += 50

        // Assign value based on resource type
        tile.resource?.let {
            val resource = tile.tileResource
            value += when (resource.resourceType) {
                ResourceType.Luxury -> 30
                ResourceType.Strategic -> 40
                else -> 20
            }
        }

        return value
    }
}
