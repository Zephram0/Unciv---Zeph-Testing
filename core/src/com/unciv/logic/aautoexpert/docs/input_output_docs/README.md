# AAuto Expert I/O Documentation

## Overview
This documentation explains how AAuto Expert handles input/output validation and identifies valid game state operations. It builds upon UnCiv's existing validated operations while adding new specialized validators for expert AI decision-making.

> **Development Status**: This documentation is being actively developed alongside the AAuto Expert AI system. Some sections are complete while others will be expanded as we implement more features. The core validation concepts and search guidelines are ready to use.

## Documentation Structure

### Core Documentation (Complete)
1. [Core Input/Output Concepts](validation/core_in_out_concepts.md)
   - Fundamental validation principles
   - 5-layer validation system
   - Component interactions

2. [Finding Valid I/O in Codebase](validation/finding_valid_io_in_codebase.md)
   - How to search for validated I/O in codebase
   - Common locations and patterns
   - Reusing existing validations

3. [Validation Guide](validation/validation_guide.md)
   - How to identify and create validators
   - Using Civilization 5 rules as reference
   - Testing and error handling

### Verified I/O Reference (In Development)
1. [Raw Inputs](verified_io/raw_inputs.md)
   - Validated game state queries
   - Safe state access patterns
   - Input validation examples
   > Note: Will be populated as we identify and validate inputs

2. [Raw Outputs](verified_io/raw_outputs.md)
   - Validated game state modifications
   - Safe action execution patterns
   - Output validation examples
   > Note: Will be populated as we implement and validate outputs

### Validator Implementation (In Development)
1. [Input Validators](validator_reference/input_validators.md)
   - Input validator implementations
   - Validator usage examples
   - Common validation patterns
   > Note: Will be expanded as new validators are created

2. [Output Validators](validator_reference/output_validators.md)
   - Output validator implementations
   - Validator usage examples
   - State modification patterns
   > Note: Will be expanded as new validators are created

## Key Concepts

### Valid Raw Input (VRI)
- Game state queries with built-in validation
- Safe read-only operations
- Follows Civilization 5 rules
- Used by both UI and AI systems

### Valid Raw Output (VRO)
- Game state modifications with pre-validation
- Atomic operations
- Maintains state consistency
- Follows Civilization 5 mechanics

## Development Workflow

1. **Search Existing Code**
   - First check [Finding Valid I/O in Codebase](validation/finding_valid_io_in_codebase.md)
   - Look for reusable validated operations
   - Understand existing validation patterns

2. **Understand Requirements**
   - Review [Core Input/Output Concepts](validation/core_in_out_concepts.md)
   - Study Civilization 5 mechanics
   - Identify validation needs

3. **Implementation**
   - Follow [Validation Guide](validation/validation_guide.md)
   - Use appropriate validator type
   - Add necessary tests

4. **Documentation**
   - Update relevant .md files
   - Add cross-references
   - Include code examples

## See Also
- [Project Overview](../projectdocs/project_overview.md)
- [Architecture](../projectdocs/architecture.md)
- [Current Focus](../projectdocs/current_focus.md)
- [Road Map](../projectdocs/road_map.md) 