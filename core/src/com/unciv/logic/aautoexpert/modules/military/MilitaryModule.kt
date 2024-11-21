package com.unciv.logic.aautoexpert.modules.military

import com.unciv.logic.aautoexpert.modules.core.AIModule
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.battle.BattleDamage
import com.unciv.logic.battle.MapUnitCombatant  
import com.unciv.logic.automation.unit.BattleHelper
import com.unciv.logic.city.City
import com.unciv.logic.battle.TargetHelper

/**
 * Handles military unit positioning and combat decisions
 */
class MilitaryModule : AIModule {

    private fun MapUnit.isRanged() = baseUnit.rangedStrength > 0
    private fun MapUnit.isMelee() = baseUnit.strength > 0 && !isRanged()
    private fun MapUnit.canMove() = currentMovement > 0

    override fun processDecisions(civInfo: Civilization) {
        val militaryUnits = civInfo.units.getCivUnits().filter { it.isMilitary() }
        
        // Process units by priority (ranged first, then melee)
        val sortedUnits = militaryUnits.sortedBy { getUnitPriority(it) }
        
        for (unit in sortedUnits) {
            processUnitDecision(unit)
        }
    }

    private fun getUnitPriority(unit: MapUnit): Int {
        return when {
            unit.isRanged() -> 1  // Ranged units move first
            unit.isMelee() -> 2   // Melee units follow
            else -> 3             // Other military units last
        }
    }

    private fun processUnitDecision(unit: MapUnit) {
        // Skip if unit can't move
        if (!unit.canMove()) return

        when {
            unit.isRanged() -> handleRangedUnit(unit)
            unit.isMelee() -> handleMeleeUnit(unit)
            else -> handleOtherMilitaryUnit(unit)
        }
    }

    /**
     * Handles ranged unit decision making and actions
     * Priority order:
     * 1. Attack high-value targets if safe
     * 2. Move to optimal position
     * 3. Garrison if needed
     */
    private fun handleRangedUnit(unit: MapUnit) {
        // Skip if unit can't act
        if (!unit.hasMovement() || unit.isFortified()) return
        
        // Check if healing is needed first
        if (unit.health < 50 && tryHealUnit(unit)) return
        
        // Get potential targets in range
        val potentialTargets = unit.getTile()
            .getTilesInDistance(unit.getRange())
            .flatMap { it.getUnits() }
            .filter { TargetHelper.getAttackableEnemies(unit, unit.movement.getDistanceToTiles())
                .any { target -> target.tileToAttack == it.getTile() } && it.civ != unit.civ }
            .toList()
        
        // If we have targets, evaluate and attack
        if (potentialTargets.isNotEmpty()) {
            val bestTarget = evaluateRangedTargets(unit, potentialTargets)
            if (bestTarget != null) {
                BattleHelper.tryAttackNearbyEnemy(unit, false)  // Changed from bestTarget.getTile()
                return
            }
        }
        
        // If no good attack, find optimal position
        val optimalPosition = findOptimalRangedPosition(unit)
        if (optimalPosition != null && optimalPosition != unit.getTile()) {
            unit.movement.headTowards(optimalPosition)
            return
        }
        
        // If no better position, consider garrison
        if (shouldGarrison(unit)) {
            val garrisonCity = findGarrisonCity(unit)
            if (garrisonCity != null) {
                unit.movement.headTowards(garrisonCity.getCenterTile())
                return
            }
        }
    }

    private fun handleMeleeUnit(unit: MapUnit) {
        // 1. Check for adjacent enemies
        // 2. If none, move to protect ranged units or strategic positions
        // TODO: Implement melee unit tactics
    }

    private fun handleOtherMilitaryUnit(unit: MapUnit) {
        // Handle other types of military units
        // TODO: Implement other unit tactics
    }

