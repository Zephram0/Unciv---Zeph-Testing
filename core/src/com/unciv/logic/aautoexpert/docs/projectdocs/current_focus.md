# Current Development Focus: Settler Movement Logic

> This document tracks the active Component being implemented from `road_map.md#Current-Development-Focus`. Update at the start of work on a new Component and during development sessions.

> **Documentation Requirements**
>
> 1. REQUIRED: **Implementation Structure** - Must show both current and target file structures with exact paths
> 2. REQUIRED: **Implementation Priority** - Must list tasks in order with dependencies
> 3. REQUIRED: **Required Changes** - Must detail specific code changes needed
> 4. REQUIRED: **Testing Strategy** - Must outline test cases and validation approach
> 5. REQUIRED: **Success Criteria** - Must define measurable completion criteria
>
> Note: All file paths must be exact and all code changes must reference specific files and functions.

## 1. Implementation Structure
> For both file trees below:
> 1. Complete class and function signatures including parameters, return types, and visibility modifiers
> 2. Properties with their types and visibility
> 3. Key dependencies and imports
> 4. Interface implementations and inheritance
> 5. Package organization and file grouping

### 1.1 Current Implementation
```kotlin
com.unciv.logic.aautoexpert/
├── expertmodules/
│   ├── expertmilitary/
│   │   ├── ExpertMilitaryModule.kt
│   │   │   └── class ExpertMilitaryModule : AIModule {
│   │   │       // Properties
│   │   │       private val militaryHandler: ExpertMilitaryHandler
│   │   │       private val settlerHandler: ExpertSettlerHandler
│   │   │       // Functions
│   │   │       override fun processDecisions(civInfo: Civilization)
│   │   │       private fun processUnit(unit: MapUnit)
│   │   │   }
│   │   └── experthandlers/
│   │       ├── ExpertSettlerHandler.kt
│   │       │   └── class ExpertSettlerHandler(private val ruleset: Ruleset) {
│   │       │       // Properties
│   │       │       private val resourceWeights: Map<ResourceType, Float>
│   │       │       private val terrainWeights: Map<String, Float>
│   │       │       // Core Functions
│   │       │       fun handleSettler(unit: MapUnit)
│   │       │       private fun findSettlementLocation(unit: MapUnit): Tile?
│   │       │       // Evaluation Functions
│   │       │       private fun evaluateTile(tile: Tile): Float
│   │       │       private fun scoreResources(tile: Tile): Float
│   │       │       private fun scoreStrategicValue(tile: Tile): Float
│   │       │       private fun scoreDefensiveValue(tile: Tile): Float
│   │       │       // Path Functions
│   │       │       private fun findSafePath(start: Tile, target: Tile): List<Tile>
│   │       │       private fun validatePath(path: List<Tile>): Boolean
│   │       │   }
│   │       └── ExpertMilitaryHandler.kt
│   │           └── class ExpertMilitaryHandler {
│   │               // Functions
│   │               fun handleMilitary(unit: MapUnit)
│   │               fun protectSettler(unit: MapUnit, settler: MapUnit)
│   │               private fun findNearestSettler(unit: MapUnit): MapUnit?
│   │               private fun calculateDefensivePosition(settler: MapUnit): Tile
│   │           }
├── rulevalidator_patch/
│   ├── input/
│   │   ├── ValidRawInputValidator.kt
│   │   │   └── object ValidRawInputValidator {
│   │   │       fun registerValidator(domain: String, validator: InputValidator)
│   │   │   }
│   │   └── unit/
│   │       ├── UnitMovementValidator.kt
│   │       │   └── object UnitMovementValidator : InputValidator {
│   │       │       fun canMoveToTile(unit: MapUnit, tile: Tile): Boolean
│   │       │       fun hasMovementPoints(unit: MapUnit): Boolean
│   │       │       fun getValidMoves(unit: MapUnit): List<Tile>
│   │       │   }
│   │       └── UnitSettlerValidator.kt
│   │           └── object UnitSettlerValidator : InputValidator {
│   │               fun canFoundCity(unit: MapUnit, tile: Tile): Boolean
│   │               fun isValidSettleLocation(tile: Tile): Boolean
│   │               fun hasSettlerCapability(unit: MapUnit): Boolean
│   │           }
│   └── output/
│       ├── ValidRawOutputValidator.kt
│       │   └── object ValidRawOutputValidator {
│       │       fun registerCommand(domain: String, command: OutputCommand)
│       │   }
│       └── unit/
│           ├── UnitMovementCommands.kt
│           │   └── object UnitMovementCommands : OutputCommand {
│           │       fun moveToTile(unit: MapUnit, tile: Tile): Boolean
│           │       fun followPath(unit: MapUnit, path: List<Tile>): Boolean
│           │   }
│           └── UnitSettlerCommands.kt
│               └── object UnitSettlerCommands : OutputCommand {
│                   fun foundCity(unit: MapUnit): Boolean
│                   fun moveAndSettle(unit: MapUnit, tile: Tile): Boolean
│               }
└── expertutils/
    ├── ExpertMovementHelper.kt
    │   └── object ExpertMovementHelper {
    │       fun moveTowardsSafely(unit: MapUnit, target: Tile)
    │       fun isMovementSafe(unit: MapUnit, tile: Tile): Boolean
    │       fun findSafePath(start: Tile, end: Tile): List<Tile>
    │   }
    └── ExpertTileEvaluator.kt
        └── object ExpertTileEvaluator {
            fun evaluateResourceValue(tile: Tile): Float
            fun evaluateStrategicValue(tile: Tile): Float
            fun evaluateDefensiveValue(tile: Tile): Float
        }
```

