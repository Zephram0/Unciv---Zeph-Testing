# AI Architecture

## Modular AI Design

### Separate Concerns

- **Divide AI functionalities into distinct modules** (e.g., `EconomyManager`, `MilitaryManager`, `DiplomacyManager`, `TacticalAI`).

### Interfaces and Abstract Classes

- **Define clear interfaces and abstract classes** to establish contracts between different AI modules.

  ```kotlin:path/to/ai_architecture_example.kt
  /*
   * 1. Interface defining the contract for AI modules.
   */
  interface AIModule {
      /*
       * 2. Makes a decision based on the current game state.
       *
       * @param gameState The current state of the game.
       * @return The decision made by the AI module.
       */
      fun makeDecision(gameState: GameState): Decision
  }

  /*
   * 3. Economy AI module responsible for economic decisions.
   */
  class EconomyManager : AIModule {
      /*
       * 4. Implements economic decision-making logic.
       *
       * @param gameState The current state of the game.
       * @return The economic decision.
       */
      override fun makeDecision(gameState: GameState): Decision {
          // 5. Analyze the current economic state and make a decision:
          //    a. Increase production if resources are sufficient.
          //    b. Focus on trade if neighboring civs are friendly.
          return Decision.IncreaseProduction
      }
  }
  ```

## Decision-Making Algorithms

### Monte Carlo Tree Search (MCTS)

- **Utilize Kotlin coroutines** for efficient parallel simulations in decision-making processes.
- **Incorporate linear influences** from personality traits and situational variables.

  ```kotlin:path/to/mcts_example.kt
  /*
   * 1. Node in the Monte Carlo Tree Search.
   *
   * @param state The current game state.
   * @param action The action leading to this state.
   */
  class MCTSNode(val state: GameState, val action: Action? = null) {
      var visits = 0
      var score = 0.0
      val children = mutableListOf<MCTSNode>()

      /*
       * 2. Simulates the game from the current node, incorporating personality influences.
       *
       * @param personality The personality traits of the civ.
       * @return The result of the simulation.
       */
      suspend fun simulate(personality: Personality): Double = coroutineScope {
          // 3. Perform simulation logic with personality influences:
          //    a. Higher aggression leads to confrontational actions.
          //    b. Higher economic focus leads to development actions.
          // 4. Simulation code goes here.
          return@coroutineScope score
      }
  }
  ```

### Q-learning / Genetic Algorithms

- **Implement adaptive AI behaviors** using reinforcement learning and evolutionary strategies.

  ```kotlin:path/to/q_learning_example.kt
  /*
   * 1. Q-learning implementation for adaptive AI behavior.
   *
   * @param learningRate The learning rate for the algorithm.
   * @param discountFactor The discount factor for future rewards.
   */
  class QLearning(val learningRate: Double, val discountFactor: Double) {
      private val qValues = mutableMapOf<StateAction, Double>()

      /*
       * 2. Updates the Q-values based on the action taken and the reward received, scaled by personality.
       *
       * @param stateAction The state-action pair.
       * @param reward The reward received from taking the action.
       * @param nextState The resulting state after taking the action.
       * @param personality The personality traits of the civ.
       */
      fun update(stateAction: StateAction, reward: Double, nextState: GameState, personality: Personality) {
          // 3. Scale the reward based on personality traits.
          val scaledReward = reward * personality.scaledFocus(PersonalityValue.Learning)
          // 4. Find the maximum future reward.
          val maxFutureReward = qValues.filterKeys { it.state == nextState }.values.maxOrNull() ?: 0.0
          // 5. Update the Q-value for the state-action pair.
          qValues[stateAction] = (1 - learningRate) * (qValues[stateAction] ?: 0.0) +
                                  learningRate * (scaledReward + discountFactor * maxFutureReward)
      }
  }
  ```

## Integration with Personality and Victory Types

### Personality Alignment

- **Ensure AI decisions are influenced by personality traits** defined in `Personality.kt`.

  ```kotlin:path/to/personality_alignment_example.kt
  /*
   * 1. Diplomacy AI module that makes decisions based on personality traits.
   *
   * @param personality The personality traits of the civ.
   */
  class DiplomacyManager(private val personality: Personality) : AIModule {
      /*
       * 2. Makes a diplomatic decision influenced by the civ's diplomacy focus.
       *
       * @param gameState The current state of the game.
       * @return The diplomatic decision.
       */
      override fun makeDecision(gameState: GameState): Decision {
          // 3. Calculate diplomacy score scaled by diplomacy focus.
          val diplomacyScore = gameState.diplomacyStatus * personality.scaledFocus(PersonalityValue.Diplomacy)
          // 4. Decide actions based on diplomacy score.
          return if (diplomacyScore > diplomacyThreshold) {
              Decision.ForgeAlliance(targetCiv)
          } else {
              Decision.DeclareWar(targetCiv)
          }
      }
  }
  ```

### Victory Type Strategies

- **Align AI strategies with preferred victory types**.

  ```kotlin:path/to/victory_strategy_example.kt
  /**
   * 1. AI module that prioritizes actions based on civilization's preferred victory type.
   */
  class VictoryStrategyManager(
      private val civ: Civilization,
      private val personality: Personality
  ) : AIModule {
      /**
       * 2. Makes strategic decisions aligned with preferred victory type.
       *
       * @param gameState The current state of the game.
       * @return The strategic decision.
       */
      override fun makeDecision(gameState: GameState): Decision {
          val victoryFocus = when (civ.nation.preferredVictoryType) {
              "Scientific" -> calculatePriority(gameState.scienceOutput, PersonalityValue.Science)
              "Cultural" -> calculatePriority(gameState.cultureOutput, PersonalityValue.Culture) 
              "Diplomatic" -> calculatePriority(gameState.cityStateInfluence, PersonalityValue.Diplomacy)
              "Domination" -> calculatePriority(gameState.militaryStrength, PersonalityValue.Military)
              else -> 0.5f // Balanced approach
          }

          return if (victoryFocus > FOCUS_THRESHOLD) {
              Decision.PursueVictoryStrategy(civ.nation.preferredVictoryType)
          } else {
              Decision.PursueBalancedGrowth()
          }
      }

      private fun calculatePriority(baseValue: Float, personalityFocus: PersonalityValue) =
          baseValue * personality.scaledFocus(personalityFocus)
  }
  ```