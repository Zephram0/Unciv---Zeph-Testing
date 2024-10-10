package com.unciv.logic.automation.civilization

import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.INonPerpetualConstruction
import com.unciv.models.stats.Stat
import com.unciv.logic.city.CityConstructions
import com.unciv.logic.civilization.diplomacy.CityStateFunctions
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.logic.map.BFS
import com.unciv.logic.map.city.CityExpansion
import com.unciv.logic.civilization.diplomacy.DiplomacyManager
import com.unciv.models.ruleset.nation.PersonalityValue
import java.util.*

object UseGoldAutomation {

    /**
     * Main function to automate gold spending for a given civilization.
     * Spending categories are prioritized based on the civilization's personality.
     * @param civ The civilization whose gold is to be spent.
     */
    fun useGold(civ: Civilization) {
        // Calculate spending priorities for different categories
        val spendingPriorities = calculateGoldSpendingPriority(civ)

        var remainingGold = civ.gold

        for (category in spendingPriorities) {
            val (categoryName, priorityValue) = category
            if (remainingGold <= 0) break

            when (categoryName) {
                "Buildings" -> {
                    val buildingsToPurchase = getBuildingsToPurchase(civ)
                    for (building in buildingsToPurchase) {
                        val city = building.city
                        if (remainingGold >= building.cost) {
                            purchaseItem(civ, building, city) // purchaseConstruction() is defined in CityConstructions.kt
                            remainingGold -= building.cost
                        }
                        if (remainingGold <= 0) break
                    }
                }
                "Military Units" -> {
                    val unitsToPurchase = getUnitsToPurchase(civ)
                    for (unit in unitsToPurchase) {
                        val city = unit.city
                        if (remainingGold >= unit.cost) {
                            purchaseItem(civ, unit, city) // purchaseConstruction() is defined in CityConstructions.kt
                            remainingGold -= unit.cost
                        }
                        if (remainingGold <= 0) break
                    }
                }
                "City-State Influence" -> {
                    for (cityState in civ.getKnownCivs().filter { it.isCityState }) { // getKnownCivs() is defined in Civilization.kt
                        val influenceValue = evaluateCityStateInfluencePurchase(civ, cityState)
                        if (remainingGold > influenceValue) {
                            cityState.cityStateFunctions.receiveGoldGift(civ, influenceValue) // receiveGoldGift() is defined in CityStateFunctions.kt
                            remainingGold -= influenceValue
                        }
                        if (remainingGold <= 0) break
                    }
                }
                "Tile Expansion" -> {
                    for (city in civ.cities) {
                        if (shouldSpendOnTileExpansion(civ, city)) {
                            val tileToPurchase = getMostDesirableTile(city) // Implemented below
                            val tileCost = city.expansion.getGoldCostOfTile(tileToPurchase) // getGoldCostOfTile() is defined in CityExpansion.kt
                            if (remainingGold >= tileCost) {
                                city.expansion.buyTile(tileToPurchase) // buyTile() is defined in CityExpansion.kt
                                remainingGold -= tileCost
                            }
                            if (remainingGold <= 0) break
                        }
                    }
                }
                "Unit Upgrades" -> {
                    for (unit in civ.getUpgradableUnits()) { // getUpgradableUnits() is defined in Civilization.kt
                        val upgradeCost = unit.getUpgradeCost() // getUpgradeCost() is defined in Unit.kt
                        if (remainingGold >= upgradeCost) {
                            UnitAutomation.tryUpgradeUnit(unit) // tryUpgradeUnit() is defined in UnitAutomation.kt
                            remainingGold -= upgradeCost
                        }
                        if (remainingGold <= 0) break
                    }
                }
            }
        }
    }

