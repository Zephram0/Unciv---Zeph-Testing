# AAutoExpert AI Development Project

> This document provides a high-level overview of the AAutoExpert project as outlined in `project_overview.md`. It provides a roadmap projecting each phase of development until completion. It serves as the primary guide for project scope and direction.

> **Road Map Documentation Requirements**
>
> This is a living document that must be modified and reorganized when project overview changes and must be updated to reflect current focus progress.
>
> 1. REQUIRED: Under "Planned Project Structure" section, update architecture when implementation plans change or current file structure in `architecture.md#current-file-structure` changes.
> 2. REQUIRED: Under "Current Development Focus" section, document all completed features, components, and tasks with file/function references and maintain links to corresponding `project_overview.md` sections
> 3. REQUIRED: Under "Current Development Focus" section goals, update goals to align with `project_overview.md`
> 4. REQUIRED: Under "Current Development Focus" section, keep features, components, and tasks synchronized with `project_overview.md#current-progress-status` and `current_focus.md#immediate-tasks`
> 5. REQUIRED: Task lists and Planned Project Structure folders/files have checkboxes, [ ] unstarted, [-] started, [✓] completed; unfinished tasks have % complete and required subtasks to complete.  
>
> All completed code, tasks and checkboxes, as well as any changed code, MUST include reference to related files and functions.

## 1. Project Structure

### 1.1 Overview
> The 5-layer pipeline architecture ensures clean separation between game state collection, processing, and action execution. This structure enables MVP development while maintaining extensibility for future enhancements.

### 1.2 Current File Structure
> Shows actual implementation in codebase. Use this to track current progress and identify gaps between current and target structure. All changes must be reflected in architecture.md.

---
autoexpert/
├── [✓] AAutoExpert.kt                     # Main entry point
├── [-] modules/                           # Basic module structure (30%)
│   ├── [-] military/                      # Initial military handling
│   │   ├── [-] MilitaryModule.kt         # Basic unit control
│   │   └── [-] handlers/                  # Unit type handlers
│   │       └── [-] SettlerHandler.kt      # Settler movement logic
│   └─ [✓] core/                         # Core interfaces
│       └── [✓] AIModule.kt               # Base module interface
└── [-] utils/                            # Basic utilities (10%)
    └── [-] Logger.kt                     # Simple logging
---

### 1.3 Current Phase Target Structure
> Next incremental goal showing minimum files needed for Phase 1 MVP. This structure represents our immediate development target and will update when entering new phases.

---
autoexpert/
├── [✓] AAutoExpert.kt                     # Main coordinator
├── [-] expertpipeline/                    # Basic pipeline (MVP)
│   ├── [-] expertinput/                   # Simple input processing
│   │   ├── [✓] ExpertRawInput.kt         # Game state collection
│   │   └── [ ] ExpertInputRefiner.kt      # Basic data transformation
│   ├── [ ] expertcore/                    # Core decision making
│   │   ├── [ ] ExpertStrategicCore.kt     # Basic decisions
│   │   └── [ ] ExpertStateManager.kt      # State tracking
│   └── [-] expertoutput/                  # Action execution
│       └── [-] ExpertRawOutput.kt         # Basic actions
├── [-] expertmodules/                     # Phase 1 modules
│   ├── [-] expertmilitary/               # Military focus
│   │   ├── [-] ExpertMilitaryModule.kt    # Unit coordination
│   │   └── [-] experthandlers/            # Unit handlers
│   │       ├── [-] ExpertSettlerHandler.kt # Settler logic
│   │       └── [ ] ExpertWarriorHandler.kt # Warrior logic
│   └── [ ] experteconomy/                 # Basic economy
│       ├── [ ] ExpertEconomyModule.kt     # Resource management
│       └── [ ] experthandlers/            # Economy handlers
│           └── [ ] ExpertCityHandler.kt   # City management
└── [ ] expertutils/                       # Phase 1 utilities
    ├── [ ] ExpertLogger.kt                # Enhanced logging
    └── [ ] ExpertMetrics.kt               # Basic metrics
---

### 1.4 Final Target Structure
> Long-term vision of complete system. While subject to change, this provides architectural guidance and helps maintain consistent naming/organization across development phases.