    private fun evaluateRangedTargets(unit: MapUnit, targets: List<MapUnit>): MapUnit? {
        // Score each target based on multiple factors
        return targets.maxByOrNull { target ->
            var score = 0f
            
            // Base damage we can do
            val expectedDamage = BattleDamage.calculateDamageToDefender(
                MapUnitCombatant(unit),
                MapUnitCombatant(target)
            ).toFloat()
            score += expectedDamage * 2  // Damage is a primary factor
            
            // Strategic value of target
            score += when {
                target.baseUnit.isProbablySiegeUnit() -> 30f  // High priority
                target.baseUnit.isRanged() -> 25f            // Also dangerous
                target.baseUnit.isMelee() -> 20f             // Standard priority
                else -> 15f                                  // Lower priority
            }
            
            // Adjust for target's health
            score *= (1f + (100f - target.health) / 100f)  // Prefer weakened targets
            
            // Reduce score if target can counterattack
            if (target.baseUnit.isRanged() && 
                target.getTile().aerialDistanceTo(unit.getTile()) <= target.getRange()) {
                score *= 0.7f
            }
            
            score
        }
    }
    
    private fun findOptimalRangedPosition(unit: MapUnit): Tile? {
        val currentTile = unit.getTile()
        val reachableTiles = unit.movement.getDistanceToTiles().keys
        
        return reachableTiles.maxByOrNull { tile ->
            var score = 0f
            
            // Base position score
            score += evaluateDefensivePosition(tile)
            
            // Attacking potential from this position
            val tilesInRange = tile.getTilesInDistance(unit.getRange())
            // Get enemy units in attack range from this position
            val enemyUnitsInRange = tile.getTilesInDistance(unit.getRange())
                .flatMap { it.getUnits() }.filter { it.civ != unit.civ }.toList()
            score += if (enemyUnitsInRange.isNotEmpty()) 10f else 0f  // Changed from direct multiplication
            
            // Distance from threats
            val threatLevel = evaluateThreatLevel(tile, unit)
            score -= threatLevel * 15f  // Reduce score based on threats
            
            score
        }
    }
    
    private fun evaluateDefensivePosition(tile: Tile): Float {
        var score = 0f
        
        // Terrain defense bonus
        score += tile.getDefensiveBonus() * 10f
        
        // Hills provide better visibility
        if (tile.isHill()) score += 15f
        
        // Prefer positions near friendly cities
        val nearestCity = tile.getTilesInDistance(3)
            .firstOrNull { it.isCityCenter() && it.getCity()?.civ == tile.getOwner() }
        if (nearestCity != null) score += 20f
        
        return score
    }
    
    private fun evaluateThreatLevel(tile: Tile, unit: MapUnit): Float {
        var threatLevel = 0f
        
        // Check for enemy units that could attack us next turn
        val enemyUnits = tile.getTilesInDistance(5)
            .flatMap { it.getUnits() }
            .filter { it.civ != unit.civ }
        
        for (enemy in enemyUnits) {
            val distance = tile.aerialDistanceTo(enemy.getTile())
            if (distance <= enemy.getRange()) {
                threatLevel += when {
                    enemy.baseUnit.isRanged() -> 3f / distance
                    enemy.baseUnit.isMelee() -> 2f / distance
                    else -> 1f / distance
                }
            }
        }
        
        return threatLevel
    }

    private fun tryHealUnit(unit: MapUnit): Boolean {
        if (unit.health >= 100) return false
        // Reference UnitAutomation.kt lines 226-253 for healing logic
        if (unit.health < 50) {
            unit.fortify()
            return true
        }
        return false
    }
    
    private fun shouldGarrison(unit: MapUnit): Boolean {
        return unit.health < 75 && !unit.getTile().isCityCenter()
    }
    
    private fun findGarrisonCity(unit: MapUnit): City? {
        return unit.getTile().getTilesInDistance(2)
            .firstOrNull { it.isCityCenter() }
            ?.getCity()
    }

}