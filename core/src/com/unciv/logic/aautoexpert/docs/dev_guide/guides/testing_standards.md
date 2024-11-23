# Testing Standards

## Testing Philosophy
AAuto Expert follows a two-phase testing approach to ensure both practical functionality and code reliability:
1. **Game-Based Testing**: Rapid validation through actual gameplay using predefined test saves.
2. **Code Testing**: Formal unit and integration tests to verify code correctness and module interactions.

## Phase 1: Game-Based Testing

### Test Save Requirements
- **Location**: `src/test/resources/testSaves/`
- **Naming Convention**: Use descriptive filenames (e.g., `early_game_settler_luxuries.save`) to indicate the scenario.
- **Diversity**: Include various starting conditions and game states to cover different aspects of AI behavior.

### Running Game Tests
1. **Load Test Save**
   - Use `GameTestSetup.loadTestSave(saveName: String)` to load the desired test scenario.
2. **Enable AAuto Expert**
   - Ensure AAuto Expert is active to process AI turns.
3. **Execute Turns**
   - Run the AI for a specified number of turns using `GameTestSetup.runAutoExpertTurns(gameInfo: GameInfo, turns: Int)`.
4. **Observe and Document**
   - Monitor AI behavior and document observations in `progress_log.md`.
5. **Issue Resolution**
   - Address critical issues identified during testing before proceeding to code testing.

### Test Save Categories
- **Early Game (Turn 1-50)**
  - Scenarios: Starting settler movements, initial city placements.
- **Mid Game (Turn 51-150)**
  - Scenarios: Multiple cities, war engagements, trade route setups.
- **Late Game (Turn 151+)**
  - Scenarios: Victory path progressions, complex diplomatic relations.
- **Special Scenarios**
  - War, diplomacy crises, unique resource distributions.

### Documentation of Game Tests
- **Test Scenario Documentation**
  - **Path**: `src/test/resources/testScenarios.md`
  - **Content Example**:
    ```markdown:src/test/resources/testScenarios.md
    # Test Scenario: Early Game Settler Movement

    ## Setup
    - **Save**: early_game_settler.save
    - **Civilization**: Egypt
    - **Turn**: 1
    - **Key Features**: Starting settler, warrior, visible luxuries

    ## Test Steps
    1. Enable AAuto Expert
    2. Run for 5 turns
    3. Observe:
       - Settler movement patterns
       - City placement decision
       - Resource prioritization

    ## Expected Behavior
    - Settler evaluates nearby tiles effectively
    - Prioritizes luxury resources and strategic positions
    - Considers defensive placements for new cities
    ```

## Phase 2: Code Testing

### Unit Testing

- **Develop Unit Tests**: Use **JUnit** and **Mockito** to create tests for individual AI modules.
  
  ```kotlin:path/to/unit_testing_example.kt
  @RunWith(MockitoJUnitRunner::class)
  class EconomyManagerTest {
      @Mock
      lateinit var gameState: GameState

      @Test
      fun `makeDecision should increase production when resources are sufficient`() {
          // 1. Arrange
          whenever(gameState.resources).thenReturn(100)
          val economyManager = EconomyManager()

          // 2. Act
          val decision = economyManager.makeDecision(gameState)

          // 3. Assert
          assertEquals(Decision.IncreaseProduction, decision)
      }
  }
  ```

### Integration Testing

- **Validate Interactions**: Ensure seamless cooperation between AI components through integration tests.

  ```kotlin:path/to/integration_testing_example.kt
  @RunWith(MockitoJUnitRunner::class)
  class MilitaryAndBattleHelperIntegrationTest {
      @Mock
      lateinit var gameState: GameState

      @Mock
      lateinit var battleHelper: BattleHelper

      @Test
      fun `MilitaryModule should use BattleHelper to attack enemies`() {
          // 1. Arrange
          whenever(gameState.getEnemyUnits()).thenReturn(listOf(mock(Unit::class.java)))
          val militaryModule = MilitaryModule(battleHelper)

          // 2. Act
          militaryModule.execute(gameState)

          // 3. Assert
          verify(battleHelper).tryAttackNearbyEnemy(any(Unit::class.java))
      }
  }
  ```

### Continuous Verification

- **Regular Checks**: Continuously verify that AI decisions align with defined personality traits and victory objectives.

### Static Code Analysis

- **Integrate Detekt**: Use Detekt to enforce Kotlin best practices and maintain high code quality standards.
  
  ```yaml:path/to/detekt_config.yml
  detekt:
    config: resources/detekt/detekt.yml
    parallel: true
    failFast: false
  ```
