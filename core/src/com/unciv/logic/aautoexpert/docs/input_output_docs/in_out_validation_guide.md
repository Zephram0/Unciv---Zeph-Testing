## Input and Output Validation Guide for AAutoExpert AI Project

### Overview

This guide provides a comprehensive framework for validating the inputs and outputs used by the AAutoExpert AI within the UnCiv codebase. It ensures that all AI interactions adhere to Civilization V mechanics, maintain game integrity, and prevent any form of cheating. The focus is on the AI's turn logic, particularly starting from the `automateCivMoves` function in `NextTurnAutomation.kt`.

---

### 1. Key Principles

- **Transparency:** All AI actions must be observable and reversible through standard game mechanics.
- **Compliance:** Strict adherence to Civilization V mechanics as implemented in UnCiv.
- **Non-Cheating:** Ensure that AI does not access hidden or restricted game states, maintaining fair play.

---

### 2. Validation Process

#### 2.1 Source Analysis Workflow

1. **Locate Entry Points:**
   - Identify where the AI interfaces with the UnCiv codebase.
   - Focus on public methods and properties accessible to the AI.
   - Document AI interactions with existing automation components.

2. **Visibility Check:**
   - Ensure all data accessed by the AI is visible to human players.
   - Confirm that no information is accessed behind the fog of war or hidden game states.

3. **Game Rule Compliance:**
   - Verify that all interactions align with Civilization V mechanics.
   - Cross-reference with UnCiv’s implementation to ensure consistency.

---

### 3. Comprehensive List of Inputs and Outputs

#### 3.1 Inputs

**A. Civilization State Queries**

- **Civilization Information (`civInfo`):**
  - General state (e.g., isBarbarian, isSpectator, isMajorCiv, isCityState).
  - Resources (e.g., gold, happiness, science, culture).
  - Policies and technologies researched.

- **Ruleset Options:**
  - Unique types affecting diplomacy (`UniqueType.DiplomaticRelationshipsCannotChange`).
  - Enabled game features (e.g., religion, espionage).

- **Trade Requests:**
  - Incoming trade offers (`civInfo.tradeRequests`).
  - Available trade resources and luxuries.

**B. Unit and Tile States**

- **Units:**
  - Unit types (e.g., Worker, Fighter, Spy).
  - Unit statuses (e.g., health, movement points, unique abilities).
  - Current actions and automation flags.

- **Tiles:**
  - Tile ownership and improvements.
  - Resource types and statuses (e.g., pillaged, under construction).
  - Visibility and exploration status relative to the civilization.

**C. Diplomatic Status**

- **Relationships:**
  - Current diplomatic relationships (`RelationshipLevel`).
  - Flags indicating diplomatic actions (e.g., DeclinedDeclarationOfFriendship).

- **Potential Allies and Enemies:**
  - Nearby civilizations and their statuses.
  - Threat assessments based on neighboring enemy units.

**D. Game State Information**

- **Victory Conditions:**
  - Current progress towards different victory types.
  
- **City Information:**
  - City states, capitals, and city-specific data.
  - Tile management within cities (e.g., worked tiles, improvements).

**E. Espionage and Religion States**

- **Espionage:**
  - Active spies and their assignments.
  - Available technologies to steal.

- **Religion:**
  - Current religion status and enhancements.
  - Religious beliefs and their impacts.

#### 3.2 Outputs

**A. Diplomatic Actions**

- **Declaring War and Peace:**
  - Methods like `DiplomacyAutomation.declareWar(civInfo)` and `DiplomacyAutomation.offerPeaceTreaty(civInfo)`.

- **Offering Diplomatic Agreements:**
  - Friendship declarations, open borders, research agreements, and defensive pacts.

- **Asking for Help:**
  - Requests for military or economic assistance during wars.

**B. Trade Operations**

- **Responding to Trade Requests:**
  - Evaluating and accepting/rejecting incoming trade offers based on current state and strategy.

- **Exchange of Luxuries:**
  - Facilitating the trade of luxury resources to satisfy city demands.

**C. Unit Automation Actions**

- **Automated Unit Movements:**
  - Moving units towards strategic locations (e.g., enemy cities, resource tiles).

- **Unit Commands:**
  - Executing actions like repairing improvements, building roads, or engaging in combat.

- **Specialized Unit Actions:**
  - Specific behaviors for units with unique abilities (e.g., Great Generals, Settlers, Spies).

**D. City Management**

