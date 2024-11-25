# Finding Valid I/O in UnCiv Codebase

This guide helps you search for and identify existing Raw Valid Inputs (RVI) and Raw Valid Outputs (RVO) in the UnCiv codebase. Before creating new validators, you should first search these locations for reusable validated operations.

## How to Search for Valid I/O

### Step 1: Identify Operation Type
Determine what kind of operation you need:
1. **Reading Game State** - Look for RVI in UI/World components
2. **Modifying Game State** - Look for RVO in action handlers
3. **Decision Making** - Check current AI automation code

### Step 2: Check Common Locations

#### For Human Player Operations
Search these packages for validated operations:

1. **UI Components** (`com.unciv.ui`)
   ```kotlin
   // Look for validation patterns like:
   if (!unit.canDoX()) return  // Pre-condition check
   if (!city.meetsRequirements()) return  // Requirement validation
   ```

2. **World Logic** (`com.unciv.logic.world`)
   ```kotlin
   // Search for state queries like:
   tile.hasViewableResource(civInfo)  // Vision validation
   city.canBuildUnit(unit)  // Production validation
   ```

3. **Game Logic** (`com.unciv.logic.game`)
   ```kotlin
   // Look for game rule enforcement:
   gameInfo.checkForVictory()  // Victory conditions
   civInfo.getKnownCivs()  // Diplomatic visibility
   ```

#### For AI Operations
Check automation packages:

1. **Automation Logic** (`com.unciv.logic.automation`)
   ```kotlin
   // Search for AI decision validation:
   unit.movement.canReach(targetTile)  // Path validation
   city.cityConstructions.getConstructableBuildings()  // Build options
   ```

2. **Battle Logic** (`com.unciv.logic.battle`)
   ```kotlin
   // Look for combat validation:
   Battle.canAttack(attacker, defender)  // Attack validation
   unit.canAttack(targetTile)  // Range attack checks
   ```

## What to Look For

### Valid Input Patterns
Search for functions that:
1. Return game state information
2. Include validation checks
3. Handle edge cases
4. Follow Civ 5 rules

Example patterns:
```kotlin
// Common RVI patterns:
fun canDoX(): Boolean {
    if (!meetsPrerequisites()) return false
    if (!hasResources()) return false
    return true
}

fun getValidOptions(): Sequence<T> {
    return options.filter { isValid(it) }
}
```

### Valid Output Patterns
Look for actions that:
1. Check preconditions
2. Execute atomically
3. Maintain consistency
4. Follow game rules

Example patterns:
```kotlin
// Common RVO patterns:
fun doAction() {
    if (!canDoAction()) return
    performAtomicChange()
    updateDependentState()
}

fun executeMove(params) {
    validateOrThrow(params)
    applyChanges()
    notifyListeners()
}
```

## Key Files by Category

### Unit Operations
- `UnitMovement.kt`: Movement validation
- `UnitActions.kt`: Action validation
- `Battle.kt`: Combat validation

### City Operations
- `CityConstructions.kt`: Building validation
- `CityExpansion.kt`: Growth validation
- `CityPopulation.kt`: Tile working validation

### Civilization Operations
- `DiplomacyManager.kt`: Diplomatic action validation
- `TechnologyManager.kt`: Research validation
- `PolicyManager.kt`: Policy adoption validation

## Tips for Finding Valid I/O

1. **Follow the UI Flow**
   - Start from UI actions
   - Trace validation chain
   - Find core validation logic

2. **Check AI Decision Points**
   - Look at automation choices
   - Find validation checks
   - Identify reusable logic

3. **Search Common Patterns**
   ```kotlin
   // Search for these patterns:
   fun canX(): Boolean
   fun getValidX(): List<X>
   fun validateX()
   ```

4. **Review Test Files**
   - Test cases often show valid operations
   - Look for validation setup
   - Check edge case handling

## Contributing Found I/O

When you discover a validated operation in the codebase, follow these steps:

### 1. **Find It!**
Look for validated operations in these key places:
- UI components for human player actions
- Current AI automation code
- Unit/City/Civilization management classes
```kotlin
// Example validation pattern to look for:
if (!unit.canDoX()) return false  // Built-in validation
if (!validateY()) return false    // Explicit validation
```

### 2. **Document It!**
Add your finding to BOTH sets of documentation:

#### Raw I/O Files
- `raw_inputs.md` for validated queries
- `raw_outputs.md` for validated actions
```kotlin
// Found in: UnitMovement.kt
// Used by: Both UI and current AI
// Validates: Movement points, terrain, ZoC
unit.movement.canMoveTo(tile)  // Validated movement check
```

#### Validator Files
- `input_validators.md` for validation implementations
- `output_validators.md` for action implementations
```kotlin
// Found in: ValidRawInputValidator
// Used by: ExpertSettlerHandler
// Validates: City spacing, terrain, resources
ValidRawInputValidator.UnitValidation.canFoundCity()  // City founding check
```

### 3. **Test It!**
Before adding to our catalog:
- Verify it works in game
- Check edge cases
- Confirm it follows Civ 5 rules
- Test with different game states

### 4. **Share It!**
Help others use your finding:
- Add clear usage examples
- Note any gotchas or limitations
- Reference related validators
- Link to similar operations

This collaborative effort will build our validated I/O catalog as we develop the AI system. Every validated operation we find is one less validator we need to create!

## See Also
- [Validation Guide](validation_guide.md)
- [Core Concepts](core_concepts.md)
- [Raw Inputs](../verified_io/raw_inputs.md)
- [Raw Outputs](../verified_io/raw_outputs.md) 