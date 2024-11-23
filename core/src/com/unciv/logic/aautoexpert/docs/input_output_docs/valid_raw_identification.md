## Insights from Comparing Human and AI Player Interactions

### 1. Unified Access Points
- **Public Methods:** Both human and AI players interact with the game state through the same set of public methods. These methods include necessary validation logic, ensuring consistency and preventing unauthorized access or actions.
  
- **Validation Logic:** All critical actions and data retrievals involve mandatory validation checks within these public methods. This ensures that both human and AI interactions adhere strictly to game rules and mechanics.

### 2. Mirrored Interaction Flow
- **Information Retrieval:**
  - **Human Players:** Access game stats and information via UI components, which internally call getter methods with built-in validations.
  - **AI Players:** Access the same information through these validated getter methods, ensuring they receive only legitimate and current game state data.
  
- **Action Execution:**
  - **Human Players:** Perform actions through UI controls that trigger public action methods with validation.
  - **AI Players:** Invoke these same action methods directly, benefiting from the same validation mechanisms that prevent cheating or invalid actions.

### 3. Common Patterns and Practices
- **Encapsulation:** Critical game state data is encapsulated within classes, accessed only through public interfaces that enforce game rules.
  
- **Descriptive Naming:** Functions and variables are named descriptively, reflecting their roles within game mechanics, facilitating easier identification of valid inputs and outputs.
  
- **State Management:** Both human and AI interactions rely on robust state management patterns, ensuring that all state changes are atomic and maintain game integrity.

### 4. Error Handling and Logging
- **Consistent Error Handling:** Both interaction types utilize consistent error handling strategies, such as early validation and mandatory returns on invalid states.
  
- **Comprehensive Logging:** Detailed logging is implemented for all significant actions and state changes, aiding in debugging and ensuring transparency in AI decision-making processes.

## Strategy for Rapid Identification of Valid Raw Inputs and Outputs

### 1. **Trace Human Interaction Pathways**
   - **Identify UI Components:** Start by mapping out all UI components involved in displaying information and accepting player actions (e.g., unit panels, city information screens).
   - **Map to Code Functions:** For each UI component, trace the underlying functions and methods it invokes to retrieve data or perform actions.
   
### 2. **Extract and Catalog Public Interfaces**
   - **List Public Methods:** From the traced functions, compile a list of all public methods that include validation logic.
   - **Categorize Inputs and Outputs:** Classify these methods into inputs (data retrieval) and outputs (action execution) based on their functionality.
   
### 3. **Leverage Existing Documentation and Patterns**
   - **Refer to Validation Guides:** Utilize `in_out_validation_guide.md`, `valid_inputs.md`, and `valid_outputs.md` to align newly identified methods with existing documentation standards.
   - **Adhere to Codebase Patterns:** Follow established code structure and naming conventions to maintain consistency and facilitate easier identification.
   
### 4. **Automate Pattern Identification**
   - **Use Code Analysis Tools:** Implement static code analysis or utilize IDE features to search for public methods with validation logic that match the defined criteria for raw inputs and outputs.
   - **Pattern Templates:** Create template patterns based on common input/output structures to streamline the identification process.
   
### 5. **Implement Comprehensive Testing**
   - **Write Unit Tests:** Develop unit tests for each identified input and output to ensure they function correctly and adhere to validation rules.
   - **Integration Tests:** Simulate complete game scenarios to verify that AI actions remain within game rules and do not exploit hidden states.
   
### 6. **Regular Documentation Updates**
   - **Maintain Valid Lists:** Continuously update `valid_inputs.md` and `valid_outputs.md` with newly identified methods, ensuring they are validated against the current codebase.
   - **Version Control Integration:** Use version control practices to track changes in inputs and outputs, facilitating easier updates and reviews.
   
### 7. **Utilize Code Reviews and Continuous Integration (CI)**
   - **Peer Reviews:** Conduct thorough code reviews to validate that newly added inputs and outputs adhere to defined standards and do not introduce vulnerabilities.
   - **Automated CI Checks:** Integrate CI pipelines that automatically verify the validity of inputs and outputs, running tests to catch any discrepancies early.
   
### 8. **Continuous Refactoring and Validation**
   - **Periodic Audits:** Schedule regular audits of the inputs and outputs lists to remove deprecated methods and incorporate new ones.
   - **Ensure Consistency:** Validate that all listed inputs and outputs remain consistent with game mechanics and UI interactions, updating documentation as necessary.

## Example Strategy Implementation

### Step-by-Step Guide

1. **Start with UI Component Mapping:**
   - **Example:** Identify that the `WorldMapHolder.kt` handles unit movement and combat interactions.
   - **Action:** Trace methods like `moveUnitToTargetTile()` and `executeCombat()` to their underlying implementations in classes such as `MapUnit.kt` and `BattleHelper.kt`.

2. **Identify Public Methods with Validation:**
   - **Example:** Methods like `canAttack()` in `MapUnit.kt` include necessary validation checks.
   - **Action:** Add these to `valid_inputs.md` under appropriate categories.

3. **Categorize and Document Inputs:**
   - **Example:** 
     ```kotlin
     // Raw Inputs
     unit.canAttack()                    // Includes mandatory combat eligibility checks
     tile.isVisible(civInfo)             // Includes fog of war validation
     ```
   - **Action:** Ensure each input includes an explanation of its purpose, issues it addresses, and considerations based on past bugs or validations.