- **Adopting Policies:**
  - Selecting and integrating new policies based on current needs and strategies.

- **Training Settlers and Other Units:**
  - Automating the production of settlers for city expansion.

- **City Development:**
  - Managing city improvements, assignments, and resource allocations.

**E. Espionage Operations**

- **Automating Spy Actions:**
  - Moving spies to target locations, performing coups, and stealing technologies.

**F. Religion and Faith Spending**

- **Founding and Enhancing Religions:**
  - Using faith to establish or enhance religious beliefs across cities.

- **Spending Faith on Religious Units:**
  - Automating the deployment of missionaries and other religious units.

**G. Infrastructure Development**

- **Building Roads and Other Improvements:**
  - Automating the construction and repair of roads between cities for connectivity.

- **Automating Construction Projects:**
  - Managing the progress and completion of infrastructure projects within cities.

---

### 4. Input Validation Checklist

#### 4.1 Data Access

- [ ] Information is publicly visible to the player.
- [ ] Respects the fog of war and visibility rules.
- [ ] Only accesses owned or visible units/cities.
- [ ] Follows established game mechanics.
- [ ] Utilizes documented public interfaces.

#### 4.2 State Validation

- [ ] The game state accessed is public and accurately represented.
- [ ] No hidden or restricted game states are accessed.
- [ ] State queries are performed through secure and validated methods.

---

### 5. Output Validation Checklist

#### 5.1 Action Executability

- [ ] Actions are executable by human players under similar conditions.
- [ ] Respects movement and action point limitations.
- [ ] Maintains game state consistency after actions are performed.
- [ ] Follows turn order and timing rules.
- [ ] Utilizes official game command interfaces without bypassing restrictions.

#### 5.2 Compliance with Game Mechanics

- [ ] All diplomatic, trade, and military actions align with Civilization V rules.
- [ ] Religious and espionage actions adhere to defined game mechanics.
- [ ] Infrastructure and city management actions follow standard procedures.

---

### 6. Specific Validation Patterns

#### 6.1 Civilizations and Diplomacy

- **Validating War Declarations:**
  ```kotlin
  fun declareWar(civInfo: Civilization, target: Civilization, motivation: Float): Boolean {
      if (DeclareWarPlanEvaluator.evaluateDeclareWarPlan(civInfo, target, motivation) > 0) {
          civInfo.getDiplomacyManager(target)!!.declareWar()
          return true
      }
      return false
  }
  ```
  - **Validates:** The motivation and strategic necessity before declaring war.
  - **Prevents:** Arbitrary war declarations without strategic backing.

- **Validating Peace Treaties and Alliances:**
  ```kotlin
  fun offerPeaceTreaty(civInfo: Civilization): Boolean {
      // Implementation ensuring peace is offered based on game state and diplomacy rules
  }
  ```
  - **Validates:** The current war state and potential benefits of peace.
  - **Prevents:** Peace treaties when not strategically advantageous.

#### 6.2 Trade Operations

- **Responding to Trade Requests:**
  ```kotlin
  fun respondToTradeRequests(civInfo: Civilization, tradeAndChangeState: Boolean) {
      for (tradeRequest in civInfo.tradeRequests.toList()) {
          val otherCiv = civInfo.gameInfo.getCivilization(tradeRequest.requestingCiv)
          if (!tradeAndChangeState || !TradeEvaluation().isTradeValid(tradeRequest.trade, civInfo, otherCiv))
              continue
          
          // Process and accept valid trades
      }
  }
  ```
  - **Validates:** Trade offers based on current resources and strategic needs.
  - **Prevents:** Accepting invalid or unfavorable trades.

- **Exchanging Luxury Resources:**
  ```kotlin
  fun exchangeLuxuries(civInfo: Civilization) {
      // Implementation ensuring luxury exchanges meet game rules and resource availability
  }
  ```
  - **Validates:** Availability and necessity of luxury resources.
  - **Prevents:** Over-exchange leading to resource depletion.

#### 6.3 Unit Automation

- **Automated Unit Movements:**
  ```kotlin
  fun automateUnits(civInfo: Civilization) {
      civInfo.units.forEach { unit ->
          UnitAutomation.automateUnitAction(unit)
      }
  }
  ```
  - **Validates:** Units move within their movement range and towards strategic targets.
  - **Prevents:** Units moving into restricted or harmful areas without cause.

