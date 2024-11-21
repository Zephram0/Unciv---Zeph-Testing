# Current Development Focus: Military Module Base Implementation

> This document tracks the current development priorities and immediate tasks for the AAutoExpert project. It serves as the primary reference for ongoing work. Sections rewritten for each focus when the Active Task changes.

> **Current Focus Documentation Requirements**
>
> This is a living document that must be updated and organized at the start and end of each development session:
>
> 1. REQUIRED: Under "Active Task" - Document/Update task name, priority, status, and implementation goals with links to external codebase files and functions.
> 2. REQUIRED: Under "Current Implementation Goals" - List/Update and optimal coding order primary objectives, with each goal's completion criteria and dependencies.
> 3. REQUIRED: Under "Immediate Tasks" - Track all tasks with checkboxes [ ] unstarted, [-] started, [✓] completed; unfinished tasks have % complete and required subtasks to complete.
> 4. REQUIRED: Under "Integration Points" - Update connections to external codebase modules, including hooks, triggers, and scope.
> 5. REQUIRED: Under "Important Patterns" - List important coding patterns for 'Integration Points' documented in `patterns` to avoid errors and lengthy debugging.
> 6. REQUIRED: Under "Next Steps" - List upcoming tasks and subtasks in priority order, aligned with phases from `road_map.md`.
>
> All updates MUST cross-reference `road_map.md` for task alignment and `project_overview.md` for goal and phase alignment.

## 1. Active Task: Separating Military Control from Base Automation
> Represents the current primary development focus, detailing the specific component or functionality being implemented or modified, along with its priority level and current status.
**Priority: High**
**Status: In Progress**

### 1.1 Current Implementation Goals
> Current Implementation Goals outline the specific objectives and deliverables for the Active Task, organized in priority order with clear completion criteria and dependencies.
1. **Military Module Independence**
   - Remove dependency on `NextTurnAutomation` for military units
   - Implement custom unit handling logic
   - Ensure no duplicate processing of units

2. **Unit Handler Implementation**
   - Complete basic handlers for:
     - Ranged units
     - Melee units
     - Land units
     - Siege units
     - Naval units
     - Air units
   - Focus on movement and positioning logic first
   - Defer complex combat decisions to the next phase

### 1.2 Immediate Tasks
> Immediate Tasks track granular development activities with checkboxes and progress indicators, referencing specific files and functions that require modification or implementation.
1. **Modify `executeTurn` in AAutoExpert.kt**
   - [x] Initial implementation of `executeTurn` in `AAutoExpert.kt`
   - [ ] Remove `NextTurnAutomation.automateCivMoves` call (Long-term goal)
   - [ ] Handle military units exclusively through `MilitaryModule`

2. **Enhance `MilitaryModule.kt`**
   - [x] Initial `MilitaryModule.kt` structure
     - [x] Created unit type handlers framework
     - [x] Added priority system
     - [x] Added air/water/land unit type separation
   - [ ] Implement unit handlers
     - [x] Basic handler structure
     - [ ] Complete `handleRangedUnit()`
     - [ ] Complete `handleMeleeUnit()`
   - [ ] Add evaluation systems
     - [ ] Add basic position evaluation
     - [ ] Include threat assessment
   - [ ] Deferred complex combat decisions to the next phase.

3. **Battle Helper Integration**
   - [x] Implemented `BattleHelper.canAttack` method.
   - [x] Corrected method signatures and parameter usage.
   - [x] Updated `MilitaryModule.kt` to use `BattleHelper` correctly.
   - [x] Documented common coding issues and patterns in `patterns/military_patterns.md`.

4. **Debug Logging**
   - [ ] Track unit decisions.
   - [ ] Monitor performance.
   - [ ] Validate no duplicate processing.

## 2. Integration Points
> Integration Points document the connections between the current focus and external codebase modules, specifying hooks, triggers, and scope of integration to ensure proper system interaction.
1. **Turn Processing**
   - **Hook:** `AAutoExpert.executeTurn()`
   - **Trigger:** "Auto Expert Turn" button
   - **Scope:** Military units only

2. **State Management**
   - Access unit positions
   - Track movement points
   - Monitor combat status


## 3.Important Patterns (from `patterns/`, List important coding patterns for 'Integration Points' documented in `patterns` to avoid errors and lengthy debugging)
>Essential coding patterns documented in patterns/ that provide battle-tested, debugged solutions for the current Immediate Tasks. Each pattern has been validated and eliminates common integration issues, debugging, and runtime errors. These patterns maintain strict consistency with the existing codebase. Replace when no longer applicable.