### 1.2 Target Implementation
```kotlin
com.unciv.logic.aautoexpert/
├── expertmodules/
│   ├── expertmilitary/
│   │   ├── ExpertMilitaryModule.kt
│   │   │   └── class ExpertMilitaryModule : AIModule {
│   │   │       // Properties
│   │   │       private val militaryHandler: ExpertMilitaryHandler
│   │   │       private val settlerHandler: ExpertSettlerHandler
│   │   │       // Functions
│   │   │       override fun processDecisions(civInfo: Civilization)
│   │   │       private fun processUnit(unit: MapUnit)
│   │   │   }
│   │   └── experthandlers/
│   │       ├── ExpertSettlerHandler.kt
│   │       │   └── class ExpertSettlerHandler(private val ruleset: Ruleset) {
│   │       │       // Properties
│   │       │       private val resourceWeights: Map<ResourceType, Float>
│   │       │       private val terrainWeights: Map<String, Float>
│   │       │       // Core Functions
│   │       │       fun handleSettler(unit: MapUnit)
│   │       │       private fun findSettlementLocation(unit: MapUnit): Tile?
│   │       │       // Evaluation Functions
│   │       │       private fun evaluateTile(tile: Tile): Float
│   │       │       private fun scoreResources(tile: Tile): Float
│   │       │       private fun scoreStrategicValue(tile: Tile): Float
│   │       │       private fun scoreDefensiveValue(tile: Tile): Float
│   │       │       // Path Functions
│   │       │       private fun findSafePath(start: Tile, target: Tile): List<Tile>
│   │       │       private fun validatePath(path: List<Tile>): Boolean
│   │       │   }
│   │       └── ExpertMilitaryHandler.kt
│   │           └── class ExpertMilitaryHandler {
│   │               // Functions
│   │               fun handleMilitary(unit: MapUnit)
│   │               fun protectSettler(unit: MapUnit, settler: MapUnit)
│   │               private fun findNearestSettler(unit: MapUnit): MapUnit?
│   │               private fun calculateDefensivePosition(settler: MapUnit): Tile
│   │           }
├── rulevalidator_patch/
│   ├── input/
│   │   ├── ValidRawInputValidator.kt
│   │   │   └── object ValidRawInputValidator {
│   │   │       fun registerValidator(domain: String, validator: InputValidator)
│   │   │   }
│   │   └── unit/
│   │       ├── UnitMovementValidator.kt
│   │       │   └── object UnitMovementValidator : InputValidator {
│   │       │       fun canMoveToTile(unit: MapUnit, tile: Tile): Boolean
│   │       │       fun hasMovementPoints(unit: MapUnit): Boolean
│   │       │       fun getValidMoves(unit: MapUnit): List<Tile>
│   │       │   }
│   │       └── UnitSettlerValidator.kt
│   │           └── object UnitSettlerValidator : InputValidator {
│   │               fun canFoundCity(unit: MapUnit, tile: Tile): Boolean
│   │               fun isValidSettleLocation(tile: Tile): Boolean
│   │               fun hasSettlerCapability(unit: MapUnit): Boolean
│   │           }
│   └── output/
│       ├── ValidRawOutputValidator.kt
│       │   └── object ValidRawOutputValidator {
│       │       fun registerCommand(domain: String, command: OutputCommand)
│       │   }
│       └── unit/
│           ├── UnitMovementCommands.kt
│           │   └── object UnitMovementCommands : OutputCommand {
│           │       fun moveToTile(unit: MapUnit, tile: Tile): Boolean
│           │       fun followPath(unit: MapUnit, path: List<Tile>): Boolean
│           │   }
│           └── UnitSettlerCommands.kt
│               └── object UnitSettlerCommands : OutputCommand {
│                   fun foundCity(unit: MapUnit): Boolean
│                   fun moveAndSettle(unit: MapUnit, tile: Tile): Boolean
│               }
└── expertutils/
    ├── ExpertMovementHelper.kt
    │   └── object ExpertMovementHelper {
    │       fun moveTowardsSafely(unit: MapUnit, target: Tile)
    │       fun isMovementSafe(unit: MapUnit, tile: Tile): Boolean
    │       fun findSafePath(start: Tile, end: Tile): List<Tile>
    │   }
    └── ExpertTileEvaluator.kt
        └── object ExpertTileEvaluator {
            fun evaluateResourceValue(tile: Tile): Float
            fun evaluateStrategicValue(tile: Tile): Float
            fun evaluateDefensiveValue(tile: Tile): Float
        }
```

