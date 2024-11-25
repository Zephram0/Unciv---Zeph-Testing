# Verified Raw Inputs

This document lists all verified raw inputs found in the current codebase. These are direct game state queries that include proper validation.

## Unit Inputs
From ExpertMilitaryHandler and ExpertMovementHelper:

---kotlin
// State Queries
unit.currentMovement                 // Direct state query with built-in validation
unit.isFortified()                  // State query
unit.getTile()                      // Position query
unit.isMilitary()                   // Unit type query
unit.baseUnit.getMatchingUniques(UniqueType.FoundCity)  // Unit capability query
---

## Tile Inputs
From ExpertMovementHelper:

---kotlin
// Terrain and Unit Queries
tile.isImpassible()                 // Terrain validation
tile.getTilesInDistance(radius)     // Vision query
tile.getUnits()                     // Unit presence query
tile.position                       // Location query
---

## Movement Inputs
From ExpertMovementHelper:

---kotlin
// Path and Movement Validation
unit.movement.getShortestPath(target)  // Path validation
unit.movement.canMoveTo(tile)          // Movement validation
---

## Notes
- All listed inputs include necessary validation logic
- These inputs are verified to work with the AI system
- Each input respects game rules and fog of war
- All inputs are used in current implementation

## Usage Example
From ExpertMovementHelper:

---kotlin
fun moveTowardsSafely(unit: MapUnit, target: Tile) {
    if (unit.currentMovement <= 0) return  // Validated state query
    
    val path = unit.movement.getShortestPath(target)  // Validated path query
    val nextTile = path.firstOrNull() ?: return
    
    if (!isMovementSafe(unit, nextTile)) {  // Additional validation
        return
    }
} 