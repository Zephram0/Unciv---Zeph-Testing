
# Current Development Focus: Settler Movement Logic

> This document tracks the active Component being implemented from `road_map`.md#Current-Development-Focus`. Update at the start of work on a new Component and during development sessions.

> **Documentation Requirements**
>
> 1. REQUIRED: **Section Name** [Reason to alter file]
> 2. REQUIRED: **Section Name** [Reason to alter file]
> ...
> 5. REQUIRED: **Section Name** [Reason to alter file]
>
> Note: This placeholder structure will be replaced with specific implementation details. Update this file before beginning new development work.


## 1. Implementation Structure
> For both file trees below:
> 1. Complete class and function signatures including parameters, return types, and visibility modifiers
> 2. Properties with their types and visibility
> 3. Key dependencies and imports
> 4. Interface implementations and inheritance
> 5. Package organization and file grouping


### 1.1 Current File Structure
com.unciv.logic.aautoexpert.modules.military/

```kotlin
└── MilitaryModule.kt
    ├── class MilitaryModule : AIModule {
    │   ├── // Properties
    │   │   ├── private val battleHelper: BattleHelper = BattleHelper()
    │   │   └── private val targetHelper: TargetHelper = TargetHelper()
    │   │
    │   ├── // Extension Properties
    │   │   ├── private fun MapUnit.isRanged(): Boolean = baseUnit.rangedStrength > 0
    │   │   ├── private fun MapUnit.isMelee(): Boolean = baseUnit.strength > 0 && !isRanged()
    │   │   └── private fun MapUnit.canMove(): Boolean = currentMovement > 0
    │   │
    │   ├── // Core Functions
    │   │   ├── override fun processDecisions(civInfo: Civilization): Unit
    │   │   ├── private fun getUnitPriority(unit: MapUnit): Int
    │   │   └── private fun processUnitDecision(unit: MapUnit): Unit
    │   │
    │   ├── // Unit Handlers
    │   │   ├── private fun handleRangedUnit(unit: MapUnit): Unit
    │   │   ├── private fun handleMeleeUnit(unit: MapUnit): Unit
    │   │   └── private fun handleOtherMilitaryUnit(unit: MapUnit): Unit
    │   │
    │   ├── // Combat Logic
    │   │   ├── private fun evaluateRangedTargets(unit: MapUnit, targets: List<MapUnit>): MapUnit?
    │   │   └── private fun findOptimalRangedPosition(unit: MapUnit): Tile?
    │   │
    │   └── // Utility Functions
    │       ├── private fun evaluateDefensivePosition(tile: Tile): Float
    │       ├── private fun evaluateThreatLevel(tile: Tile, unit: MapUnit): Float
    │       ├── private fun tryHealUnit(unit: MapUnit): Boolean
    │       ├── private fun shouldGarrison(unit: MapUnit): Boolean
    │       └── private fun findGarrisonCity(unit: MapUnit): City?
    │
    └── // Dependencies
        ├── import com.unciv.logic.aautoexpert.modules.core.AIModule
        ├── import com.unciv.logic.civilization.Civilization
        ├── import com.unciv.logic.map.mapunit.MapUnit
        ├── import com.unciv.logic.map.tile.Tile
        ├── import com.unciv.logic.battle.*
        └── import com.unciv.logic.city.City
```


