# Required Output Validators

This document lists all output validators currently implemented in the codebase. These validators are required where direct raw outputs are not available or need additional validation.

## Unit Commands
From ValidRawOutputValidator:

---kotlin
// City Founding
ValidRawOutputValidator.UnitCommands.foundCity(unit)
// Required because: No atomic city founding command
// Used in: ExpertSettlerHandler
// Handles: 
// - Pre-validation of city location
// - City creation
// - Settler unit cleanup
// - Error handling and logging

// Unit Fortification
ValidRawOutputValidator.CombatCommands.fortify(unit)
// Required because: Needs additional validation
// Used in: ExpertMilitaryHandler
// Handles:
// - Movement point validation
// - Current state validation
// - Error handling
---

## Notes
- Validators ensure atomic operations
- Each validator includes error handling
- All validators maintain game state consistency
- Validators use ExpertStateLogger for debugging

## Usage Example
From ValidRawOutputValidator:

---kotlin
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