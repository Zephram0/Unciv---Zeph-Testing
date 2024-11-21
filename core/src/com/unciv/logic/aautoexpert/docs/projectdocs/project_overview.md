# AAutoExpert AI Development Project

> This document provides a high-level overview of the AAutoExpert project, its goals, and implementation strategy. It serves as the primary reference for project scope and direction, acting as the dominant summary of the project's goals, status, and strategy. It ensures alignment with the project's unified vision by referencing updates in `road_map.md` and `architecture.md`, providing actionable steps for updating project progress and goals.

> **Project Overview Documentation Requirements**
>
> This is a living document that must be updated and organized whenever project scope, direction, or status changes. Updates should be made in the specific sections outlined below:
>
> 1. REQUIRED: **Project Goals** – Update this section if there are any changes to the project's objectives or scope, ensuring alignment with the overall vision.
> 2. REQUIRED: **Design and Architecture** – Modify this section when the implementation strategy or technical architecture changes, detailing new approaches or technologies.
> 3. REQUIRED: **Current Project Status** – Update with recent progress, achievements, and upcoming objectives.
> 4. REQUIRED: **Projected Phases** – Adjust to reflect changes in project phases, timelines, or milestones.
> 5. REQUIRED: **Future Development Plans** – Add updates to long-term plans as new modules or strategies are identified.
>
> Additional sections may be updated as necessary to reflect changes in project direction or unforeseen developments. All changes MUST be evaluated against the "Project Goals" section to maintain focus and ensure consistency.

---

## Project Overview

The AAutoExpert project aims to create a sophisticated AI system for Unciv (an open-source Civilization V-like game) that operates alongside the existing automation system. The AI focuses on achieving human expert-level gameplay through advanced decision-making and strategic planning, while strictly adhering to standard game rules without relying on artificial advantages or "cheats". This parallel system enhances the game's challenge and competitiveness through pure strategic intelligence rather than unfair advantages.


## Project Goals

- **Develop a Non-Cheating, High-Performance AI:** Ensure the AI makes strategic decisions based on standard game mechanics without exploiting hidden advantages.
- **Modular and Scalable Design:** Create a system that allows for easy expansion and maintenance through clearly defined modules.
- **Alignment with Personality Traits and Victory Objectives:** Tailor AI behavior based on predefined personality profiles and targeted victory conditions.
- **Enhance Gameplay Experience:** Integrate advanced decision-making algorithms and optimize runtime performance to provide overwhelmingly challenging, competitive, fair, and dynamic gameplay.


## Design and Architecture

### Projected Technical Architecture
*(Highlights planned expansions and overarching design principles.)*
  
- **Design Principles:**
  - **Modular, Self-Contained Systems:** Each module operates independently to handle specific aspects of AI behavior.
  - **Clear Separation of Concerns:** Modules focus on distinct responsibilities for maintainability.
  - **Non-Cheating AI Behavior:** Adherence to standard game rules without hidden advantages.
  - **Performance Optimization:** Efficient algorithms and data handling to ensure swift decision-making.
  - **Extensible Architecture:** Ability to add new modules and features with minimal disruption to existing systems.

### Current Technical Architecture Status
*(Reflects the existing structure, including all folders, subfolders, and `.kt` files.)*

- **Core Framework:**
  - Basic AI system operational.
  - Successfully integrated with the game loop.
  - "Auto Expert Turn" button added and functional.
  - Module structure established.

- **Military Module (In Progress):**
  - Basic unit handling structure created.
  - Priority system implemented and tested.
  - Unit type classification system operational.
  - Handler framework established.
  - Initial separation from base automation completed.
  - Working on tactical decision-making implementation.


## Projected Phases (Past, Current, Future – to Completion)

### Phase 1: Military Module Foundation (Completed)
- Set up module structure.
- Implement unit priority system.
- Create unit type classification system.
- Implement unit processing order.
- Add basic unit type handlers framework.
- Initial integration with turn system.

### Phase 2: Military AI Enhancement (Current)
- Implement specific unit type handlers.
- Add strategic position evaluation.
- Develop combat decision making.
- Create unit coordination system.

### Phase 3: Economic Foundation (Future)
- **City Management:**
  - Implement city placement logic.
  - Optimize build order.
  - Enhance worker automation.
  - Develop resource priority system.
- **Resource Management:**
  - Strategic resource evaluation.
  - Trade route optimization.
  - Gold management.
  - Science focus decisions.

### Phase 4: Strategic Layer (Future)
- **Diplomatic Relations:**
  - Threat assessment.
  - Alliance evaluation.
  - War declaration logic.
  - Peace treaty conditions.
- **Victory Strategy:**
  - Victory path evaluation.
  - Long-term planning.
  - Resource allocation.
  - Strategic pivoting.

### Phase 5: AI Enhancement (Future)
- **Performance Optimization:**
  - Implement caching.
  - Optimize decision trees.
  - Enhance state management.
  - Streamline action execution.
- **Advanced Features:**
  - Personality-based decisions.
  - Adaptive strategies.
  - Multi-theater operations.
  - Complex diplomatic chains.


## Current Project Status

### Current Phase Status
*(Summarizes progress within the current phase.)*

- **Military AI Enhancement:**
  - Implementing specific unit type handlers.
  - Adding strategic position evaluation.
  - Developing combat decision making.
  - Creating unit coordination system.

### Immediate Goals
*(Highlights specific objectives for the current phase.)*

- **Technical Infrastructure:**
  - Add logging system for debugging.
  - Implement state tracking.
  - Create performance monitoring.
- **Military AI Enhancement:**
  - Complete `handleRangedUnit()` in `MilitaryModule.kt`.
  - Complete `handleMeleeUnit()` in `MilitaryModule.kt`.
  - Add strategic position evaluation.
  - Include threat assessment.


## Future Development Plans
*(Outlines long-term plans and strategies for project growth.)*

- **Planned Modules:**
  - Economy Management.
  - City Development.
  - Diplomatic Relations.
  - Victory Strategy.

- **Enhancement Areas:**
  - Strategic resource management.
  - Long-term planning.
  - Adaptive behavior based on game state and personality.