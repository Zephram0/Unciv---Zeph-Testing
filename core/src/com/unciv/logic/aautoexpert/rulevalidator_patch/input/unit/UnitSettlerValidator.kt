package com.unciv.logic.aautoexpert.rulevalidator_patch.input.unit

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger
import com.unciv.models.ruleset.unique.UniqueType

/**
 * Validates settler-specific operations following the 5-layer validation system.
 * This is part of the Raw Input Layer that ensures all settler operations are valid.
 * 
 * IMPORTANT: This validator only implements validations that don't exist as Raw Valid Inputs (RVI)
 * in the base codebase. For existing RVIs, we use them directly instead of reimplementing.
 * See raw_inputs.md for the list of existing RVIs.
 */
object UnitSettlerValidator {
    /**
     * Validates if a city can be founded at the given location.
     * This is a necessary validator as no direct RVI exists for city founding validation.
     * Only validates mandatory game rules, not strategic considerations.
     */
    fun canFoundCity(unit: MapUnit, tile: Tile): Boolean {
        // 1. Early validation
        if (!validateBasicRequirements(unit)) return false
        
        // 2. Game rule validation
        if (!validateGameRules(tile)) return false
        
        // 3. State validation
        if (!validateGameState(unit, tile)) return false
        
        return true
    }

    /**
     * Basic requirement checks (unit type, etc)
     */
    private fun validateBasicRequirements(unit: MapUnit): Boolean {
        if (!unit.baseUnit.getMatchingUniques(UniqueType.FoundCity).any()) {
            ExpertStateLogger.logDecision("Unit cannot found cities")
            return false
        }

        return true
    }

    /**
     * Game rule validation (terrain, spacing, etc)
     */
    private fun validateGameRules(tile: Tile): Boolean {
        if (tile.isWater || tile.isImpassible()) {
            ExpertStateLogger.logDecision("Invalid terrain for city")
            return false
        }

        // City spacing rules
        if (tile.getTilesInDistance(3).any { it.isCityCenter() }) {
            ExpertStateLogger.logDecision("Too close to existing city")
            return false
        }

        return true
    }

    /**
     * Current game state validation
     */
    private fun validateGameState(unit: MapUnit, tile: Tile): Boolean {
        // Check if tile is visible and explored
        if (!unit.civ.hasExplored(tile)) {
            ExpertStateLogger.logDecision("Tile not explored")
            return false
        }

        // Check if tile is owned by another civ
        if (tile.getOwner() != null && tile.getOwner() != unit.civ) {
            ExpertStateLogger.logDecision("Tile owned by another civilization")
            return false
        }

        return true
    }
} 