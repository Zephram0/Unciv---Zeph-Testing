package com.unciv.logic.aautoexpert.expertmodules.expertmilitary.experthandlers

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertStateLogger
import com.unciv.logic.aautoexpert.expertutils.ExpertMovementHelper
import com.unciv.models.ruleset.Ruleset
import com.unciv.logic.aautoexpert.rulevalidator_patch.ValidRawOutputValidator
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertEvaluationModule
import com.unciv.logic.aautoexpert.rulevalidator_patch.ValidRawInputValidator
import com.badlogic.gdx.math.Vector2

/**
 * Handles settler unit movement and city founding decisions.
 * Ensures cities are founded by turn 2 (turn 3 only for exceptional locations).
 */
class ExpertSettlerHandler(private val ruleset: Ruleset) {
    private val evaluationModule = ExpertEvaluationModule(ruleset)

    /**
     * Main handler for settler units. Evaluates potential settlement locations and moves/settles accordingly.
     * 
     * @param unit The settler unit to process
     */
    fun handleSettler(unit: MapUnit) {
        if (unit.currentMovement <= 0) return

        ExpertStateLogger.logDecision("Processing settler ${unit.name} (ID: ${unit.id})")
        ExpertStateLogger.logDecision("Settler movement points: ${unit.currentMovement}/${unit.getMaxMovement()}")

        val currentTurn = unit.civ.gameInfo.turns
        val currentTile = unit.getTile()
        
        // Log all tiles within movement range
        val tilesInRange = currentTile.getTilesInDistance(unit.getMaxMovement().toInt())
        ExpertStateLogger.logDecision("Evaluating ${tilesInRange.count()} tiles within movement range")
        
        val evaluatedTiles = tilesInRange
            .map { tile -> 
                val score = evaluationModule.evaluateSettlementLocation(tile).getTotalScore()
                val isExceptional = evaluationModule.evaluateSettlementLocation(tile).isExceptionalLocation()
                Triple(tile, score, isExceptional)
            }
            .sortedByDescending { it.second }
            .take(5)  // Top 5 locations
        
        ExpertStateLogger.logDecision("Top 5 locations within range:")
        evaluatedTiles.forEach { (tile, score, isExceptional) ->
            ExpertStateLogger.logDecision("  Location: ${tile.position}, Score: $score, Exceptional: $isExceptional")
        }

        val currentScore = evaluationModule.evaluateSettlementLocation(currentTile).getTotalScore()
        val isExceptionalLocation = evaluationModule.evaluateSettlementLocation(currentTile).isExceptionalLocation()

        ExpertStateLogger.logDecision("Current location: ${currentTile.position}, Score: $currentScore, Exceptional: $isExceptionalLocation")

        // Found city immediately if:
        // 1. It's turn 2+ and we're on a good enough tile
        // 2. It's turn 3 and we're on any valid tile
        // 3. We're on an exceptional location
        if (shouldSettleHere(currentTurn, currentScore, isExceptionalLocation)) {
            ExpertStateLogger.logDecision("Decided to settle here: Turn $currentTurn, Score $currentScore, Exceptional $isExceptionalLocation")
            if (ValidRawInputValidator.UnitValidation.canFoundCity(unit, currentTile)) {
                if (ValidRawOutputValidator.UnitCommands.foundCity(unit)) {
                    ExpertStateLogger.logDecision("Founded city at ${currentTile.position}")
                    return
                }
            }
        }

        // Look for better location if we haven't settled yet
        val settlementLocation = findSettlementLocation(unit, currentTurn)
        if (settlementLocation != null) {
            val targetScore = evaluationModule.evaluateSettlementLocation(settlementLocation).getTotalScore()
            ExpertStateLogger.logDecision("Found potential location: ${settlementLocation.position}, Score: $targetScore")
            if (settlementLocation == currentTile) {
                // If at target location, found city if valid
                if (ValidRawInputValidator.UnitValidation.canFoundCity(unit, settlementLocation)) {
                    if (ValidRawOutputValidator.UnitCommands.foundCity(unit)) {
                        ExpertStateLogger.logDecision("Founded city at ${settlementLocation.position}")
                    }
                }
            } else {
                // Move towards target location safely
                ExpertMovementHelper.moveTowardsSafely(unit, settlementLocation)
                ExpertStateLogger.logDecision("Moving settler towards ${settlementLocation.position}")
            }
        }
    }

    /**
     * Determines if we should settle at the current location based on turn number and location quality
     */
    private fun shouldSettleHere(currentTurn: Int, currentScore: Float, isExceptionalLocation: Boolean): Boolean {
        return when {
            isExceptionalLocation -> currentTurn >= 1  // Even exceptional locations should wait a turn
            currentTurn >= 4 -> true  // Must settle by turn 4
            currentTurn == 3 -> currentScore >= 80f  // Need good location by turn 3
            currentTurn == 2 -> currentScore >= 85f  // Need very good location
            currentTurn == 1 -> currentScore >= 90f  // Need excellent location
            else -> currentScore >= 95f  // Turn 0 requires near-perfect score
        }
    }

    /**
     * Finds the optimal settlement location within search radius.
     * Search radius and minimum acceptable score decrease as turns progress.
     * 
     * @param unit The settler unit
     * @param currentTurn The current game turn
     * @return The best tile to settle on, or null if none found
     */
    private fun findSettlementLocation(unit: MapUnit, currentTurn: Int): Tile? {
        // Reduce search radius as turns progress to ensure timely settlement
        val searchRadius = when {
            currentTurn >= 3 -> 2  // Very limited search on turn 3+
            currentTurn == 2 -> 3  // Limited search on turn 2
            currentTurn == 1 -> 4  // Moderate search on turn 1
            else -> 5              // Wide search on turn 0
        }

        val currentTile = unit.getTile()
        val minimumScore = when {
            currentTurn >= 3 -> 75f   // Need decent location
            currentTurn == 2 -> 80f   // Need good location
            currentTurn == 1 -> 85f   // Need very good location
            else -> 90f               // Need excellent location
        }

        var bestTile = currentTile
        var bestScore = evaluationModule.evaluateSettlementLocation(currentTile).getTotalScore()

        // Get tiles we can actually reach this turn
        val reachableTiles = currentTile.getTilesInDistance(searchRadius)
            .filter { tile -> 
                ValidRawInputValidator.UnitValidation.canFoundCity(unit, tile) &&
                unit.movement.canReachInCurrentTurn(tile)
            }

        for (tile in reachableTiles) {
            val score = evaluationModule.evaluateSettlementLocation(tile).getTotalScore()
            if (score > bestScore) {
                bestTile = tile
                bestScore = score
            }
        }

        return if (bestScore >= minimumScore) bestTile else null
    }
}