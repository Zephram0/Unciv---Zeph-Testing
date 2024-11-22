# AAutoExpert Technical Architecture

> This document details the technical implementation of the AAutoExpert AI system. It serves as the authoritative source for architectural decisions and system design.

> **Architecture Documentation Requirements**
>
> This is a living document that must be updated and reorganized whenever architectural changes are made. For all implementations:
>
> 1. REQUIRED: **System Architecture** - Update to accurately reflect current `aautoexpert/` directory structure.
> 2. REQUIRED: **Project Architecture** - Document design decisions and technical specifications aligned with `project_overview.md#Current-Technical-Architecture-Status` and current module implementations.
> 3. REQUIRED: **Technical Implementation** - Detail implementation strategies that reflect current patterns in the codebase and `aautoexpert/modules/`.
> 4. REQUIRED: **Development Patterns** - Maintain coding standards and critical patterns found in the external codebase and `aautoexpert/patterns/` and support the module structure in `aautoexpert/`.
> 5. REQUIRED: **Integration Reference** - Document integration points and module interactions based on current implementations in `aautoexpert/modules/` and planned integrations from `road_map.md`.
>
> All architectural changes MUST reflect actual structure of `aautoexpert/` and maintain consistency with project goals in `project_overview.md` and development plans in `road_map.md`.


## 1. System Architecture

### 1.1 Current Implementation

- **Active Modules and Responsibilities**
  - **Core Module**: Provides shared utilities and base interfaces.
  - **Military Module**: Handles military decision-making and unit control.
  - **Battle Helper**: Assists with battle-related operations and combat calculations.

- **Current Data Flow**
  1. **Game State Input**
     - The AI receives civilization state through `AAutoExpert.executeTurn()`.
  2. **Module Processing**
     - Military module processes units by type (air, water, land).
     - Units are prioritized based on type and situation.
  3. **Decision Making**
     - Currently implementing unit-specific decision making.
     - Separate handlers for different unit types.
  4. **Action Execution**
     - Direct unit control through MilitaryModule.
     - Separate from base game automation.
  5. **State Updates**
     - Executed actions result in updates to the game state.

