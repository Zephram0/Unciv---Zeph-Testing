package com.unciv.logic.automation.civilization

import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.ruleset.INonPerpetualConstruction
import com.unciv.models.ruleset.nation.PersonalityValue
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.stats.Stat
import com.unciv.logic.civilization.NotificationCategory
import com.unciv.logic.civilization.NotificationIcon
import com.unciv.logic.civilization.CityAction
import com.unciv.models.ruleset.ResourceType

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
                                val unitName = unit.baseUnit.name ?: "Unknown Unit"
                                civ.addNotification(
                                    "[$unitName] has been upgraded for [$upgradeCost] gold.",
                                    CityAction.withLocation(unit.getTile()?.getCity() ?: civ.getCapitalCity()),
                                    NotificationCategory.Units,
                                    NotificationIcon.Scout // Ensure 'Scout' exists; replace if necessary
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
        val personality = civ.getPersonality() // Assuming getPersonality() returns Map<PersonalityValue, Float>
        val basePriority = mutableMapOf(
            "Military Units" to (personality[PersonalityValue.Militaristic] ?: 0f),
            "City-State Influence" to (((personality[PersonalityValue.Diplomatic] ?: 0f) + (personality[PersonalityValue.Commercial] ?: 0f)) / 2f),
            "Buildings" to (((personality[PersonalityValue.Industrial] ?: 0f) + (personality[PersonalityValue.Scientific] ?: 0f) + (personality[PersonalityValue.Cultural] ?: 0f)) / 3f),
            "Tile Expansion" to (personality[PersonalityValue.Expansive] ?: 0f),
            "Unit Upgrades" to (personality[PersonalityValue.Militaristic] ?: 0f)
        )

        val currentSupply = civ.units.getCivUnitsSize().toFloat()
        val maxSupply = civ.stats.getUnitSupply().toFloat()

        val militaryFocus = (personality[PersonalityValue.Militaristic] ?: 0f) / 10f
        val targetSupplyPercentage = 0.2f + 0.7f * militaryFocus // Scale from 20% to 90% based on personality

        val currentSupplyPercentage = if (maxSupply > 0f) currentSupply / maxSupply else 0f

        val priorityMultiplier = if (currentSupplyPercentage < targetSupplyPercentage) 1.1f else 0.9f

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
            val cityName = purchase.city.name ?: "Unknown City"
            val constructionName = purchase.construction.name ?: "Unknown Construction"
            val notificationText = "[$cityName] has purchased [$constructionName] for [${purchase.cost}] gold."
            civ.addNotification(
                notificationText,
                CityAction.withLocation(purchase.city),
                NotificationCategory.Production,
                NotificationIcon.Construction // Ensure 'Construction' exists; replace if necessary
            )
            civ.addGold(-purchase.cost)
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
                if (!baseUnit.isMilitaryUnit()) continue // Ensure 'isMilitaryUnit()' exists in BaseUnit
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
            val cityName = purchase.city.name ?: "Unknown City"
            val unitName = purchase.construction.name ?: "Unknown Military Unit"
            val notificationText = "[$cityName] has purchased [$unitName] for [${purchase.cost}] gold."
            civ.addNotification(
                notificationText,
                CityAction.withLocation(purchase.city),
                NotificationCategory.Units,
                NotificationIcon.Scout // Ensure 'Scout' exists; replace if necessary
            )
            civ.addGold(-purchase.cost)
        }
    }

    /**
     * Retrieves a list of city-state influence purchases based on available gold.
     */
    private fun getCityStateInfluencePurchases(civ: Civilization): List<InfluencePurchase> {
        val influencePurchases = mutableListOf<InfluencePurchase>()
        val knownCityStates = civ.getKnownCivs().filter { it.isCityState }

        for (cityState in knownCityStates) {
            val diploStatus = civ.diplomacyManager.getDiplomaticStatus(cityState)
            val currentInfluence = diploStatus.influence

            if (currentInfluence < 20 && civ.gold >= 500) {
                influencePurchases.add(InfluencePurchase(cityState, 500))
            } else if (currentInfluence < 40 && civ.gold >= 250) {
                influencePurchases.add(InfluencePurchase(cityState, 250))
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
            purchase.cityState.diplomacyManager.receiveGoldGift(civ, purchase.cost)
            val notificationText = "Gained influence with [${purchase.cityState.name ?: "Unknown City-State"}] via gold gift."
            civ.addNotification(
                notificationText,
                CityAction.withLocation(civ.getCapitalCity()),
                NotificationCategory.Diplomacy,
                NotificationIcon.Diplomacy // Ensure 'Diplomacy' exists; replace if necessary
            )
            civ.addGold(-purchase.cost)
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
            val cityName = expansion.city.name ?: "Unknown City"
            val tilePosition = expansion.tile.position ?: "Unknown Position"
            val notificationText = "[$cityName] has expanded to tile [$tilePosition] for [${expansion.cost}] gold."
            civ.addNotification(
                notificationText,
                CityAction.withLocation(expansion.city),
                NotificationCategory.Cities,
                NotificationIcon.City // Ensure 'City' exists; replace if necessary
            )
            civ.addGold(-expansion.cost)
        }
    }

    /**
     * Retrieves a list of units that can be upgraded.
     */
    private fun getUnitsToUpgrade(civ: Civilization): List<MapUnit> {
        return civ.units.getCivUnits().filter { it.canUpgrade() }.toList()
    }

    private fun MapUnit.canUpgrade(): Boolean {
        return this.getUpgradedUnit() != null
    }

    private fun MapUnit.getUpgradedUnit(): BaseUnit? {
        val upgradeName = this.baseUnit.upgradesTo.firstOrNull() ?: return null
        return this.civ.ruleset.units[upgradeName]
    }

    private fun MapUnit.getUpgradeCost(): Int? {
        val upgradedUnit = this.getUpgradedUnit() ?: return null
        return upgradedUnit.cost - this.baseUnit.cost
    }

    private fun shouldSpendOnTileExpansion(civ: Civilization, city: City): Boolean {
        return city.cityConstructions.canExpand() && civ.gold >= city.expansion.getMinimumGoldCost()
    }

    private fun getMostDesirableTile(city: City): Tile? {
        val desirableTiles = city.tilesInRange.filter { it.isDesirableForExpansion(city.civ) }
        return desirableTiles.maxByOrNull { calculateTileValue(it) }
    }

    private fun Tile.isDesirableForExpansion(civ: Civilization): Boolean {
        if (!isVisible(civ)) return false
        if (getOwner() != null) return false
        return true
    }

    private fun calculateTileValue(tile: Tile): Int {
        var value = 0
        if (tile.naturalWonder != null) value += 50

        tile.resource?.let { resource ->
            val resourceType = resource.resourceType
            value += when (resourceType) {
                ResourceType.Luxury -> 30
                ResourceType.Strategic -> 40
                else -> 20
            }
        }

        return value
    }
}
