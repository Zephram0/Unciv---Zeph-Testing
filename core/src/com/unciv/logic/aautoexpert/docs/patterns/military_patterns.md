# Military Module Coding Patterns

> This guide documents common patterns and gotchas when working with military units in Unciv. Use these examples to write correct code the first time and avoid common implementation mistakes.

> **Pattern Documentation Requirements**
>
> This is a living document that must be updated and organized whenever debugging was required after coding and new military patterns are discovered, documenting the new error. For all implementations:
>
> 1. REQUIRED: `Core Battle Integration` - Document battle-related method signatures, integration points, and type requirements from both `aautoexpert/modules/military/` and Unciv's combat system implementations.
> 2. REQUIRED: `Unit Management Patterns` - Update unit handling patterns and examples based on implementations in `aautoexpert/modules/military/handlers/` and Unciv's unit management systems.
> 3. REQUIRED: `Implementation Guidelines` - Add correct/incorrect usage examples and type requirements discovered during development, referencing both internal patterns and external Unciv interfaces.
> 4. REQUIRED: `Error Prevention & Debugging` - Document bugs, solutions, and testing procedures found during implementation, including interactions with Unciv's core systems.
> 5. REQUIRED: `Integration Reference` - Update integration points and module interactions based on current implementations in `aautoexpert/modules/military/` implementations, patterns from `architecture.md#Integration-Reference`, and required Unciv game system interfaces.
>
> All new patterns MUST be compared to existing patterns to update patterns and avoid redundancy.

## 1. Core Battle Integration

### 1.1 Method Signatures and Usage

**Correct Usage:**
```kotlin:path/to/aautoexpert/modules/military/BattleHelper.kt
fun tryAttackNearbyEnemy(unit: MapUnit, stayOnTile: Boolean = false): Boolean
```

**Incorrect Usage:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
BattleHelper.tryAttackNearbyEnemy(unit, targetTile) // Wrong! Can't pass Tile as second parameter
```

### 1.2 Battle Helper Integration Points

- **Integration with MilitaryModule:**
  - Utilizes `BattleHelper` methods for executing attacks and evaluating positions.
  - Ensures separation between battle logic and base automation.

### 1.3 Common Battle Methods Reference

#### 1.3.1 **Attack Methods**

- `tryAttackNearbyEnemy(unit, stayOnTile)` - Returns if unit can't move further.
- `chooseAttackTarget(unit, attackableEnemies)` - Returns best `AttackableTile`.
- `canAttack(attacker, targetTile)` - Checks if attack is possible.

#### 1.3.2  **Position Evaluation**

- `evaluateDefensivePosition(tile)` - Returns float score.
- `getTilesInDistance(range)` - Returns tiles within range.
- `getAttackableEnemies(unit, distanceToTiles)` - Returns list of attackable enemies.

### 1.4 Type Requirements and Validations

- Ensure all parameters match the expected types in `BattleHelper` methods.
- Use named parameters to enhance clarity and prevent type mismatches.

---

## 2. Unit Management Patterns

### 2.1 Unit Classification System

```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
// Correct pattern for unit type classification
fun classifyUnit(unit: MapUnit): UnitType {
    return when {
        unit.type.isAirUnit() -> UnitType.AIR
        unit.type.isWaterUnit() -> UnitType.WATER
        else -> UnitType.LAND
    }
}
```

### 2.2 Priority Calculations

```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
// Correct pattern for priority calculation
fun calculateUnitPriority(unit: MapUnit): Float {
    return when (classifyUnit(unit)) {
        UnitType.AIR -> calculateAirUnitPriority(unit)
        UnitType.WATER -> calculateWaterUnitPriority(unit)
        UnitType.LAND -> calculateLandUnitPriority(unit)
    }
}
```

### 2.3 Movement Patterns

- Implement controlled movement functions to ensure units move correctly without bypassing game rules.
- Example: Use `moveUnit(unit, tile)` with validation checks.

### 2.4 State Management

- Centralize state updates through state managers to maintain consistency across modules.
- Example: Updating unit positions and movement points in a centralized manner.

---

## 3. Implementation Guidelines

### 3.1 Correct vs Incorrect Usage Examples

**Incorrect:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
BattleHelper.tryAttackNearbyEnemy(unit, bestTarget.getTile())  // Incorrect
```

**Correct:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
BattleHelper.tryAttackNearbyEnemy(unit, stayOnTile = false)  // Correct
```

### 3.2 Type Matching Requirements

- Ensure all method calls have matching parameter types as per method signatures.
- Use type-safe operations to prevent runtime errors.

### 3.3 Parameter Validation

- Validate all input parameters before processing to avoid unexpected behaviors.
- Example: Check if `unit` is not null and `tile` is within valid bounds.

### 3.4 State Verification

- Verify the state of units and game before executing actions.
- Example: Ensure units have sufficient movement points before moving or attacking.

### 3.5 Module Communication

- Establish clear interfaces between modules to ensure proper data flow.
- Use standardized message formats when passing data between modules.
- Implement proper error handling for inter-module communication.
- Document module dependencies and communication patterns.

---

## 4. Error Prevention & Debugging

### 4.1 Common Gotchas

#### 4.1.1 **Type Mismatches in Battle Helper Calls**

**Issue:**  
Passing incorrect parameter types (e.g., `Tile` instead of `Boolean`) to `BattleHelper` methods.

**Location:**  
`MilitaryModule.kt`

**Example Error:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
BattleHelper.tryAttackNearbyEnemy(unit, bestTarget.getTile())  // Incorrect
```

