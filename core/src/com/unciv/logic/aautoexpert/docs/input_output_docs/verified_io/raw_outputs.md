# Verified Raw Outputs

This document lists all verified raw outputs found in the current codebase. These are direct game actions that include proper validation.

## Unit Commands
From ValidRawOutputValidator and ExpertMovementHelper:

---kotlin
// Movement and State Changes
unit.movement.moveToTile(tile)      // Movement execution with validation
unit.fortify()                      // State change with validation
unit.destroy()                      // Unit removal - NOTE: Currently only verified as part of city founding
                                   // Further evaluation needed for general unit disbanding
---

## City Commands
From ValidRawOutputValidator:

---kotlin
// City Creation
unit.civ.addCity(tile.position, unit)  // City creation with position validation
---

## Combat Commands
Currently no verified combat outputs in our implementation. Combat system needs validator implementation.

## Notes
- All listed outputs include necessary validation logic
- These outputs are verified to work with the AI system
- Each output maintains game state consistency
- All outputs are used in current implementation
- Some outputs may need further verification for general use cases

## Usage Example
From ValidRawOutputValidator:

---kotlin
fun foundCity(unit: MapUnit): Boolean {
    try {
        // Pre-execution validation
        if (!ValidRawInputValidator.UnitValidation.canFoundCity(unit, unit.getTile())) {
            return false
        }

        // Execute core city founding mechanic
        val tile = unit.getTile()
        unit.civ.addCity(tile.position, unit)  // Verified raw output
        unit.destroy()                         // Verified raw output (as part of city founding)
        
        return true
    } catch (e: Exception) {
        ExpertStateLogger.logError("Error founding city: ${e.message}")
        return false
    }
} 