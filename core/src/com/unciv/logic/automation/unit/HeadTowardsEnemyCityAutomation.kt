package com.unciv.logic.automation.unit

import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.logic.battle.BattleDamage
import com.unciv.logic.battle.CityCombatant
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.city.City
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.mapunit.movement.PathsToTilesWithinTurn
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.map.MapPathing

object HeadTowardsEnemyCityAutomation {

    /** @returns whether the unit has taken this action */
    fun tryHeadTowardsEnemyCity(unit: MapUnit): Boolean {
        if (unit.civ.cities.isEmpty()) return false

        // only focus on *attacking* 1 enemy at a time otherwise you'll lose on both fronts
        val closestReachableEnemyCity = getEnemyCitiesByPriority(unit)
            .firstOrNull { unit.movement.canReach(it.getCenterTile()) }
            ?: return false // No enemy city reachable

        return headTowardsEnemyCity(
            unit,
            closestReachableEnemyCity.getCenterTile(),
            // This should be cached after the `canReach` call above.
            unit.movement.getShortestPath(closestReachableEnemyCity.getCenterTile())
        )
    }

    private fun getEnemyCitiesByPriority(unit: MapUnit): Sequence<City> {
        val enemies = unit.civ.getKnownCivs()
            .filter { unit.civ.isAtWarWith(it) && it.cities.isNotEmpty() }

        val closestEnemyCity = enemies
            .mapNotNull { NextTurnAutomation.getClosestCities(unit.civ, it) }
            .minByOrNull { it.aerialDistance }?.city2
            ?: return emptySequence() // no attackable cities found

        // Our main attack target is the closest city, but we're fine with deviating from that a bit
        var enemyCitiesByPriority = closestEnemyCity.civ.cities
            .associateWith { it.getCenterTile().aerialDistanceTo(closestEnemyCity.getCenterTile()) }
            .asSequence().filterNot { it.value > 10 } // anything 10 tiles away from the target is irrelevant
            .sortedBy { it.value }.map { it.key } // sort the list by closeness to target - least is best!

        if (unit.baseUnit.isRanged()) // ranged units don't harm capturable cities, waste of a turn
            enemyCitiesByPriority = enemyCitiesByPriority.filterNot { it.health == 1 }

        return enemyCitiesByPriority
    }


    internal const val maxDistanceFromCityToConsiderForLandingArea = 5
    internal const val minDistanceFromCityToConsiderForLandingArea = 1

    /** @returns whether the unit has taken this action */
    fun headTowardsEnemyCity(
        unit: MapUnit,
        closestReachableEnemyCity: Tile,
        shortestPath: List<Tile>
    ): Boolean {
        val unitDistanceToTiles = unit.movement.getDistanceToTiles()

        val unitRange = unit.getRange()
        if (unitRange > 2) { // long-ranged unit, should never be in a bombardable position
            return headTowardsEnemyCityLongRange(closestReachableEnemyCity, unitDistanceToTiles, unitRange, unit)
        }

        val nextTileInPath = shortestPath[0]

        // None of the stuff below is relevant if we're still quite far away from the city, so we
        // short-circuit here for performance reasons.
        if (unit.currentTile.aerialDistanceTo(closestReachableEnemyCity) > maxDistanceFromCityToConsiderForLandingArea
            // Even in the worst case of only being able to move 1 tile per turn, we would still
            // not overshoot.
            && shortestPath.size > minDistanceFromCityToConsiderForLandingArea ) {
            unit.movement.moveToTile(nextTileInPath)
            return true
        }

        val ourUnitsAroundEnemyCity = closestReachableEnemyCity.getTilesInDistance(6)
            .flatMap { it.getUnits() }
            .filter { it.isMilitary() && it.civ == unit.civ }

        val city = closestReachableEnemyCity.getCity()!!

        if (cannotTakeCitySoon(ourUnitsAroundEnemyCity, city)) {
            return headToLandingGrounds(closestReachableEnemyCity, unit)
        }

        unit.movement.moveToTile(nextTileInPath) // go for it!

        return true
    }

    /** Cannot take within 5 turns */
    private fun cannotTakeCitySoon(
        ourUnitsAroundEnemyCity: Sequence<MapUnit>,
        city: City
    ): Boolean {
        val cityCombatant = CityCombatant(city)
        val expectedDamagePerTurn = ourUnitsAroundEnemyCity
            .sumOf { BattleDamage.calculateDamageToDefender(MapUnitCombatant(it), cityCombatant) }

        val cityHealingPerTurn = 20
        return expectedDamagePerTurn < city.health && // Cannot take immediately
            (expectedDamagePerTurn <= cityHealingPerTurn // No lasting damage
                || city.health / (expectedDamagePerTurn - cityHealingPerTurn) > 5) // Can damage, but will take more than 5 turns
    }

    /**
     * Evaluates if a landing spot is safe for EMBARKED units based on naval threats.
     * 
     * @param tile The potential landing tile to evaluate
     * @param unit The unit attempting to land
     * @return True if the landing spot is deemed safe, false otherwise
     */