    /**
     * Calculates the spending priority for different categories based on the civilization's personality.
     * Military Units priority is adjusted relative to current and max supply to balance overproduction.
     * @param civ The civilization whose gold spending priority is being calculated.
     * @return A list of spending categories and their respective priorities.
     */
    private fun calculateGoldSpendingPriority(civ: Civilization): List<Pair<String, Float>> {
        val personality = civ.getPersonality() // getPersonality() is defined in Civilization.kt
        val basePriority = mutableMapOf(
            "Military Units" to personality[PersonalityValue.Military],
            "City-State Influence" to ((personality[PersonalityValue.Diplomacy] + personality[PersonalityValue.Commerce]) / 2),
            "Buildings" to ((personality[PersonalityValue.Production] + personality[PersonalityValue.Science] + personality[PersonalityValue.Culture]) / 3),
            "Tile Expansion" to personality[PersonalityValue.Expansion],
            "Unit Upgrades" to personality[PersonalityValue.Military]
        )

        // Calculate current supply percentage and target supply percentage for military units
        val currentSupply = civ.units.civ.units.getCivUnitsSize() // getCivUnitsSize() is defined in UnitManager.kt.toFloat() // civ.units.getCivUnitsSize() // getCivUnitsSize() is defined in UnitManager.kt is defined in UnitsCollection.kt
        val maxSupply = civ.stats.getUnitSupply().toFloat() // getUnitSupply() is defined in Stats.kt

        val militaryFocus = personality[PersonalityValue.Military] / 10f
        val targetSupplyPercentage = 0.2f + 0.7f * militaryFocus // Scale from 20% to 90% based on personality

        val currentSupplyPercentage = if (maxSupply > 0) currentSupply / maxSupply else 0f

        // Adjust military units priority with a priority multiplier
        val priorityMultiplier = if (currentSupplyPercentage < targetSupplyPercentage) 1.1f else 0.9f

        // Apply priority multiplier to Military Units priority
        basePriority["Military Units"] = (basePriority["Military Units"] ?: 0f) * priorityMultiplier

        return basePriority.entries.map { it.key to it.value }.sortedByDescending { it.second }
    }

    /**
     * Gets a list of buildings that can be purchased for the civilization.
     * @param civ The civilization whose buildings are being evaluated.
     * @return A list of buildings available for purchase.
     */
    private fun getBuildingsToPurchase(civ: Civilization): List<Building> {
        val buildingsToPurchase = mutableListOf<Building>()
        for (city in civ.cities.filter { !it.isPuppet && !it.isBeingRazed }) {
            val cityConstructions = city.cityConstructions
            for (building in cityConstructions.getBuildableBuildings()) { // getBuildableBuildings() is defined in CityConstructions.kt
                val value = calculateCityPurchaseValue(city, building)
                if (value > 0) {
                    buildingsToPurchase.add(building)
                }
            }
        }
        return buildingsToPurchase.sortedByDescending { calculateCityPurchaseValue(it.city, it) }
    }

    /**
     * Gets a list of military units that can be purchased for the civilization.
     * @param civ The civilization whose units are being evaluated.
     * @return A list of military units available for purchase.
     */
    private fun getUnitsToPurchase(civ: Civilization): List<MapUnit // MapUnit replaces BaseUnit based on the UnitManager.kt structure> {
        val unitsToPurchase = mutableListOf<MapUnit // MapUnit replaces BaseUnit based on the UnitManager.kt structure>()
        for (city in civ.cities) {
            val units = city.getRuleset().units.values.filter { it.isMilitary && civ.civ.units.canPurchaseUnit($1) // canPurchaseUnit() should be implemented in UnitManager.kt to check if a unit can be purchased } // civ.units.canPurchaseUnit($1) // canPurchaseUnit() should be implemented in UnitManager.kt to check if a unit can be purchased is defined in Civilization.kt
            unitsToPurchase.addAll(units)
        }
        return unitsToPurchase.sortedByDescending { calculateCityPurchaseValue(it.city, it) }
    }

