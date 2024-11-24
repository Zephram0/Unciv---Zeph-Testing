package com.unciv.logic.aautoexpert.rulevalidator_patch

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger
import com.unciv.models.UnitActionType
import com.unciv.models.ruleset.unique.UniqueType

/**
 * Validates and executes all raw output operations for the AI system.
 * This ensures that all game state modifications follow proper validation rules and mechanics.
 * Used due to lack of proper Raw Valid Outputs in game.
 * Only used if Raw Vaoid Outputs are unavailable in UnCiv codebase.
 * 
 * See input_output_docs folder for more information.
 */
object ValidRawOutputValidator {
    object UnitCommands {
        /**
         * Validates and executes city founding using core game mechanics
         */
        fun foundCity(unit: MapUnit): Boolean {
            try {
                // Pre-execution validation
                if (!ValidRawInputValidator.UnitValidation.canFoundCity(unit, unit.getTile())) {
                    return false
                }

                // Execute core city founding mechanic
                val tile = unit.getTile()
                unit.civ.addCity(tile.position, unit)
                unit.destroy()  // Explicitly destroy the settler unit
                
                ExpertStateLogger.logDecision("City founded successfully at ${tile.position}")
                return true
            } catch (e: Exception) {
                ExpertStateLogger.logError("Error founding city: ${e.message}")
                return false
            }
        }

        /**
         * Validates and executes unit movement
         */
        fun moveToTile(unit: MapUnit, tile: Tile): Boolean {
            try {
                // Pre-movement validation
                if (!unit.hasMovement()) {
                    ExpertStateLogger.logDecision("Unit has no movement points")
                    return false
                }
                if (!unit.movement.canMoveTo(tile)) {
                    ExpertStateLogger.logDecision("Cannot move to target tile")
                    return false
                }

                // Execute movement through proper game mechanics
                unit.movement.moveToTile(tile)
                ExpertStateLogger.logDecision("Unit moved to ${tile.position}")
                return true
            } catch (e: Exception) {
                ExpertStateLogger.logError("Error moving unit: ${e.message}")
                return false
            }
        }
    }

    /**
     * Validates and executes combat-related commands
     */
    object CombatCommands {
        /**
         * Validates and executes unit fortification
         */
        fun fortify(unit: MapUnit): Boolean {
            try {
                if (!unit.hasMovement()) return false
                if (unit.isFortified()) return false

                unit.fortify()
                return true
            } catch (e: Exception) {
                ExpertStateLogger.logError("Error fortifying unit: ${e.message}")
                return false
            }
        }
    }
}