---
autoexpert/
├── [✓] AAutoExpert.kt                     # Main coordinator
├── [-] expertpipeline/                    # 5-layer AI processing pipeline (15%)
│   ├── [-] expertinput/                   # Input processing layers
│   │   ├── [✓] ExpertRawInput.kt         # Game state collection
│   │   ├── [ ] ExpertInputRefiner.kt      # Data transformation
│   │   └── [ ] expertutils/               # Input utilities
│   │       └── [ ] ExpertInputValidator.kt # Input validation
│   ├── [ ] expertcore/                    # Strategic decision core
│   │   ├── [ ] ExpertStrategicCore.kt     # Decision processing
│   │   ├── [ ] ExpertStateManager.kt      # State management
│   │   ├── [ ] ExpertVictoryManager.kt    # Victory tracking
│   │   └── [ ] ExpertPersonality.kt       # AI personality
│   └── [-] expertoutput/                  # Output processing
│       ├── [ ] ExpertOutputRefiner.kt     # Action refinement
│       ├── [-] ExpertRawOutput.kt         # Action execution
│       └── [ ] expertutils/               # Output utilities
│           └── [ ] ExpertOutputValidator.kt# Action validation
├── [-] expertmodules/                     # Domain modules
│   ├── [-] expertmilitary/               # Military operations
│   │   ├── [-] ExpertMilitaryModule.kt    # Unit coordination
│   │   └── [-] experthandlers/            # Unit handlers
│   ├── [ ] experteconomy/                 # Economic operations
│   ├── [ ] expertculture/                 # Cultural operations
│   ├── [ ] expertscience/                 # Research operations
│   ├── [ ] expertreligion/                # Religious operations
│   └── [ ] expertdiplomacy/               # Diplomatic operations
├── [ ] experttesting/                     # Test implementations
│   ├── [ ] ExpertTestGame.kt              # Test game setup
│   ├── [ ] ExpertUnitTests.kt             # Unit testing
│   └── [ ] expertintegration/             # Integration tests
└── [ ] expertutils/                       # Global utilities
    ├── [ ] ExpertLogger.kt                # Logging system
    ├── [ ] ExpertMetrics.kt               # Performance monitoring
    ├── [ ] ExpertPipeline.kt              # Pipeline tracking
    └── [ ] ExpertDebugger.kt              # Debug utilities
---

### 1.5 Pipeline Architecture
> Visualizes data flow and module interactions. Critical for understanding how components communicate and maintaining clean separation between layers.

```mermaid
graph TD
    subgraph Pipeline Layers
        ExpertRawInput --> ExpertInputRefiner
        ExpertInputRefiner --> ExpertStrategicCore
        ExpertStrategicCore --> ExpertOutputRefiner
        ExpertOutputRefiner --> ExpertRawOutput
    end

    subgraph Core Systems
        ExpertStrategicCore --> ExpertStateManager
        ExpertStrategicCore --> ExpertVictoryManager
        ExpertStrategicCore --> ExpertPersonality
    end

    subgraph Expert Modules
        ExpertMilitaryModule
        ExpertEconomyModule
        ExpertCultureModule
        ExpertScienceModule
        ExpertReligionModule
        ExpertDiplomacyModule
    end

    ExpertStrategicCore --> Expert Modules
    ExpertStateManager --> Expert Modules
    ExpertVictoryManager --> Expert Modules


## 2. Current Development Focus: Milestone 1.2 - Early Game Basics
> This section outlines the Features needed to complete the current Milestone, breaking each into key Components. Components track overall progress but defer implementation details to `current_focus.md`.

## 2. Current Development Focus: Milestone 1.2 - Early Game Basics
> This section outlines the Features needed to complete the current Milestone, breaking each into key Components. Components track overall progress but defer implementation details to `current_focus.md`.

### 2.1 Feature 1: First Turn Decision Making (35%)
*(Ref: `project_overview.md#milestone-1.2`)*

#### 2.1.1 Component 1: Settler Movement Logic (40%)
- [✓] Initial position evaluation
- [-] Resource proximity scoring (50%)
- [ ] Strategic location assessment
- [ ] Movement path optimization

#### 2.1.2 Component 2: City Placement System (30%)
- [✓] Basic resource evaluation
- [-] Growth potential analysis (60%)
- [ ] Strategic value calculation
- [ ] Defensive position assessment

