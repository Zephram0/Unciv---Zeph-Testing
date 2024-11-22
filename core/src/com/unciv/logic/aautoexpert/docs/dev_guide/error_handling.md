# Error Handling

## Guard Clauses

- **Implement guard clauses** to handle preconditions and invalid states early in functions.

  ```kotlin:path/to/guard_clauses_example.kt
  /*
   * Performs an action if it is valid and feasible based on current civ stats and personality.
   *
   * @param action The action to be performed.
   * @param personality The personality traits of the civ.
   * @param civStats The current statistics of the civ.
   * @return The result of the action execution.
   */
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
  ```

## Meaningful Messages

- **Provide clear and meaningful error messages and logging** to facilitate debugging.

  ```kotlin:path/to/meaningful_messages_example.kt
  /*
   * Executes a diplomatic action and logs the outcome.
   *
   * @param action The diplomatic action to execute.
   * @param personality The personality traits of the civ.
   */
  fun executeDiplomaticAction(action: DiplomaticAction, personality: Personality) {
      try {
          // 1. Execute the diplomatic action.
          // Action logic goes here.

          // 2. Log successful execution.
          log.info("Diplomatic action ${action.type} executed successfully.")
      } catch (e: Exception) {
          // 3. Log the error with a meaningful message.
          log.error("Error executing diplomatic action ${action.type}: ${e.message}", e)
          throw e
      }
  }
  ```