    /**
     * Evaluates the influence purchase value for a given city-state.
     * @param civ The civilization attempting to gain influence.
     * @param cityState The target city-state.
     * @return The calculated influence purchase value.
     */
    private fun evaluateCityStateInfluencePurchase(civ: Civilization, cityState: Civilization): Float {
        val personality = civ.getPersonality() // getPersonality() is defined in Civilization.kt
        val influenceValue = when (cityState.cityStateType) {
            "Cultural" -> personality[PersonalityValue.Culture]
            "Maritime" -> personality[PersonalityValue.Food]
            "Mercantile" -> personality[PersonalityValue.Happiness]
            "Militaristic" -> personality[PersonalityValue.Military]
            "Religious" -> personality[PersonalityValue.Faith]
            else -> 0f
        }
        return influenceValue * 250 // Adjusted influence cost value
    }

    /**
     * Determines whether the civilization should spend gold on tile expansion for a city.
     * @param civ The civilization attempting tile expansion.
     * @param city The city being considered for tile expansion.
     * @return True if tile expansion is warranted, false otherwise.
     */
    private fun shouldSpendOnTileExpansion(civ: Civilization, city: City): Boolean {
        val desirableTiles = getHighlyDesirableTiles(city) // Implemented below
        return desirableTiles.isNotEmpty() && city.cityStats.currentCityStats.production < civ.cities.map { it.cityStats.currentCityStats.production }.average()
    }

    /**
     * Gets the most desirable tile for a city to expand to.
     * @param city The city being evaluated.
     * @return The most desirable tile for expansion.
     */
    private fun getMostDesirableTile(city: City): Tile {
        val desirableTiles = getHighlyDesirableTiles(city)
        return desirableTiles.maxByOrNull { it.tileValue } ?: throw IllegalStateException("No desirable tiles found.")
    }

    /**
     * Gets a list of highly desirable tiles for a city.
     * @param city The city being evaluated.
     * @return A list of highly desirable tiles.
     */
    private fun getHighlyDesirableTiles(city: City): List<Tile> {
        return city.tilesInRange.filter {
            it.getOwner() == null && it.isVisible(city.civ) &&
                    (it.naturalWonder != null || it.hasLuxuryResource(city.civ) || it.hasStrategicResource(city.civ))
        }
    }

    /**
     * Determines if a tile has a luxury resource the civilization does not own.
     * @param tile The tile being evaluated.
     * @param civ The civilization attempting to acquire the tile.
     * @return True if the tile has a luxury resource the civilization does not own, false otherwise.
     */
    private fun Tile.hasLuxuryResource(civ: Civilization): Boolean {
        return this.hasViewableResource(civ) && this.tileResource.resourceType == ResourceType.Luxury && !civ.hasResource(this.resource!!)
    }

    /**
     * Determines if a tile has a strategic resource the civilization has little to none of.
     * @param tile The tile being evaluated.
     * @param civ The civilization attempting to acquire the tile.
     * @return True if the tile has a strategic resource the civilization lacks, false otherwise.
     */
    private fun Tile.hasStrategicResource(civ: Civilization): Boolean {
        return this.hasViewableResource(civ) && this.tileResource.resourceType == ResourceType.Strategic && civ.getResourceAmount(this.resource!!) <= 3
    }

    /**
     * Calculates the purchase value of an item for a city.
     * Higher value for low-production cities as purchases are immediate.
     * @param city The city where the item will be purchased.
     * @param item The item to be purchased.
     * @return The calculated purchase value.
     */
    private fun calculateCityPurchaseValue(city: City, item: IConstruction): Float {
        val averageProduction = city.civ.cities.map { it.cityStats.currentCityStats.production }.average()
        val productionValue = if (city.cityStats.currentCityStats.production < averageProduction) {
            2.0f // Higher value for cities with low production
        } else {
            1.0f
        }
        val personalityModifier = city.civ.getPersonality().modifierFocus(item.statFocus ?: Stat.Production, 0.5f) // modifierFocus() is defined in Personality.kt
        return productionValue * personalityModifier
    }

    /**
     * Placeholder for purchasing an item for the city.
     * @param civ The civilization making the purchase.
     * @param item The item being purchased.
     * @param city The city where the item will be purchased.
     */
    private fun purchaseItem(civ: Civilization, item: IConstruction, city: City) {
        city.cityConstructions.purchaseConstruction(item.name, -1, true) // purchaseConstruction() is defined in CityConstructions.kt
    }
}
