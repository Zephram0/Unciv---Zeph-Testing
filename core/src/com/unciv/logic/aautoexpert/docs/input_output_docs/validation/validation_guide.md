# Validation Guide

This guide explains how to identify Valid Raw Inputs (VRI) and Valid Raw Outputs (VRO) in the UnCiv codebase, and when to create validators. Since UnCiv implements Civilization 5 mechanics, understanding the original game rules is crucial for proper validation.

## Using Civilization 5 Rules as Reference

Before identifying VRI/VRO, understand the corresponding Civilization 5 mechanics:

### Key Mechanics to Consider
1. **Unit Actions**
   - Units can only move within movement points
   - Combat requires valid line of sight
   - City founding needs minimum city spacing (3 tile spacing)
   - Units can't move after combat (with exceptions like Great Generals)

2. **Resource and Vision Rules**
   - Resources only visible after proper technology (e.g., Calendar for Dyes)
   - Units can only see within vision range (2 tiles for most units)
   - Fog of war hides unexplored areas
   - Strategic resources require improvements (Mine for Iron)

3. **City Management**
   - Cities need minimum spacing (3 tile spacing)
   - Population affects workable tiles (max 3 rings)
   - Buildings have prerequisites (Library needs Writing)
   - Trade routes need valid paths (roads/harbors)

### Examples of Rule Validation
---kotlin
// Valid: Follows Civ 5 rules
unit.movement.canMoveTo(tile)  // Checks movement points, terrain, ZoC
unit.canSee(tile)  // Validates vision range and fog of war

// Invalid: Would bypass Civ 5 mechanics
unit.teleportTo(tile)  // Ignores movement rules and ZoC
city.addBuilding(building)  // Might bypass tech/building prerequisites
---

## Identifying Valid Raw Inputs

### Step 1: Check Function Properties
A function is likely a VRI if it follows Civilization 5's information access rules:
- Returns game state information (like unit vision range)
- Includes built-in validation (e.g., checking tech prerequisites)
- Doesn't modify game state (read-only like resource visibility)
- Is publicly accessible (like trade route calculations)

Example from our codebase:
---kotlin
// Valid: Read-only states with built-in Civ 5 rule validation
unit.currentMovement     // Considers terrain, roads, and promotions
unit.getCombatStrength() // Includes flanking, terrain bonuses
city.getProductionNeeded() // Accounts for wonders, difficulty

// Invalid: Direct access without proper Civ 5 rule checking
unit.health             // Needs combat damage validation
city.population         // Needs growth limit validation
---

### Step 2: Verify Validation
Check if the function enforces Civilization 5 rules:
1. Validates inputs (e.g., unit can embark)
2. Handles edge cases (e.g., citadel culture flips)
3. Respects fog of war (e.g., unit visibility)
4. Returns consistent results (e.g., combat calculations)

Example from ExpertMovementHelper:
---kotlin
// Good: Includes Civ 5 movement validation
unit.movement.canMoveTo(tile) {
    validateMovementPoints()  // Checks roads, terrain
    validateZoneOfControl()   // Applies ZoC rules
    validateEnemyUnits()      // Checks combat eligibility
}

// Bad: Would need validator for Civ 5 rules
unit.position = newPosition  // Bypasses movement/combat rules
---

## Identifying Valid Raw Outputs

### Step 1: Check Action Properties
An action is likely a VRO if it follows Civilization 5's state change rules:
- Modifies game state atomically (like combat resolution)
- Includes pre-condition validation (tech requirements)
- Uses established game mechanics (worker improvements)
- Maintains state consistency (city working tiles)

Example from our codebase:
---kotlin
// Valid: Follows Civ 5 action rules
unit.movement.moveToTile(tile) {  // Proper movement validation
    spendMovementPoints()         // Accounts for terrain
    triggerCulturalBorders()      // Handles border crossing
    updateTradeRoutes()           // Recalculates if needed
}

// Invalid: Would bypass Civ 5 mechanics
unit.teleportTo(tile)    // Ignores movement/vision rules
city.forceBuildUnit()    // Bypasses production queue
---

## When to Create Validators

### Input Validators Needed When:
1. Direct property access is required
2. Complex validation is needed
3. Multiple conditions must be checked
4. No existing validated function exists

Example from our codebase:
---kotlin
// Need validator: Complex city founding rules
ValidRawInputValidator.UnitValidation.canFoundCity(unit, tile)
---

### Output Validators Needed When:
1. Multiple steps must be atomic
2. Complex state changes required
3. Error handling needed
4. No existing validated action exists

Example from our codebase:
---kotlin
// Need validator: Multi-step city founding
ValidRawOutputValidator.UnitCommands.foundCity(unit)
---

## Testing Validation

### For Inputs:
1. Test with invalid states
2. Verify fog of war handling
3. Check edge cases
4. Ensure consistent results

Example:
---kotlin
// Testing movement validation
fun testMovementValidation() {
    val unit = createTestUnit(movement = 0)
    assert(!unit.movement.canMoveTo(targetTile))  // Should fail with no movement
}
---

### For Outputs:
1. Test pre-conditions
2. Verify atomic execution
3. Check error handling
4. Validate state consistency

Example:
---kotlin
// Testing city founding
fun testCityFounding() {
    val unit = createSettler()
    val result = ValidRawOutputValidator.UnitCommands.foundCity(unit)
    assert(result)  // Should succeed
    assert(unit.isDestroyed())  // Settler should be removed
    assert(tile.hasCity())  // City should exist
}
---

## Common Pitfalls

1. **Direct Property Access**
   - Always use validated functions instead of direct property access
   - Create validators if needed

2. **State Modifications**
   - Never modify state directly
   - Use established game mechanics
   - Create atomic validators for complex operations

3. **Validation Bypass**
   - Don't skip validation for performance
   - Don't assume valid states
   - Always handle edge cases

4. **Error Handling**
   - Always use ExpertStateLogger
   - Handle exceptions gracefully
   - Maintain state consistency

## See Also

- [Core Input/Output Concepts](core_in_out_concepts.md)
- [Raw Inputs](../verified_io/raw_inputs.md)
- [Raw Outputs](../verified_io/raw_outputs.md)
- [Input Validators](../validator_reference/input_validators.md)
- [Output Validators](../validator_reference/output_validators.md) 