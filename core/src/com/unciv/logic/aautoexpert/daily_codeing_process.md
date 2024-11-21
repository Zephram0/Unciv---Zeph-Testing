# Daily Coding Workflow

> This document outlines the standard procedure for a single day in the AAutoExpert AI development process. It ensures consistency, effective planning, and thorough documentation. It will not be used for processes that do not modify AAuto Expert files.

> **Daily Workflow Documentation Requirements**
>
> This is a living document that must be updated daily to reflect current work and ensure alignment with project goals:
>
> 1. REQUIRED: Begin with a review of all relevant documentation files.
> 2. REQUIRED: Create a plan based on the current project focus and priorities.
> 3. REQUIRED: Adjust relevant next tasks in the project roadmap or current_focus.md as needed.
> 4. REQUIRED: Implement code changes following established patterns and guidelines.
> 5. REQUIRED: Debug and reference existing code or patterns as necessary.
> 6. REQUIRED: Update documentation files appropriately (excluding progress_log.md).
>
> All changes MUST be consistent with overall project goals and reference related files and functions.

## Daily Workflow Steps

1. **Review Documentation**
   - Read through all relevant `.md` files (e.g., `road_map.md`, `project_overview.md`, `architecture.md`, `current_focus.md`) to understand the current project status and priorities.

2. **Plan the Day**
   - Based on the current focus, outline tasks to accomplish today.
   - Update active tasks and prioritize based on the project roadmap.
   - Adjust next tasks in relevant `.md` files if necessary.

3. **Implement Code Changes**
   - Write or modify code according to the daily plan.
   - Follow coding patterns and guidelines documented in `patterns/military_patterns.md` and other relevant pattern files to ensure correctness and minimize debugging needs.

4. **Debugging and Testing**
   - Test implemented features to ensure they work as intended.
   - If issues arise, debug using references from the codebase and successful implementations.
   - Document any new patterns or fixes in the appropriate pattern documentation for future reference.

5. **Update Documentation**
   - Update all relevant `.md` files (e.g., `project_overview.md`, `architecture.md`) to reflect changes made during the day.
   - Ensure that any new tasks or adjustments are consistent with project goals.

6. **Update Progress Log**
   - At the end of the day, record the day's accomplishments, challenges, solutions, and updates in `progress_log.md`.

## Best Practices

- **Cross-Reference Documents**
  - Ensure that updates to one document are reflected in related documents to maintain coherence across the documentation.

- **Follow DRY Principle**
  - Avoid duplicating information across documents; instead, reference existing sections where applicable.

- **Consistent Formatting**
  - Maintain consistent formatting and structure across all documentation files.

- **Use Clear and Descriptive Comments**
  - When writing code, include comments that explain complex logic and reference relevant documentation.

## Example Final Daily Evaluation

### Day 3: Military Module Enhancement

#### Review
- Reviewed `road_map.md`, `current_focus.md`, and `patterns/military_patterns.md` to understand current priorities.

#### Plan
- Implement `handleMeleeUnit` in `MilitaryModule.kt`.
- Refactor existing unit handlers to align with the updated priority system.
- Update `patterns/military_patterns.md` with new debugging patterns discovered today.

#### Implementation
- Added `handleMeleeUnit` function following the patterns outlined in `patterns/military_patterns.md`.
- Refactored unit handlers to include more explicit logging.

#### Debugging
- Encountered a type mismatch in `BattleHelper.canAttack` method calls.
- Resolved by correcting parameter types as per `BattleHelper.kt`.

#### Documentation Updates
- Updated `patterns/military_patterns.md` with new debugging practices.
- Modified `architecture.md` to include the new `handleMeleeUnit` function.

#### Progress Log Update
- Will update `progress_log.md` at the end of the day.
