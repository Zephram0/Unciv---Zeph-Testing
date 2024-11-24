package com.unciv.logic.aautoexpert.rulevalidator_patch

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger
import com.unciv.ui.screens.worldscreen.unit.actions.UnitActionsFromUniques
import com.unciv.logic.civilization.diplomacy.DiplomacyFlags
import com.unciv.ui.screens.worldscreen.unit.actions.UnitActionModifiers

/**
 * Validates all raw input operations for the AI system.
 * This ensures that all game state queries follow proper validation rules and access patterns.
 * Used due to lack of proper Raw Valid Inputs in game.
 * Only used if Raw Vaoid Inputs are unavailable in UnCiv codebase.
 * 
 * See input_output_docs folder for more information.
 */
object ValidRawInputValidator {
    object UnitValidation {
        /**
         * Validates if a unit can found a city on a given tile
         */
        fun canFoundCity(unit: MapUnit, tile: Tile): Boolean {
            try {
                // Basic unit validation
                val foundCityUnique = unit.getMatchingUniques(UniqueType.FoundCity).firstOrNull()
                    ?: return false

                // One City Challenge validation
                if (unit.civ.isOneCityChallenger() && unit.civ.hasEverOwnedOriginalCapital) {
                    ExpertStateLogger.logDecision("Cannot found city - One City Challenge")
                    return false
                }

                // Movement validation
                if (!unit.hasMovement()) {
                    ExpertStateLogger.logDecision("Unit has no movement points")
                    return false
                }

                // Tile validation
                if (tile.isWater || tile.isImpassible()) {
                    ExpertStateLogger.logDecision("Invalid terrain for city")
                    return false
                }

                // Settlement spacing validation
                if (!tile.canBeSettled()) {
                    ExpertStateLogger.logDecision("Tile cannot be settled")
                    return false
                }

                // Action modifier validation
                if (!UnitActionModifiers.canActivateSideEffects(unit, foundCityUnique)) {
                    ExpertStateLogger.logDecision("Cannot activate city founding effects")
                    return false
                }

                return true
            } catch (e: Exception) {
                ExpertStateLogger.logError("Error in canFoundCity validation: ${e.message}")
                return false
            }
        }

        /**
         * Checks if a unit has any valid city founding uniques
         */
        fun hasFoundCityAbility(unit: MapUnit): Boolean {
            return unit.getMatchingUniques(UniqueType.FoundCity).any()
        }

        /**
         * Validates if a unit has movement points available
         */
        fun hasMovement(unit: MapUnit): Boolean {
            return unit.hasMovement()  // Uses established game mechanic
        }

        /**
         * Gets maximum movement points for a unit
         */
        fun getMaxMovement(unit: MapUnit): Int {
            return unit.getMaxMovement()  // Uses established game mechanic
        }
    }

    object TileValidation {
        /**
         * Pure tile validation for city placement.
         * Only checks fundamental game rules, not strategic considerations.
         */
        fun isValidCityLocation(tile: Tile): Boolean {
            // Terrain validation
            if (tile.isWater || tile.isImpassible()) return false

            // City spacing validation - minimum distance between cities
            if (tile.getTilesInDistance(3).any { it.isCityCenter() }) return false

            // Natural wonder validation
            if (tile.naturalWonder != null) return false

            // Ice validation
            if (tile.terrainFeatures.contains("Ice")) return false

            // TODO: [Diplomacy] Add proper diplomatic validation here
            // Currently skipping diplomatic promise validation as it needs to be handled
            // by a dedicated diplomacy system. See issue #XYZ for tracking.
            // Related to: getLeadersWePromisedNotToSettleNear() implementation
            
            return true
        }

        // Commenting out for now - will be moved to diplomacy system
        /*
        private fun getLeadersWePromisedNotToSettleNear(civInfo: Civilization, tile: Tile): String? {
            val leadersWePromisedNotToSettleNear = HashSet<String>()
            for (otherCiv in civInfo.getKnownCivs().filter { it.isMajorCiv() && !civInfo.isAtWarWith(it) }) {
                val diplomacyManager = otherCiv.getDiplomacyManager(civInfo)!!
                if (diplomacyManager.hasFlag(DiplomacyFlags.AgreedToNotSettleNearUs)) {
                    val citiesWithin6Tiles = otherCiv.cities
                        .filter { it.getCenterTile().aerialDistanceTo(tile) <= 6 }
                        .filter { otherCiv.hasExplored(it.getCenterTile()) }
                    if (citiesWithin6Tiles.isNotEmpty()) 
                        leadersWePromisedNotToSettleNear += otherCiv.getLeaderDisplayName()
                }
            }
            return if(leadersWePromisedNotToSettleNear.isEmpty()) null 
                   else leadersWePromisedNotToSettleNear.joinToString(", ")
        }
        */
    }
}