#### 2.1.3 Component 3: Starting Unit Control (20%)
- [✓] Warrior positioning
- [ ] Scout movement patterns
- [ ] Barbarian camp handling
- [ ] Initial exploration routes

### 2.2 Feature 2: Early Production Queue (15%)
*(Ref: `project_overview.md#milestone-1.2`)*

#### 2.2.1 Component 1: Build Order Priority (15%)
- [-] Scout vs Warrior evaluation (40%)
- [ ] Worker timing optimization
- [ ] Monument consideration
- [ ] Resource improvement sequencing

#### 2.2.2 Component 2: Resource Management (10%)
- [-] Initial tile working strategy (30%)
- [ ] Growth vs Production balance
- [ ] Luxury resource prioritization
- [ ] Strategic resource planning

### 2.3 Feature 3: Initial Expansion Planning (8%)
*(Ref: `project_overview.md#milestone-1.2`)*

#### 2.3.1 Component 1: Territory Assessment (5%)
- [-] Second city location scoring (20%)
- [ ] Border expansion prediction
- [ ] Resource securing strategy
- [ ] Chokepoint identification

#### 2.3.2 Component 2: Early Military Security (10%)
- [-] Threat assessment system (25%)
- [ ] Defensive unit positioning
- [ ] Barbarian camp clearing priority
- [ ] Scout safety protocols

### 2.4 Feature 4: Early Diplomacy (9%)
*(Ref: `project_overview.md#milestone-1.2`)*

#### 2.4.1 Component 1: Initial Diplomatic Relations (10%)
- [ ] Alliance initiation protocols
- [-] Threat assessment (30%)
- [ ] Diplomatic messaging system
- [ ] Trade negotiation strategies

#### 2.4.2 Component 2: Diplomatic Strategy Development (8%)
- [ ] Early game treaties
- [ ] Strategic alliance formation
- [-] Diplomacy logging (25%)
- [ ] Reactive adjustments

### 2.5 Feature 5: Economic Foundations (10%)
*(Ref: `project_overview.md#milestone-1.2`)*

#### 2.5.1 Component 1: Early Gold Management (8%)
- [-] Gold income optimization (20%)
- [ ] Early investment strategies
- [ ] Economic event responses
- [ ] Budget allocation

#### 2.5.2 Component 2: Resource Allocation Strategies (12%)
- [-] Resource type balancing (30%)
- [ ] Luxury vs strategic prioritization
- [ ] Dynamic prioritization
- [ ] City resource distribution


## 3. Current Feature Implementation: Feature 1 - Settler Movement Logic
> This section details the specific code changes needed to evolve the current Feature toward our current Phase Target Structure. It shows both current implementation and planned changes to help developers understand the transformation path.

### 3.1 Current Implementation Analysis
> Analysis of MilitaryModule.kt showing current functionality organization. The file tree must show:
> 1. All functions grouped by their role (Properties, Core Functions, etc.)
> 2. Complete function signatures including parameters and return types
> 3. Key properties and their types
> 4. Important dependencies and imports
> 5. Any extension functions or special modifiers
> This detailed breakdown helps identify code that needs to be extracted or refactored in the transformation process.

```kotlin
MilitaryModule.kt
├── Properties
│   ├── private fun MapUnit.isRanged(): Boolean
│   ├── private fun MapUnit.isMelee(): Boolean
│   └── private fun MapUnit.canMove(): Boolean
├── Core Functions
│   ├── override fun processDecisions(civInfo: Civilization)
│   ├── private fun getUnitPriority(unit: MapUnit): Int
│   └── private fun processUnitDecision(unit: MapUnit)
├── Unit Handlers
│   ├── private fun handleRangedUnit(unit: MapUnit)
│   ├── private fun handleMeleeUnit(unit: MapUnit)
│   └── private fun handleOtherMilitaryUnit(unit: MapUnit)
├── Combat Logic
│   ├── private fun evaluateRangedTargets(unit: MapUnit, targets: List<MapUnit>): MapUnit?
│   └── private fun findOptimalRangedPosition(unit: MapUnit): Tile?
└── Utility Functions
    ├── private fun evaluateDefensivePosition(tile: Tile): Float
    ├── private fun evaluateThreatLevel(tile: Tile, unit: MapUnit): Float
    ├── private fun tryHealUnit(unit: MapUnit): Boolean
    ├── private fun shouldGarrison(unit: MapUnit): Boolean
    └── private fun findGarrisonCity(unit: MapUnit): City?
```