1. **Military Unit Processing Pattern**
   - ❌ WRONG: Process all units at once
   ```kotlin
   // In MilitaryModule.kt
   fun handleMilitaryUnits() {
       // Tries to handle everything, gets messy
       units.forEach { unit ->
           when(unit.type) {
               UnitType.Ranged -> handleRangedUnit(unit)
               UnitType.Melee -> handleMeleeUnit(unit)
               // Gets unwieldy with more types
           }
       }
   }
   ```
   - ✅ RIGHT: Modular handler system
   ```kotlin
   // In MilitaryModule.kt
   private val unitHandlers = mapOf(
       UnitType.Ranged to ::handleRangedUnit,
       UnitType.Melee to ::handleMeleeUnit
   )

   fun handleMilitaryUnits() {
       val processed = mutableSetOf<Unit>()
       units.sortedByDescending { it.getPriority() }
           .forEach { unit ->
               if (unit !in processed) {
                   unitHandlers[unit.type]?.invoke(unit)
                   processed.add(unit)
               }
           }
   }
   ```

2. **Battle Helper Integration Pattern**
   - ❌ WRONG: Direct combat checks
   ```kotlin
   // In MilitaryModule.kt
   fun handleRangedUnit(unit: Unit) {
       // Brittle direct checks
       if (unit.canAttack() && target.isVisible()) {
           unit.attack(target)
       }
   }
   ```
   - ✅ RIGHT: Use BattleHelper
   ```kotlin
   // In MilitaryModule.kt
   fun handleRangedUnit(unit: Unit) {
       val target = findBestTarget(unit)
       if (BattleHelper.canAttack(unit, target)) {
           executeAttack(unit, target)
       }
   }
   ```

3. **Debug Logging Pattern**
   - ❌ WRONG: Scattered logging
   ```kotlin
   // In MilitaryModule.kt
   fun handleMeleeUnit(unit: Unit) {
       println("Handling unit ${unit.name}")  // Hard to track
       if (someCondition) println("Moving to $destination")
   }
   ```
   - ✅ RIGHT: Structured logging
   ```kotlin
   // In MilitaryModule.kt
   private val logger = LoggerFactory.getLogger(MilitaryModule::class.java)
   
   fun handleMeleeUnit(unit: Unit) {
       logger.debug("Starting melee unit processing: ${unit.name}")
       logger.info("Unit ${unit.name} action: ${unit.currentAction}")
       if (unit.needsHealing()) {
           logger.warn("Unit ${unit.name} requires healing")
       }
   }
   ```

4. **Turn Processing Integration Pattern**
   - ❌ WRONG: Monolithic turn processing
   ```kotlin
   // In AAutoExpert.kt
   fun executeTurn() {
       // Everything mixed together
       automateCivMoves()
       handleAllUnits()
       // Hard to modify individual parts
   }
   ```
   - ✅ RIGHT: Modular turn processing
   ```kotlin
   // In AAutoExpert.kt
   fun executeTurn() {
       logger.debug("Starting military phase")
       militaryModule.handleMilitaryUnits()
       logger.debug("Military phase complete")
       
       // Future phases can be added cleanly
       // economyModule.handleEconomyActions()
       // diplomaticModule.handleDiplomacy()
   }
   ```

5. **State Management Pattern**
   - ❌ WRONG: Direct state modifications
   ```kotlin
   // In MilitaryModule.kt
   fun moveUnit(unit: Unit, tile: Tile) {
       unit.position = tile  // Dangerous direct modification
       unit.movementPoints = 0
   }
   ```
   - ✅ RIGHT: Controlled state updates
   ```kotlin
   // In MilitaryModule.kt
   fun moveUnit(unit: Unit, tile: Tile): Boolean {
       if (!validateMove(unit, tile)) return false
       
       try {
           unit.moveTo(tile)
           updateUnitState(unit)
           logger.debug("Unit ${unit.name} moved to $tile")
           return true
       } catch (e: Exception) {
           logger.error("Move failed: ${e.message}")
           return false
       }
   }
   ```


## 4. Next Steps
>The following steps outline the planned development phases and tasks that align with the project `road_map.md`, focusing on implementing core functionality while maintaining consistency with the overall architectural vision and project goals in `project_overview.md`.
1. **Finalize Melee Unit Logic**
   - Implement combat and movement strategies.
   - Test melee unit interactions in various scenarios.

2. **Implement Debug Logging**
   - Add logs to all critical decision points.
   - Ensure logs provide meaningful insights for debugging.

3. **Begin Combat Logic Development**
   - Develop advanced combat decision-making algorithms.
   - Integrate with battle outcomes and unit state updates.