**Solution:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
BattleHelper.tryAttackNearbyEnemy(unit, stayOnTile = false)  // Correct
```

**Prevention:**
- Always check method signatures in helper classes before implementation.
- Use named parameters for clarity to avoid confusion with parameter order.
- Ensure argument types match the expected parameters in method signatures.

#### 4.1.2 **Collection Operations on Non-Collections**

**Issue:**  
Using collection methods like `isNotEmpty()` on objects that are not collections, leading to type mismatches.

**Location:**  
`MilitaryModule.kt`

**Example Error:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
score += enemyUnitsInRange.isNotEmpty() * 10f  // Incorrect boolean multiplication
```

**Solution:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
score += if (enemyUnitsInRange.isNotEmpty()) 10f else 0f  // Correct boolean handling
```

**Prevention:**
- Ensure that the variable is a collection before performing collection operations.
- Use explicit conditional statements instead of relying on implicit type conversions.
- Regularly review variable types, especially when performing operations that are type-specific.

#### 4.1.3 **Unused Parameters and Variables**

**Issue:**  
Declaring parameters or variables that are never used, resulting in compiler warnings and potential confusion.

**Location:**  
`MilitaryModule.kt`

**Example Warnings:**
```
Parameter 'unit' is never used
Variable 'currentTile' is never used
```

**Solution:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
// Original function with unused parameter
private fun unusedFunction(unit: MapUnit) { ... }

// Fixed by removing the unused parameter
private fun unusedFunction() { ... }
```

**Prevention:**
- Regularly review compiler warnings and address them promptly.
- Use IDE features to identify and remove unused code.
- Adopt a practice of writing minimalistic code, adding complexity only when necessary.

#### 4.1.4 Collection Operations

**Incorrect Usage:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
// Wrong - Direct boolean multiplication
score += enemyUnitsInRange.isNotEmpty() * 10f  
```

**Correct Usage:**
```kotlin:path/to/aautoexpert/modules/military/MilitaryModule.kt
// Correct - Explicit boolean handling
score += if (enemyUnitsInRange.isNotEmpty()) 10f else 0f
```

### 4.2 Debugging Practices

- **Address Errors Systematically**
  - Tackle one error at a time.
  - Use compiler messages to locate and understand issues.

- **Use Logging for Debugging**
  - Add debug logs to track unit decisions and performance.
  - Verify the flow of unit actions through logs.

- **Test After Each Fix**
  - Compile and test the game after each fix to ensure no new issues arise.
  - Monitor for any runtime issues and debug as necessary.

### 4.3 Testing Procedures

- Implement unit tests using **JUnit** and **Mockito** for all AI modules.
- Develop integration tests to validate interactions between AI components.
- Ensure tests cover all critical functionalities and edge cases.

### 4.4 Performance Considerations

- Optimize algorithms to reduce latency in decision-making.
- Utilize caching and efficient data structures to speed up operations.

---

## 5. Integration Reference

### 5.1 Related Pattern Documents

- (Not implemented yet) [combat_patterns.md](combat_patterns.md) - Combat system integration
- (Not implemented yet) [movement_patterns.md](movement_patterns.md) - Unit movement patterns

### 5.2 External Module Connections

- **Battle Helper Integration**
  - Connects with `BattleHelper.kt` for executing combat actions.
  - Utilizes `StateManager.kt` for state synchronization.

### 5.3 Required Interfaces

- Implements interfaces defined in `AIExpertModule.kt` for seamless integration with other AI modules.
- Adheres to contracts specified in `AIModule.kt` to maintain consistency across modules.

### 5.4 State Synchronization

- Ensures consistent state across all modules by centralizing state updates and access through `StateManager.kt`.
- Utilizes observer patterns to listen for and respond to state changes.

### 5.5 Event Handling

- Implements observer patterns to handle events such as state changes and action triggers.
- Facilitates communication between `MilitaryModule`, `BattleHelper`, and other AI components through event-driven mechanisms.

### 5.6 Module Dependencies

- `MilitaryModule` depends on `BattleHelper` for combat calculations and executing combat actions
- `MilitaryModule` uses `MapUnitCombatant` and `BattleDamage` for combat simulations and damage calculations
- `MilitaryModule` implements `AIModule` interface for integration with the AI system
- `MilitaryModule` operates on `MapUnit`, `Tile`, `City` and `Civilization` objects for military operations