### 3.2 Target Implementation
> Final structure after implementing all changes. The file tree must show:
> 1. Complete class and function signatures including parameters, return types, and visibility modifiers
> 2. Properties with their types and visibility
> 3. Key dependencies and imports
> 4. Interface implementations and inheritance
> 5. Package organization and file grouping
> This detailed structure serves as the blueprint for the transformation process.

```kotlin
expertmilitary/
├── ExpertMilitaryModule.kt                 # Main military coordination
│   ├── Dependencies
│   │   ├── import com.unciv.logic.aautoexpert.modules.core.AIModule
│   │   ├── import com.unciv.logic.aautoexpert.expertutils.ExpertStateManager
│   │   └── import com.unciv.logic.civilization.Civilization
│   ├── Properties
│   │   ├── private val stateManager: ExpertStateManager
│   │   ├── private val unitHandlers: Map<UnitType, UnitHandler>
│   │   └── private val battleCalculator: ExpertBattleCalc
│   ├── Core Functions
│   │   ├── override fun processDecisions(civInfo: Civilization)
│   │   ├── private fun delegateUnitControl(unit: MapUnit): Boolean
│   │   └── private fun updateMilitaryState(civInfo: Civilization)
│   └── Event Handlers
│       ├── private fun onUnitCreated(unit: MapUnit)
│       └── private fun onUnitDestroyed(unit: MapUnit)
├── experthandlers/                         # Specialized unit handlers
│   ├── UnitHandler.kt                     # Base handler interface
│   │   └── interface UnitHandler
│   │       ├── fun canHandle(unit: MapUnit): Boolean
│   │       ├── fun handleUnit(unit: MapUnit)
│   │       └── fun getPriority(): Int
│   ├── ExpertSettlerHandler.kt            # Settler unit control
│   │   └── class ExpertSettlerHandler : UnitHandler
│   │       ├── override fun canHandle(unit: MapUnit): Boolean
│   │       ├── override fun handleUnit(unit: MapUnit)
│   │       ├── override fun getPriority(): Int
│   │       ├── fun evaluatePosition(tile: Tile): Float
│   │       ├── fun calculateResourceScore(tile: Tile): Float
│   │       └── fun determineOptimalPath(start: Tile, target: Tile): List<Tile>
│   ├── ExpertRangedHandler.kt             # Ranged unit control
│   │   └── class ExpertRangedHandler : UnitHandler
│   │       ├── override fun canHandle(unit: MapUnit): Boolean
│   │       ├── override fun handleUnit(unit: MapUnit)
│   │       ├── override fun getPriority(): Int
│   │       ├── fun evaluateTargets(unit: MapUnit): List<MapUnit>
│   │       └── fun findOptimalPosition(unit: MapUnit): Tile
│   └── ExpertWarriorHandler.kt            # Melee unit control
│       └── class ExpertWarriorHandler : UnitHandler
│           ├── override fun canHandle(unit: MapUnit): Boolean
│           ├── override fun handleUnit(unit: MapUnit)
│           ├── override fun getPriority(): Int
│           ├── fun evaluateCombatPosition(tile: Tile): Float
│           └── fun calculateThreatResponse(threats: List<MapUnit>): Action
└── expertutils/                           # Utility modules
    ├── ExpertBattleCalc.kt               # Combat calculations
    │   └── object ExpertBattleCalc
    │       ├── fun calculateEngagementOutcome(attacker: MapUnit, defender: MapUnit): CombatPrediction
    │       ├── fun evaluatePositionalAdvantage(unit: MapUnit, tile: Tile): Float
    │       └── fun predictBattleDamage(attacker: MapUnit, defender: MapUnit): Float
    └── ExpertTactics.kt                  # Strategic utilities
        └── object ExpertTactics
            ├── fun evaluateDefensivePosition(tile: Tile): Float
            ├── fun calculateThreatLevel(tile: Tile, unit: MapUnit): Float
            └── fun findOptimalRetreatPath(unit: MapUnit, threats: List<MapUnit>): List<Tile>
```