     //TODO: Adjust so units will still move to tile if they are already under threat; don't avoid moving if all tiles to move to are also under threat.
     internal fun isSafeLandingSpot(tile: Tile, unit: MapUnit): Boolean {
        val wouldBeEmbarked = !tile.isLand || !unit.movement.canMoveTo(tile)
        if (!wouldBeEmbarked) return true
        
        val navySearchRadius = 8
        val tilesInRange = tile.getTilesInDistance(navySearchRadius)
            .filter { it.isVisible(unit.civ) }
            .toList()
            
        if (tilesInRange.isEmpty()) return false
        
        // Get all units and cities in range once
        val allUnits = tilesInRange.flatMap { it.getUnits() }.groupBy { it.getTile() }
        val nearbyEnemyCity = tilesInRange.firstOrNull { threatTile ->
            threatTile.isCityCenter() && 
            threatTile.getOwner()?.isAtWarWith(unit.civ) == true && 
            threatTile.aerialDistanceTo(tile) <= 3
        }
        
        // Partition units by allegiance and military status at once
        val (enemyMilitary, friendlyMilitary) = allUnits.values
            .flatten()
            .filter { it.isMilitary() }
            .partition { it.civ != unit.civ }
        
        // Count threats within range
        val militaryUnitCount = enemyMilitary.count { enemyUnit ->
            enemyUnit.getTile().aerialDistanceTo(tile) <= enemyUnit.getRange()
        }
        
        // Process friendly units (only check high health)
        val healthyFriendlyUnits = friendlyMilitary.filter { it.health > 80 }
        val (navalSupport, otherSupport) = healthyFriendlyUnits.partition { it.baseUnit.isWaterUnit }
        val highHealthNavalSupport = navalSupport.size
        val highHealthOtherSupport = otherSupport.count { !it.isEmbarked() }
        
        // Early exit if we don't have minimum required support
        if (highHealthNavalSupport < 1 || (highHealthNavalSupport + highHealthOtherSupport) < 3) return false
        
        // If no city nearby, just check military threats
        if (nearbyEnemyCity == null) return militaryUnitCount == 0
        
        // Check military ratio near enemy city using already collected units
        val tilesNearCity = nearbyEnemyCity.getTilesInDistance(3).toSet()
        val enemyMilitaryNearCity = enemyMilitary.count { it.getTile() in tilesNearCity }
        val friendlyMilitaryNearCity = friendlyMilitary.count { it.getTile() in tilesNearCity }
        
        return friendlyMilitaryNearCity > enemyMilitaryNearCity * 3 || militaryUnitCount <= 1
    }

    /**
     * Finds and moves towards safe landing grounds near the target city.
     * Prevents unnecessary naval approaches when land routes are available.
     * 
     * @param closestReachableEnemyCity The target enemy city tile
     * @param unit The unit seeking landing grounds
     * @return True if a landing spot was found and movement initiated
     */
    internal fun headToLandingGrounds(closestReachableEnemyCity: Tile, unit: MapUnit): Boolean {
        // 1. Get potential landing tiles within strategic range of the city
        val candidateTiles = closestReachableEnemyCity
            .getTilesInDistanceRange(minDistanceFromCityToConsiderForLandingArea..maxDistanceFromCityToConsiderForLandingArea)
            .filter { tile -> 
                // 2. Basic landing requirements
                tile.isCoastalTile() &&
                tile.isLand && 
                unit.getDamageFromTerrain(tile) <= 0 &&
                // 3. Check for threats and support
                unit.movement.canMoveTo(tile) &&
                isSafeLandingSpot(tile, unit)
            }
            
        // 4. Find tiles with valid water path
        val tilesWithWaterPath = candidateTiles.filter { tile ->
            MapPathing.getConnection(
                unit.civ,
                unit.currentTile,
                tile,
                { civ, pathTile -> 
                    (pathTile.isWater || pathTile == tile) && 
                    !pathTile.isImpassible() &&
                    civ.hasExplored(pathTile) &&
                    unit.movement.canMoveTo(pathTile)
                }
            ) != null
        }
        if (tilesWithWaterPath.none()) return false
        
        // 5. Find tiles with valid land path to city
        val tilesWithBothPaths = tilesWithWaterPath.filter { tile ->
            MapPathing.getConnection(
                unit.civ,
                tile,
                closestReachableEnemyCity,
                { civ, pathTile -> 
                    pathTile.isLand && 
                    !pathTile.isImpassible() && 
                    civ.hasExplored(pathTile)
                }
            ) != null
        }
        if (tilesWithBothPaths.none()) return false
        
        // 6. Choose closest valid spot
        val landingSpot = tilesWithBothPaths.minByOrNull { it.aerialDistanceTo(unit.currentTile) }
            ?: return false
    
        // 7. Move towards chosen landing spot
        unit.movement.headTowards(landingSpot)
        return true
    }

    private fun headTowardsEnemyCityLongRange(
        closestReachableEnemyCity: Tile,
        unitDistanceToTiles: PathsToTilesWithinTurn,
        unitRange: Int,
        unit: MapUnit
    ): Boolean {
        val tilesInBombardRange = closestReachableEnemyCity.getTilesInDistance(2).toSet()
        val tileToMoveTo =
            unitDistanceToTiles.asSequence()
                .filter {
                    it.key.aerialDistanceTo(closestReachableEnemyCity) <=
                        unitRange && it.key !in tilesInBombardRange
                        && unit.getDamageFromTerrain(it.key) <= 0 // Don't set up on a mountain
                }
                .minByOrNull { it.value.totalDistance }?.key ?: return false // return false if no tile to move to

        // move into position far away enough that the bombard doesn't hurt
        unit.movement.headTowards(tileToMoveTo)
        return true
    }
}
