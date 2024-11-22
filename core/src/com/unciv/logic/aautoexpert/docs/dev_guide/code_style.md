# Code Style and Structure

## Conciseness and Clarity

- **Write concise, technical Kotlin code** with clear and accurate examples.
- **Include relevant comments** to explain complex logic and decisions, using numbered comments for detailed explanations.

  ```kotlin
  /*
   * Evaluates the strategic value of a tile by considering:
   * 1. Proximity to enemy borders for potential threats.
   * 2. Connectivity to our territories for expansion.
   * 3. Control over chokepoints to restrict enemy movements.
   * 4. Resource availability to support growth.
   *
   * @param tile The tile to evaluate.
   * @param civ The civilization evaluating the tile.
   * @param personality The personality traits influencing strategic decisions.
   * @return The strategic score of the tile.
   */
  ```

## Modular Design

- **Utilize Kotlin’s functional and object-oriented paradigms** to create a modular AI system.
- **Implement separate modules** for different AI aspects (e.g., `EconomyManager`, `MilitaryManager`, `DiplomacyManager`, `TacticalAI`).
- **Use interfaces and abstract classes** to define contracts between modules.

  ```kotlin:path/to/code_style_example.kt
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

## DRY Principle

- **Avoid code duplication** by abstracting common functionalities into reusable components.

## Readability and Maintainability

- **Prioritize easy-to-read and maintainable code** over excessive performance optimizations.
- **Use meaningful variable and function names**, and ensure code is well-commented to facilitate understanding.

## Complete Implementations

- **Fully implement all requested functionalities** without leaving TODOs, placeholders, or incomplete sections.

## Error Handling

- **Incorporate robust error handling and validation** to manage edge cases and unexpected states gracefully.