- **Repairing and Building Improvements:**
  ```kotlin
  fun tryRepairOrBuild(unit: MapUnit, tile: Tile) {
      if (tile.isPillaged()) {
          unit.repair(tile)
      } else {
          unit.buildImprovement(tile)
      }
  }
  ```
  - **Validates:** Only owned and visible tiles are targeted for repairs and improvements.
  - **Prevents:** Building on unauthorized or non-constructible tiles.

#### 6.4 City Management

- **Adopting Policies:**
  ```kotlin
  fun adoptPolicy(civInfo: Civilization) {
      // Implementation ensuring policies align with current game strategy and resources
  }
  ```
  - **Validates:** Policies selected are beneficial and within resource constraints.
  - **Prevents:** Adopting policies that could disrupt game balance or strategy.

- **Training Settlers:**
  ```kotlin
  fun trainSettler(civInfo: Civilization) {
      if (civInfo.canTrainSettler()) {
          civInfo.trainUnit("Settler")
      }
  }
  ```
  - **Validates:** Availability of resources and strategic necessity before training settlers.
  - **Prevents:** Training settlers when resources are insufficient or strategically unwise.

---

### 7. Testing and Validation

#### 7.1 Unit Tests

- **Example Test for Worker Automation:**
  ```kotlin
  @Test
  fun `should repair pillaged tile`() {
      // Setup: Create a pillaged tile and assign a worker
      val currentTile = testGame.tileMap[1,1]
      currentTile.resource = "Iron"
      currentTile.setImprovement("Mine")
      currentTile.setPillaged()

      val mapUnit = testGame.addUnit("Worker", civInfo, currentTile)

      // Act: Automate worker action
      workerAutomation.automateWorkerAction(mapUnit, hashSetOf())

      // Assert: Worker should attempt to repair the mine
      assertEquals("Repair", currentTile.improvementInProgress)
      assertTrue(currentTile.turnsToImprovement > 0)
  }
  ```
  - **Ensures:** Workers correctly identify and repair pillaged improvements.

#### 7.2 Integration Tests

- **Comprehensive Turn Simulation:**
  - Simulate complete turns to validate AI decisions across diplomacy, trade, unit actions, and city management.
  - Verify that all AI actions are within the game rules and do not exploit hidden information.

---

### 8. Error Handling and State Management

- **Early Validation:**
  - Handle edge cases at the beginning of functions to prevent invalid states.
  - Use early returns for scenarios where actions should not proceed.

- **Error Logging:**
  - Implement proper logging for any discrepancies or invalid state accesses.
  - Ensure logs are detailed for debugging and validation purposes.

- **State Consistency:**
  - Maintain game state integrity after each AI action.
  - Ensure that state updates are atomic and reversible if necessary.

---

### 9. Documentation and References

- **Civilization V Mechanics Documentation:** Reference for ensuring AI actions align with game mechanics.
- **UnCiv Codebase Patterns:** Adherence to existing code structures and design patterns.
- **Kotlin Best Practices:** Maintain code quality and efficiency.
- **libGDX Documentation:** Understanding of the game framework used by UnCiv.

---

### 10. Implementation Steps for Features

1. **Understand Corresponding Game Mechanic:**
   - Thoroughly research the Civilization V mechanic that the AI feature is intended to implement.

2. **Study Existing Implementations:**
   - Examine how similar features are currently implemented within the UnCiv codebase.

3. **Plan Implementation:**
   - Consider state management, multiplayer synchronization, and how the feature integrates with existing systems.

4. **Write Code Following Patterns:**
   - Ensure new code adheres to UnCiv’s established code structures and naming conventions.

5. **Add Appropriate Tests:**
   - Develop unit and integration tests to validate both technical correctness and game rule compliance.

6. **Document Complex Logic:**
   - Provide clear documentation for any intricate game mechanics or unique implementations.

---

### Conclusion

This guide serves as a foundational document to ensure that the AAutoExpert AI operates within the defined parameters of the UnCiv project. By meticulously validating inputs and outputs, adhering to game mechanics, and maintaining transparency, the AI can function effectively without compromising the integrity of the game.

Should further specificity be required, such as detailed validation patterns or focused sections on particular mechanics, feel free to indicate the area of interest.

**Documentation To Be Completed:**
   - Note: `valid_inputs.md` needs to be populated with input validation patterns and examples
   - Note: `valid_outputs.md` needs to be populated with output validation patterns and examples
   - These files MUST be updated regularly. Update with concrete input and output patterns as they are established and validated.