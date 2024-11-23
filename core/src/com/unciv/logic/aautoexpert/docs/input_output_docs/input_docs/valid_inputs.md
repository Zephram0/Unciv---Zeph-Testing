# Valid Raw Inputs for AAutoExpert

> Use in_out_validation_guide.md as guide to find and validate raw inputs.
> **CRITICAL**: Check `patterns/` docs first, validate against current codebase, then search for established patterns.
> Follow template below. Keep updated with current code to prevent bugs.


## Valid Raw Input Definition:

**Valid Raw Input:**
> A direct query of game state that:
1. Returns unmodified data directly from the game's data structures
2. Does not perform calculations or transformations beyond mandatory validation
3. Does not trigger state changes
4. Is accessible through public interfaces
5. Only accesses information that would be visible to a human player
6. Includes necessary validation logic to prevent illegal state access

Examples:
````kotlin
// Raw Inputs
unit.hasMovement()                  // Includes necessary movement point validation
unit.canAttack()                    // Includes mandatory combat eligibility checks
tile.isVisible(civInfo)             // Includes fog of war validation
unit.canEnterTile(tile)             // Includes terrain/unit stacking validation
````

### Invalid Examples:

**Invalid Raw Inputs:**
````kotlin
// These bypass game rules or validation
unit.health                         // Direct property access without validation
tile.resource                       // Might bypass fog of war
unit.currentMovement               // Should use hasMovement() instead
````

Valid Raw Input definition emphasizes that:
1. Raw inputs/outputs must include their mandatory validation logic
2. Direct property access/modification is usually not valid
3. Actions must use established game mechanics
4. Validation is part of the "raw" interface, not an optional addition


## Pattern Template

```kotlin:SourceFile.kt:[include line numbers]
// Function signature and core implementation
fun patternFunction(param1: Type1, param2: Type2): ReturnType {
    return result
}

// Usage example
    fun usePattern() {
        val result = patternFunction(value1, value2)
        handleResult(result)
    }
```
- **Enables:** What the pattern enables user to know, must be a valid, non-cheating observation.
- **Issues:** Integration problems, primary issue, secondary issues, etc.
- **Considerations:** Previous bugs/issues, solution description.

## 1. Unit System Inputs
> Raw inputs from Unciv's unit management system (com.unciv.logic.map.mapunit)

### 1.1 Unit Combat State Query

```kotlin:MapUnit.kt:[lines]
fun canAttack(): Boolean {
    if (!hasMovement()) return false
    if (isCivilian()) return false
    return attacksThisTurn < maxAttacksPerTurn()
}

// Usage example
    fun evaluateCombatCapability() {
        if (unit.canAttack() && unit.health > 50) {
            considerAttackingTarget()
        }
    }
```
- **Enables:** Query unit's current combat capabilities and attack availability
- **Issues:** Unit existence validation, movement point validation
- **Considerations:** Previous combat validation bugs, solved by comprehensive state checks

### 1.2 Unit Movement Query

```kotlin:MapUnit.kt:[lines]
fun hasMovement(): Boolean = currentMovement > 0

fun getMaxMovement(ignoreOtherUnit: Boolean = false): Int {
    var movement =
            if (isEmbarked()) 2
            else baseUnit.movement

    movement += getMatchingUniques(UniqueType.Movement, checkCivInfoUniques = true)
            .sumOf { it.params[0].toInt() }

    if (movement < 1) movement = 1
    return movement
}

// Usage example
    fun evaluateMovementCapability() {
        if (unit.hasMovement() && unit.getMaxMovement() > 1) {
            considerStrategicMovement()
        }
    }
```
- **Enables:** Query unit's current and maximum movement capabilities
- **Issues:** Movement point validation, embarked state handling
- **Considerations:** Previous movement calculation bugs, solved by proper state validation