- **Current File Structure with Paths from aautoexpert/**
    aautoexpert/
    ├── AAutoExpert.kt                # Main entry point
    ├── modules/
    │   ├── military/                 # Military decision making
    │   │   └── MilitaryModule.kt
    │   └── core/                     # Shared utilities
    │       └── AIModule.kt           # Base interface


- **Integration Points with Base Game**
  - Integrates with Unciv's game state and unit management systems.
  - Utilizes observer patterns to listen for game state changes.

### 1.2 Module Interactions

- **Integration Roadmap**
  - Phase 1: Implement Economy Module.
  - Phase 2: Develop Diplomacy Module.
  - Phase 3: Integrate new modules with existing system.
  - Phase 4: Optimize data flows and performance across all modules.

  #### 1.2.1 Military Module 

- **Military Module ↔ Battle Helper:**
  - The Military Module relies on the Battle Helper for executing attacks and evaluating combat scenarios.
  - Example: Calling `BattleHelper.tryAttackNearbyEnemy` within `handleRangedUnit`.


## 2. Project Architecture
  
### 2.1 Overview

The Unciv AI Expert project follows a modular architecture to ensure scalability, maintainability, and ease of development. Each module is responsible for specific aspects of the AI's functionality.

### 2.2 AAutoExpert Project Modules

#### 2.2.1 AAutoExpert Current Modules

##### 2.2.1.1 Military Module

**Path:** `core/src/com/unciv/logic/aautoexpert/modules/military/MilitaryModule.kt`

**Responsibilities:**
- Handles decision-making for military units, including ranged, melee, naval, and air units.
- Manages unit classification and priority.
- Integrates with battle utilities to execute attacks and evaluate positions.

**Key Components:**
- **Unit Handlers:** Functions like `handleRangedUnit`, `handleMeleeUnit`, etc.
- **Battle Integration:** Utilizes `BattleHelper` for attack operations.

**Data Flow**
1. **Unit Decision:** The Military Module processes each unit's actions based on priority and type.
2. **Battle Execution:** When an attack is needed, the Military Module delegates to the Battle Helper.
3. **Outcome Handling:** Post-battle outcomes are managed and updated within the Military Module.

#### 2.2.2 AAutoExpert Planned Project Modules

##### 2.2.2.1 Economy Module

- **Path:** `core/src/com/unciv/logic/aautoexpert/modules/economy/ExpertEconomyModule.kt`

- **Responsibilities**
  - Manages city and resource-related decisions.
  - Handles economic strategies and resource allocation.

- **Key Components**
  - **City Handlers:** `ExpertCityHandler.kt`
  - **Trade Handlers:** `ExpertTradeHandler.kt`
  - **Worker Handlers:** `ExpertWorkerHandler.kt`
  - **Economic Calculations:** `ExpertEconCalc.kt`

##### 2.2.2.2 Diplomacy Module

- **Path:** `core/src/com/unciv/logic/aautoexpert/modules/diplomacy/ExpertDiplomacyModule.kt`

- **Responsibilities**
  - Manages inter-civilization relations and diplomatic actions.
  - Handles treaties and relationship dynamics.

- **Key Components**
  - **Treaty Handlers:** `ExpertTreatyHandler.kt`
  - **Relations Handlers:** `ExpertRelationsHandler.kt`
  - **Diplomatic Calculations:** `ExpertThreatCalc.kt`

### 2.3 External Modules

#### 2.3.1 Battle Helper

**Path:** `core/src/com/unciv/logic/automation/unit/BattleHelper.kt`

**Responsibilities:**
- Provides utility methods for battle-related operations.
- Determines attack feasibility and executes combat actions.

**Key Methods:**
- `tryAttackNearbyEnemy(unit: MapUnit, stayOnTile: Boolean = false): Boolean`
- `canAttack(attacker: MapUnit, targetTile: Tile): Boolean`
- `chooseAttackTarget(unit: MapUnit, attackableEnemies: List<AttackableTile>): AttackableTile?`


## 3. Technical Implementation

### 3.1 State Management

- **Accessing Game State**
  - Utilize observer patterns to listen for game state changes.
  ---
  class GameStateObserver : Observer<GameState> {
      override fun onChanged(newState: GameState) {
          // Handle state changes
      }
  }
  ---
  
- **Caching Strategies**
  - Implement memoization for expensive calculations.
  - Use in-memory caches with eviction policies.

- **Update Mechanisms**
  - Trigger state updates based on AI decisions.
  - Ensure thread-safety with synchronized access.

### 3.2 Performance

- **Critical Paths**
  - Optimize decision-making algorithms to reduce latency.
  - Example: Replace recursive algorithms with iterative counterparts where feasible.

- **Optimization Points**
  - Profile AI modules using [Your Profiling Tool].
  - Focus on optimizing `MilitaryModule.kt` during combat evaluations.

- **Resource Usage**
  - Monitor memory consumption using Kotlinx Memory Analyzer.
  - Ensure coroutine usage does not lead to excessive thread creation.

### 3.3 Integration Patterns

- **Module Communication**
  - Use event-driven architecture to facilitate communication between modules.
  - [See Integration Points](current_focus.md#integration-points)
  - Core integration points include:
    - Automation system interfaces
    - Game state management
    - Decision-making pipeline
    - Module communication channels

- **Priority Systems**
  - Assign priorities to modules to manage decision-making order.
  - Example: Military decisions take precedence over economic decisions during wartime.

- **Resource Sharing**
  - Share common resources like game state data and utility functions via the `core` module.

- **Handler Implementations**
  - Implement abstract handler patterns for different unit and economic types to ensure modularity.

## 4. Development Patterns

### 4.1 Design Patterns

- **Handler Implementation Patterns**
  - Established an abstract handler pattern for different unit types (Air, Water, Land) to ensure modular and scalable unit processing.
  
- **Processing Order Patterns**
  - Introduced a processing order pattern to handle units in a priority-sorted manner, enhancing tactical decision-making efficiency.
  
- **Priority Systems**
  - Implemented a priority system within the Military Module to determine the order in which units are processed based on their type and calculated priority.
  
- **Module Interaction Patterns**
  - Ensured clear separation of concerns by abstracting battle-related logic into the Battle Helper.

### 4.2 Implementation Standards

- **Error Handling**
  - Implement guard clauses to handle preconditions and invalid states early in functions.
  ---
  fun performAction(action: Action, personality: Personality, civStats: Stats): Result {
      // 1. Check if the action is valid.
      if (!action.isValid()) return Result.Error("Invalid action")
  
      // 2. Check if the action is feasible.
      if (civStats.isUnderThreat() * personality.scaledFocus(PersonalityValue.Aggressive) < action.threshold) {
          return Result.Error("Action not feasible due to threat level and aggression focus")
      }
  
      // 3. Proceed with performing the action.
      // Action execution logic goes here.
      return Result.Success("Action performed successfully")
  }
  ---

- **Logging**
  - Utilize `Logback` for structured logging across all modules.
  ---
  private val log = LoggerFactory.getLogger(DiplomacyManager::class.java)
  
  fun executeDiplomaticAction(action: DiplomaticAction, personality: Personality) {
      try {
          // 1. Execute action
          // Action logic goes here.
  
          // 2. Log successful execution.
          log.info("Diplomatic action ${action.type} executed successfully.")
      } catch (e: Exception) {
          // 3. Log the error with a meaningful message.
          log.error("Failed to execute diplomatic action: ${action.type}", e)
          throw e
      }
  }
  ---
  
- **Testing Requirements**
  - Develop unit tests for AI modules using **JUnit** and **Mockito**.
  - Implement integration tests to validate interactions between AI components.
  
- **Performance Requirements**
  - Optimize algorithms and data processing to maximize runtime speed.
  - Utilize caching techniques and efficient data structures to minimize redundant calculations.

## 5. Integration Reference

### 5.1 Base Game Integration

- **Integration Points with Unciv**
  - Integrates with Unciv's game state and unit management systems.
  - Utilizes observer patterns to listen for game state changes.
  
- **Required Interfaces**
  - Implements interfaces defined in `AIModule.kt` for seamless integration.
  
- **State Management**
  - Accesses and updates the game state through centralized state managers.
  
- **Performance Considerations**
  - Ensures that AI integrations do not introduce significant latency to the game's core mechanics.

### 5.2 Module Integration

- **Inter-module Communication**
  - Facilitates communication between modules using event-driven patterns.
  - Example: Military Module communicates with Battle Helper to execute attacks.
  
- **Shared Resources**
  - Utilizes shared utilities and state managers from the `core` module.
  
- **Event Handling**
  - Implements observer patterns to handle events such as state changes and action triggers.
  
- **State Synchronization**
  - Ensures consistent state across all modules by centralizing state updates and access.

