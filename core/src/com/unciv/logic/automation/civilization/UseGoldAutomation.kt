package com.unciv.logic.automation.civilization

import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.INonPerpetualConstruction
import com.unciv.models.ruleset.ResourceType
import com.unciv.models.ruleset.nation.PersonalityValue
import com.unciv.models.stats.Stat
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.ui.notifications.CityAction
import com.unciv.ui.notifications.NotificationCategory
import com.unciv.ui.notifications.NotificationIcon

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
                    val unitsToPurchase = getUnitsToPurchase(civ)
                    for (unit in unitsToPurchase) {
                        if (remainingGold >= unit.cost) {
                            purchaseUnit(civ, unit)
                            remainingGold -= unit.cost
                        }
                        if (remainingGold <= 0) break
                    }
                }
                "City-State Influence" -> {
                    useGoldForCityStates(civ)
                    remainingGold = civ.gold
                    if (remainingGold <= 0) break
                }
                "Tile Expansion" -> {
                    maybeBuyCityTiles(civ)
                    remainingGold = civ.gold
                    if (remainingGold <= 0) break
                }
                "Unit Upgrades" -> {
                    for (unit in civ.units.getCivUnits()) {
                        if (UnitAutomation.tryUpgradeUnit(unit)) {
                            val upgradeCost = unit.getUpgradeCost() ?: continue
                            if (remainingGold >= upgradeCost) {
                                remainingGold -= upgradeCost
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
     * @return A list of spending categories and their respective priorities.
     */
    private fun calculateGoldSpendingPriority(civ: Civilization): List<Pair<String, Float>> {
        val personality = civ.getPersonality()
        val basePriority = mutableMapOf(
            "Military Units" to personality[PersonalityValue.Military],
            "City-State Influence" to ((personality[PersonalityValue.Diplomacy] + personality[PersonalityValue.Commerce]) / 2),
            "Buildings" to ((personality[PersonalityValue.Production] + personality[PersonalityValue.Science] + personality[PersonalityValue.Culture]) / 3),
            "Tile Expansion" to personality[PersonalityValue.Expansion],
            "Unit Upgrades" to personality[PersonalityValue.Military]
        )

        // Adjust priorities if needed
        // For simplicity, we'll sort by the priority values directly
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
            val availableConstructions = city.cityConstructions.getPurchaseableConstructions(Stat.Gold)
            for (construction in availableConstructions) {
                if (construction !is INonPerpetualConstruction) continue
                val statBuyCost = construction.getStatBuyCost(city, Stat.Gold) ?: continue
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
                "[${purchase.city.name}] has purchased [${purchase.construction.name}] for [${purchase.cost}] gold.",
                CityAction.withLocation(purchase.city),
                NotificationCategory.Production,
                NotificationIcon.Construction
            )
        }
    }

    /**
     * Retrieves a list of units that can be purchased with available gold.
     */
    private fun getUnitsToPurchase(civ: Civilization): List<UnitPurchase> {
        val purchasableUnits = mutableListOf<UnitPurchase>()
        for (city in civ.cities.sortedByDescending { it.population.population }) {
            val availableUnits = city.cityConstructions.getPurchaseableConstructions(Stat.Gold)
                .filterIsInstance<BaseUnit>()
                .filter { it.isMilitary }
            for (unit in availableUnits) {
                val statBuyCost = unit.getStatBuyCost(city, Stat.Gold) ?: continue
                purchasableUnits.add(UnitPurchase(unit, city, statBuyCost))
            }
        }
        return purchasableUnits.sortedBy { it.cost }
    }

    /**
     * Data class to hold information about a unit purchase option.
     */
    private data class UnitPurchase(val unit: BaseUnit, val city: City, val cost: Int)

    /**
     * Attempts to purchase a unit.
     */
    private fun purchaseUnit(civ: Civilization, purchase: UnitPurchase) {
        if (purchase.city.cityConstructions.purchaseConstruction(purchase.unit, 0, true)) {
            civ.addNotification(
                "[${purchase.city.name}] has purchased [${purchase.unit.name}] for [${purchase.cost}] gold.",
                CityAction.withLocation(purchase.city),
                NotificationCategory.Production,
                NotificationIcon.Unit
            )
        }
    }

    /**
     * Automate interactions with city-states
     */
    private fun useGoldForCityStates(civ: Civilization) {
        // Code from old UseGoldAutomation.kt adjusted
        if (civ.gold < 250) return
        val knownCityStates = civ.getKnownCivs().filter { it.isCityState }
        for (cityState in knownCityStates) {
            val diploManager = cityState.getDiplomacyManager(civ) ?: continue
            if (diploManager.getInfluence() < 40 && civ.gold >= 250) {
                cityState.cityStateFunctions.receiveGoldGift(civ, 250)
            } else if (civ.gold >= 500) {
                cityState.cityStateFunctions.receiveGoldGift(civ, 500)
            }
            if (civ.gold < 250) break
        }
    }

    /**
     * Automate the purchase of highly desirable city tiles
     */
    private fun maybeBuyCityTiles(civInfo: Civilization) {
        // Code from old UseGoldAutomation.kt
        if (civInfo.gold <= 0) return
        if (civInfo.gameInfo.turns < (civInfo.gameInfo.speed.scienceCostModifier * 20).toInt()) return

        val highlyDesirableTiles: SortedMap<Tile, MutableSet<City>> = getHighlyDesirableTilesToCityMap(civInfo)

        for (highlyDesirableTile in highlyDesirableTiles.keys) {
            val citiesAssociated = highlyDesirableTiles[highlyDesirableTile] ?: continue
            val cityWithLeastCostToBuy = citiesAssociated.minByOrNull {
                it.getCenterTile().aerialDistanceTo(highlyDesirableTile)
            } ?: continue

            val tileCost = cityWithLeastCostToBuy.expansion.getGoldCostOfTile(highlyDesirableTile)
            if (civInfo.gold >= tileCost) {
                cityWithLeastCostToBuy.expansion.buyTile(highlyDesirableTile)
                civInfo.addNotification(
                    "[${cityWithLeastCostToBuy.name}] has expanded to tile [${highlyDesirableTile.position}] for [${tileCost}] gold.",
                    CityAction.withLocation(cityWithLeastCostToBuy),
                    NotificationCategory.Expansion,
                    NotificationIcon.Tile
                )
                civInfo.gold -= tileCost
            }
            if (civInfo.gold <= 0) break
        }
    }

    /**
     * Retrieve a sorted map of highly desirable tiles mapped to their associated cities
     */
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

    /**
     * Determine if a tile is highly desirable for purchase
     */
    private fun isHighlyDesirableTile(tile: Tile, civInfo: Civilization, city: City): Boolean {
        if (!tile.isVisible(civInfo)) return false
        if (tile.getOwner() != null) return false
        if (tile.neighbors.none { neighbor -> neighbor.getCity() == city }) return false

        fun hasNaturalWonder() = tile.naturalWonder != null

        fun hasLuxuryCivDoesntOwn() =
            tile.hasViewableResource(civInfo)
                && tile.tileResource.resourceType == ResourceType.Luxury
                && !civInfo.hasResource(tile.resource!!)

        fun hasResourceCivHasNoneOrLittle() =
            tile.hasViewableResource(civInfo)
                && tile.tileResource.resourceType == ResourceType.Strategic
                && civInfo.getResourceAmount(tile.resource!!) <= 3

        return (hasNaturalWonder() || hasLuxuryCivDoesntOwn() || hasResourceCivHasNoneOrLittle())
    }

    /**
     * Extension function to get the upgrade cost of a unit
     */
    private fun MapUnit.getUpgradeCost(): Int? {
        // Implement logic to determine the upgrade cost
        // Placeholder implementation, adjust as per actual game logic
        val upgradeUnit = this.getUpgradedUnit() ?: return null
        return upgradeUnit.cost - this.baseUnit.cost
    }

    /**
     * Extension function to check if a unit can upgrade
     */
    private fun MapUnit.canUpgrade(): Boolean {
        // Implement logic to check if the unit can upgrade
        // Placeholder implementation, adjust as per actual game logic
        return this.getUpgradedUnit() != null
    }

    /**
     * Extension function to get the upgraded unit
     */
    private fun MapUnit.getUpgradedUnit(): BaseUnit? {
        // Implement logic to retrieve the upgraded unit
        // Placeholder implementation, adjust as per actual game logic
        val upgradeName = this.baseUnit.upgradesTo.firstOrNull() ?: return null
        return this.civ.getRuleset().units[upgradeName]
    }
}