### 1.2 Target File Structure
com.unciv.logic.aautoexpert.expertmilitary/
```kotlin
├── ExpertMilitaryModule.kt
│   ├── class ExpertMilitaryModule : AIModule {
│   │   ├── // Properties
│   │   │   ├── private val stateManager: ExpertStateManager
│   │   │   ├── private val unitHandlers: Map<UnitType, UnitHandler>
│   │   │   └── private val battleCalculator: ExpertBattleCalc
│   │   │
│   │   ├── // Core Functions
│   │   │   ├── override fun processDecisions(civInfo: Civilization): Unit
│   │   │   ├── private fun delegateUnitControl(unit: MapUnit): Boolean
│   │   │   └── private fun updateMilitaryState(civInfo: Civilization): Unit
│   │   │
│   │   └── // Event Handlers
│   │       ├── private fun onUnitCreated(unit: MapUnit): Unit
│   │       └── private fun onUnitDestroyed(unit: MapUnit): Unit
│   │
│   └── // Dependencies
│       ├── import com.unciv.logic.aautoexpert.modules.core.AIModule
│       ├── import com.unciv.logic.aautoexpert.expertutils.ExpertStateManager
│       └── import com.unciv.logic.civilization.Civilization
│
├── experthandlers/
│   ├── UnitHandler.kt
│   │   └── interface UnitHandler {
│   │       ├── fun canHandle(unit: MapUnit): Boolean
│   │       ├── fun handleUnit(unit: MapUnit): Unit
│   │       └── fun getPriority(): Int
│   │   }
│   │
│   ├── ExpertSettlerHandler.kt
│   │   └── class ExpertSettlerHandler : UnitHandler {
│   │       ├── override fun canHandle(unit: MapUnit): Boolean
│   │       ├── override fun handleUnit(unit: MapUnit): Unit
│   │       ├── override fun getPriority(): Int
│   │       ├── fun evaluatePosition(tile: Tile): Float
│   │       ├── fun calculateResourceScore(tile: Tile): Float
│   │       └── fun determineOptimalPath(start: Tile, target: Tile): List<Tile>
│   │   }
│   │
│   ├── ExpertRangedHandler.kt
│   │   └── class ExpertRangedHandler : UnitHandler {
│   │       ├── override fun canHandle(unit: MapUnit): Boolean
│   │       ├── override fun handleUnit(unit: MapUnit): Unit
│   │       ├── override fun getPriority(): Int
│   │       ├── fun evaluateTargets(unit: MapUnit): List<MapUnit>
│   │       └── fun findOptimalPosition(unit: MapUnit): Tile
│   │   }
│   │
│   └── ExpertWarriorHandler.kt
│       └── class ExpertWarriorHandler : UnitHandler {
│           ├── override fun canHandle(unit: MapUnit): Boolean
│           ├── override fun handleUnit(unit: MapUnit): Unit
│           ├── override fun getPriority(): Int
│           ├── fun evaluateCombatPosition(tile: Tile): Float
│           └── fun calculateThreatResponse(threats: List<MapUnit>): Action
│       }
│
└── expertutils/
    ├── ExpertBattleCalc.kt
    │   └── object ExpertBattleCalc {
    │       ├── fun calculateDamage(attacker: MapUnit, defender: MapUnit): Int
    │       └── fun predictBattleOutcome(attacker: MapUnit, defender: MapUnit): BattlePrediction
    │   }
    │
    └── ExpertTactics.kt
        └── object ExpertTactics {
            ├── fun evaluatePosition(tile: Tile): Float
            └── fun calculateThreatLevel(tile: Tile): Float
        }
```


## 2. Important Inputs
> Get from `input_docs/valid_inputs.md`
> **CRITICAL**: Check `patterns/` docs second, validate against current codebase third, then search for established patterns.
> Follow valid_inputs.md template.

### [Pattern Name]
```kotlin:path/to/implementation/file
// Function signature and core implementation
fun patternFunction(param1: Type1, param2: Type2): ReturnType {
    return result
}

// Usage example
class Implementation {
    fun usePattern() {
        val result = patternFunction(value1, value2)
        handleResult(result)
    }
}
```
- **Source:** `patterns/category_patterns.md#pattern-name`, `SourceFile.kt:lines`
- **Prevents:** Primary issue, secondary issues, integration problems
- **Solved:** Original bug/issue, solution description, documentation link

## 3. Important Outputs
> Get from `output_docs/valid_outputs.md`
> **CRITICAL**: Check `patterns/` docs second, validate against current codebase third, then search for established patterns.
> Follow valid_outputs.md template.

### [Pattern Name]
```kotlin:path/to/implementation/file
// Function signature and core implementation
fun patternFunction(param1: Type1, param2: Type2): ReturnType {
    return result
}

// Usage example
class Implementation {
    fun usePattern() {
        val result = patternFunction(value1, value2)
        handleResult(result)
    }
}
```
- **Source:** `patterns/category_patterns.md#pattern-name`, `SourceFile.kt:lines`
- **Prevents:** Primary issue, secondary issues, integration problems
- **Solved:** Original bug/issue, solution description, documentation link
