# Valid Raw Outputs for AAutoExpert

> Use in_out_validation_guide.md as guide to find and validate raw outputs.
> **CRITICAL**: Check `patterns/` docs first, validate against current codebase, then search for established patterns.
> Follow template below. Keep updated with current code to prevent bugs.


## Valid Raw Output Definition:

**Valid Raw Output:**
> A direct action that:
1. Maps 1:1 with a single player action
2. Has direct, observable effects
3. Cannot be broken down into more fundamental actions
4. Includes mandatory validation checks
5. Only performs actions available to human players
6. Uses established game mechanics for state changes

Examples:
````kotlin
// Raw Outputs
unit.movement.moveToTile(tile)      // Includes movement point validation
unit.fortify()                      // Legal state change with validation
city.purchaseBuilding(building)     // Includes resource/gold validation
unit.doAttack(defender)             // Includes combat eligibility checks
````

### Invalid Examples:

**Invalid Raw Outputs:**
````kotlin
// These bypass game rules or validation
unit.health = newValue             // Direct property modification not allowed
unit.position = newPosition        // Must use movement system
unit.teleportTo(tile)             // Bypasses movement rules
city.addPopulation(5)             // Must use growth mechanics
````

### Example of Proper Raw Output with Validation:
````kotlin:MapUnit.kt
// Valid raw output with proper validation
fun moveToTile(targetTile: Tile) {
    // Mandatory validation
    if (!hasMovement()) return
    if (!canEnterTile(targetTile)) return
    if (!movement.canMoveTo(targetTile)) return
    
    // Actual movement execution through proper game mechanics
    movement.headTowards(targetTile)
}
````

Valid Raw Output definition emphasizes that:
1. Raw inputs/outputs must include their mandatory validation logic
2. Direct property access/modification is usually not valid
3. Actions must use established game mechanics
4. Validation is part of the "raw" interface, not an optional addition


## Pattern Template

```kotlin:SourceFile.kt:[lines]
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
- **Enables:** What the pattern enables user to do, must be a valid, non-cheating action.
- **Issues:** Integration problems, primary issue, secondary issues, etc.
- **Considerations:** Previous bugs/issues, solution description.

## 1. Unit Command Outputs
> Raw outputs to Unciv's unit command system (com.unciv.logic.map.mapunit)

### 1.1 Movement Command

```kotlin:com/unciv/logic/map/mapunit/MapUnit.kt:[lines]
// Execute unit movement
fun moveUnit(unit: MapUnit, targetTile: Tile) {
    require(unit.movement.canMoveTo(targetTile)) { "Invalid movement target" }
    unit.movement.headTowards(targetTile)
}

// Usage example
    fun executeUnitMovement() {
        if (moveUnit(selectedUnit, targetTile)) {
            updateUnitPosition(selectedUnit)
        }
    }
```
- **Enables:** Move units to valid adjacent tiles, following movement rules and terrain restrictions
- **Issues:** Movement point validation, path accessibility, unit stacking rules
- **Considerations:** Previous path validation bugs, solved by pre-move validation checks

### 1.2 Unit State Command

```kotlin:com/unciv/logic/map/mapunit/MapUnit.kt:[lines]
// Change unit state
fun setUnitState(unit: MapUnit, state: UnitState) {
    when(state) {
        UnitState.FORTIFIED -> unit.fortify()
        UnitState.SLEEP -> unit.sleep()
        UnitState.ALERT -> unit.alertMode()
        UnitState.ACTIVE -> unit.action = null
    }
}

// Usage example
    fun updateUnitStatus() {
        setUnitState(defendingUnit, UnitState.FORTIFIED)
        updateUnitDisplay(defendingUnit)
    }
```
- **Enables:** Change unit states between fortified, sleep, alert, and active modes
- **Issues:** State transition validation, action interruption handling
- **Considerations:** Previous state inconsistency bugs, solved by atomic state transitions

## 2. Battle Command Outputs
> Raw outputs to Unciv's battle system (com.unciv.logic.battle)

### 2.1 Combat Command

```kotlin:com/unciv/logic/battle/BattleHelper.kt:[lines]
// Execute combat action
fun executeCombat(attacker: MapUnit, defender: MapUnit) {
    require(BattleHelper.canAttack(attacker, defender.getTile())) { "Invalid attack" }
    BattleHelper.tryAttackNearbyEnemy(attacker, stayOnTile = false)
}

// Usage example
    fun performAttack() {
        if (executeCombat(attackingUnit, targetUnit)) {
            handlePostCombat(attackingUnit)
        }
    }
```
- **Enables:** Execute combat between units following game rules and combat mechanics
- **Issues:** Range validation, line of sight checks, combat eligibility
- **Considerations:** Previous combat sequence bugs, solved by pre-combat validation

## 3. City Command Outputs
> Raw outputs to Unciv's city system (com.unciv.logic.city)

### 3.1 Garrison Command

```kotlin:com/unciv/logic/city/City.kt:[lines]
// Execute garrison action
fun garrisonUnit(unit: MapUnit, city: City) {
    require(city.canGarrison(unit)) { "Invalid garrison target" }
    unit.movement.headTowards(city.getCenterTile())
}

// Usage example
    fun setupCityDefense() {
        if (garrisonUnit(militaryUnit, targetCity)) {
            updateCityDefenses(targetCity)
        }
    }
```
- **Enables:** Station military units in cities for defense bonuses
- **Issues:** Garrison capacity limits, unit eligibility, movement requirements
- **Considerations:** Previous garrison stacking bugs, solved by capacity validation
