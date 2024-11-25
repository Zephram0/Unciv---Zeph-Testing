# Required Input Validators

This document lists all input validators currently implemented in the codebase. These validators are required where direct raw inputs are not available or need additional validation.

## Unit Validation
From ValidRawInputValidator:

---kotlin
// City Founding Validation
ValidRawInputValidator.UnitValidation.canFoundCity(unit, tile)
// Required because: No direct city founding validation in base game
// Used in: ExpertSettlerHandler, ValidRawOutputValidator
// Validates: 
// - Terrain suitability
// - City spacing rules
// - Resource availability
// - Unit capability
---

## Notes
- Validators are only created when necessary
- Each validator includes comprehensive validation
- All validators maintain game rule compliance
- Validators are used consistently across the AI system

## Usage Example
From ValidRawOutputValidator:

---kotlin
// Example of validator usage in city founding
if (!ValidRawInputValidator.UnitValidation.canFoundCity(unit, unit.getTile())) {
    ExpertStateLogger.logDecision("Cannot found city - validation failed")
    return false
} 