## 2. Implementation Priority

1. **Validation Layer Refinement (Current Focus)**
   - [x] Basic validator structure
   - [-] Unit movement validation (50%)
   - [ ] City founding validation
   - [ ] Split validators into domain-specific files

2. **Settler Movement Logic**
   - [-] Resource proximity scoring (50%)
   - [ ] Strategic location assessment
   - [ ] Movement path optimization
   - [ ] Integration with validators

3. **Testing & Stability**
   - [ ] Unit tests for validators
   - [ ] Integration tests for settler movement
   - [ ] Performance monitoring
   - [ ] Error handling improvements

## 3. Required Changes

### 3.1 ValidRawInputValidator.kt Updates
- Add UnitSettlerValidator.canFoundCity validation:
  ```kotlin
  // Check city spacing
  if (tile.getTilesInDistance(3).any { it.isCityCenter() }) return false
  // Check terrain validity
  if (tile.isWater || tile.isImpassible()) return false
  // Check resources
  if (tile.resource != null && tile.resource!!.resourceType == ResourceType.Luxury) {
      // Additional validation for luxury resources
  }
  ```

### 3.2 ExpertSettlerHandler.kt Adjustments
- Update resource scoring to consider:
  ```kotlin
  private fun scoreResources(tile: Tile): Float {
      var score = 0f
      // Prioritize luxury resources
      if (tile.resource?.resourceType == ResourceType.Luxury) score += 10f
      // Consider nearby strategic resources
      tile.getTilesInDistance(2).forEach { nearbyTile ->
          if (nearbyTile.resource?.resourceType == ResourceType.Strategic)
              score += 5f
      }
      return score
  }
  ```

### 3.3 ExpertMovementHelper.kt Updates
- Add settler-specific path safety:
  ```kotlin
  fun findSettlerSafePath(settler: MapUnit, target: Tile): List<Tile> {
      // Avoid tiles adjacent to barbarians
      // Prefer tiles within military unit protection
      // Consider terrain movement costs
  }
  ```

## 4. Testing Strategy

### 4.1 In-Game Testing (Primary Focus)
- **Test Scenarios:**
  1. Start new game with different map types
  2. Place settler in various terrain situations
  3. Test with different resource distributions
  4. Verify behavior with nearby barbarians

- **Validation Points:**
  - Settler moves towards valuable resources
  - City placement avoids poor locations
  - Military units properly protect settler
  - No invalid city placements attempted

- **Debug Process:**
  1. Enable ExpertStateLogger verbose mode
  2. Monitor decision logs during play
  3. Save games at key decision points
  4. Document unexpected behaviors

### 4.2 Unit Testing (Support Only)
- Test critical calculations:
  ```kotlin
  @Test fun testResourceScoring() {
      val tile = createTestTile(luxury = true)
      assertEquals(10f, scoreResources(tile))
  }
  ```

### 4.3 Integration Testing (Support Only)
- Verify validator integration:
  ```kotlin
  @Test fun testSettlerValidation() {
      val settler = createTestSettler()
      val tile = createTestTile()
      assertTrue(ValidRawInputValidator.UnitValidation.canFoundCity(settler, tile))
  }
  ```

## 5. Success Criteria

### 5.1 MVP Requirements
- Settler successfully founds city near luxury/strategic resources
- Military units maintain protective positions
- No crashes or invalid actions
- Reasonable performance (< 1s per decision)

### 5.2 In-Game Validation
- **Must Pass:**
  1. Settler founds city by turn 2 (turn 3 only for exceptional locations)
  2. City location has at least 1 luxury or 2 strategic resources
  3. No cities founded on invalid tiles
  4. Military protection prevents settler loss
  5. Location provides sustainable growth (food) and production

- **Exceptional Location Definition:**
  - Multiple luxury resources within workable range
  - Hill + river + mountain combination
  - Natural wonder within workable range
  - Strategic chokepoint with defensive terrain

### 5.3 Code Quality
- All validator checks implemented
- ExpertStateLogger provides clear decision tracking
- No direct property access (use validators)