4. **Categorize and Document Outputs:**
   - **Example:** 
     ```kotlin
     // Raw Outputs
     unit.movement.moveToTile(tile)      // Includes movement point validation
     unit.doAttack(defender)             // Includes combat eligibility checks
     ```
   - **Action:** Similarly, document each output with details on what it enables, issues it handles, and any relevant considerations.

5. **Automate Future Identifications:**
   - **Tooling:** Set up scripts or use IDE search functionalities to find new public methods that fit input/output criteria based on naming patterns and validation presence.

6. **Integrate Testing Early:**
   - **Example:** For each new input/output, write unit tests that simulate both valid and invalid scenarios, ensuring methods behave as expected.
   - **Action:** Incorporate these tests into the CI pipeline to maintain ongoing validation.

7. **Maintain Documentation Regularly:**
   - **Action:** Assign team members to update `valid_inputs.md` and `valid_outputs.md` as part of their development workflow, ensuring no valid methods are overlooked.

### Sample Entries for Documentation

#### `valid_inputs.md`

```markdown
## 1. Unit System Inputs
> Raw inputs from Unciv's unit management system (com.unciv.logic.map.mapunit)

### 1.1 Unit Combat State Query

```kotlin:MapUnit.kt:29-39
fun canAttack(): Boolean {
    if (!hasMovement()) return false
    if (isCivilian()) return false
    return attacksThisTurn < maxAttacksPerTurn()
}

// Usage example
fun evaluateCombatCapability() {
    if (unit.canAttack() && unit.health > 50) {
        considerAttackingTarget()
    }
}
```
- **Enables:** Query unit's current combat capabilities and attack availability
- **Issues:** Unit existence validation, movement point validation
- **Considerations:** Previous combat validation bugs, solved by comprehensive state checks

### 1.2 Unit Movement Query

```kotlin:MapUnit.kt:50-75
fun hasMovement(): Boolean = currentMovement > 0

fun getMaxMovement(ignoreOtherUnit: Boolean = false): Int {
    var movement =
            if (isEmbarked()) 2
            else baseUnit.movement

    movement += getMatchingUniques(UniqueType.Movement, checkCivInfoUniques = true)
            .sumOf { it.params[0].toInt() }

    if (movement < 1) movement = 1
    return movement
}

// Usage example
fun evaluateMovementCapability() {
    if (unit.hasMovement() && unit.getMaxMovement() > 1) {
        considerStrategicMovement()
    }
}
```
- **Enables:** Query unit's current and maximum movement capabilities
- **Issues:** Movement point validation, embarked state handling
- **Considerations:** Previous movement calculation bugs, solved by proper state validation
```

#### `valid_outputs.md`

```markdown
## 1. Unit Command Outputs
> Raw outputs to Unciv's unit command system (com.unciv.logic.map.mapunit)

### 1.1 Movement Command

```kotlin:MapUnit.kt:100-115
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

```kotlin:MapUnit.kt:120-135
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
```

## Final Strategy for Rapid Identification and Documentation

1. **Start with UI to Code Mapping:**
   - **Objective:** Ensure that all player-accessible data and actions are mirrored within the AI's input and output lists.
   - **Action:** For each UI component, document the corresponding public methods used for data retrieval and action execution.

2. **Leverage Validation Guides:**
   - **Objective:** Align with the `in_out_validation_guide.md` to maintain consistency and adherence to game rules.
   - **Action:** Use the guide's framework to validate each identified input/output before adding it to the documentation.

3. **Automate Searches for Public Methods:**
   - **Objective:** Reduce manual effort by automating the identification of potential inputs and outputs.
   - **Action:** Utilize code analysis tools or IDE search functionalities to find public methods that include validation logic and match the defined criteria.

4. **Categorize and Document Efficiently:**
   - **Objective:** Streamline the documentation process by following a consistent template.
   - **Action:** Use the provided pattern templates in `valid_inputs.md` and `valid_outputs.md` to ensure each entry includes necessary details such as source, enables, issues, and considerations.

5. **Integrate with Development Workflow:**
   - **Objective:** Ensure that new inputs and outputs are documented in real-time as they are developed.
   - **Action:** Incorporate documentation updates into the standard development workflow, making it a required step before merging new code.

6. **Implement Regular Audits and Reviews:**
   - **Objective:** Maintain the accuracy and completeness of the inputs and outputs lists.
   - **Action:** Schedule periodic reviews to cross-reference the documentation with the current codebase, removing outdated entries and adding new ones as necessary.

7. **Enhance Testing Framework:**
   - **Objective:** Validate the correctness and integrity of each identified input and output.
   - **Action:** Develop comprehensive unit and integration tests that simulate both human and AI interactions, ensuring all actions and data accesses are legitimate and properly validated.

8. **Foster Collaborative Documentation:**
   - **Objective:** Ensure that all team members contribute to and maintain the documentation.
   - **Action:** Use collaborative tools and set clear guidelines for documentation updates, encouraging team members to add to `valid_inputs.md` and `valid_outputs.md` as they develop or modify features.

### Summary

By systematically tracing UI interactions, leveraging existing documentation, automating method identification, and integrating documentation into the development workflow, you can rapidly build and maintain a comprehensive list of Valid Raw Inputs and Valid Raw Outputs. This approach ensures that the AAutoExpert AI operates within the defined parameters, adhering to game mechanics, and maintaining game integrity without compromising performance or introducing bugs.

Implementing these strategies will streamline the process of documenting AI interactions, facilitate easier testing and debugging, and ensure that the AI remains a fair and effective participant in the game.