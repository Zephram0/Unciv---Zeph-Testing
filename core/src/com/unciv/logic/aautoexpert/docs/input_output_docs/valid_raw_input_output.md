
## 1. Key Places to Look:

1. **UI Component Entry Points:**
   - `WorldScreen.kt` - Main game interface
   - `UnitTable.kt` - Unit information display
   - `CityScreen.kt` - City management interface
   - `BattleTable.kt` - Combat information

2. **Core Game Logic Classes:**
   - `MapUnit.kt` - Unit state and actions
   - `City.kt` - City state and management
   - `Battle.kt` - Combat mechanics
   - `Civilization.kt` - Civilization state and actions

### 1.1 Verified Raw Inputs:

1. **Unit State Queries:**
`````kotlin:MapUnit.kt
// Movement
unit.hasMovement(): Boolean
unit.canMoveTo(tile: Tile): Boolean
unit.getMaxMovement(): Int

// Combat
unit.canAttack(): Boolean
unit.canAttackTarget(target: MapUnit): Boolean
unit.isFortified(): Boolean
unit.isEmbarked(): Boolean

// Unit State
unit.isCivilian(): Boolean
unit.canGarrison(): Boolean
unit.canPillage(): Boolean
`````

2. **City State Queries:**
`````kotlin:City.kt
// Resources and Production
city.canBuildUnit(unit: String): Boolean
city.canBuild(construction: String): Boolean
city.isConnectedToCapital(): Boolean
city.isBlockaded(): Boolean

// Combat
city.getCombatStrength(): Int
city.getRangedCombatStrength(): Int
city.canBombard(): Boolean
`````

3. **Civilization State Queries:**
`````kotlin:Civilization.kt
// Resources
civInfo.getGold(): Int
civInfo.getHappiness(): Float
civInfo.getScience(): Float

// Diplomacy
civInfo.isAtWarWith(otherCiv: Civilization): Boolean
civInfo.getDiplomacyStatus(otherCiv: Civilization): DiplomaticStatus
`````

### Verified Raw Outputs:

1. **Unit Commands:**
`````kotlin:MapUnit.kt
// Movement
unit.movement.moveToTile(tile: Tile)
unit.fortify()
unit.sleep()
unit.garrison()

// Combat
unit.doAttack(defender: MapUnit)
unit.pillage()
unit.fortifyUntilHealed()
`````

2. **City Commands:**
`````kotlin:City.kt
// Production
city.startBuilding(construction: String)
city.purchaseBuilding(building: String)
city.buyTile(tile: Tile)

// Management
city.assignPopulation(tile: Tile)
city.setFocus(focus: String)
`````

3. **Civilization Commands:**
`````kotlin:Civilization.kt
// Diplomacy
civInfo.declareWar(otherCiv: Civilization)
civInfo.makePeace(otherCiv: Civilization)
civInfo.proposeTrade(trade: Trade)

// Research
civInfo.tech.startResearchOf(tech: String)
`````

### 1.2 Strategy to Find More:

1. **Follow UI Flow:**
   - Trace each button/action in the UI to its underlying function
   - Verify the function includes proper validation
   - Check if it's a direct input/output or a composite action

2. **Check Core Game Classes:**
   - Look for public methods with validation
   - Verify they represent atomic actions
   - Ensure they match human player capabilities

Would you like me to:
1. Continue listing more verified inputs/outputs?
2. Focus on a specific category?
3. Analyze a particular game system in detail?


## 2. Refined Definitions:

**Raw Input:**
> A direct query of game state that:
1. Returns unmodified data directly from the game's data structures
2. Does not perform calculations or transformations beyond mandatory validation
3. Does not trigger state changes
4. Is accessible through public interfaces
5. Only accesses information that would be visible to a human player
6. Includes necessary validation logic to prevent illegal state access

Examples:
````kotlin
// Raw Inputs
unit.hasMovement()                  // Includes necessary movement point validation
unit.canAttack()                    // Includes mandatory combat eligibility checks
tile.isVisible(civInfo)             // Includes fog of war validation
unit.canEnterTile(tile)             // Includes terrain/unit stacking validation
````

**Raw Output:**
> A direct action that:
1. Maps 1:1 with a single player action
2. Has direct, observable effects
3. Cannot be broken down into more fundamental actions
4. Includes mandatory validation checks
5. Only performs actions available to human players
6. Uses established game mechanics for state changes

Examples:
````kotlin
// Raw Outputs
unit.movement.moveToTile(tile)      // Includes movement point validation
unit.fortify()                      // Legal state change with validation
city.purchaseBuilding(building)     // Includes resource/gold validation
unit.doAttack(defender)             // Includes combat eligibility checks
````

### Invalid Examples:

**Invalid Raw Inputs:**
````kotlin
// These bypass game rules or validation
unit.health                         // Direct property access without validation
tile.resource                       // Might bypass fog of war
unit.currentMovement               // Should use hasMovement() instead
````

**Invalid Raw Outputs:**
````kotlin
// These bypass game rules or validation
unit.health = newValue             // Direct property modification not allowed
unit.position = newPosition        // Must use movement system
unit.teleportTo(tile)             // Bypasses movement rules
city.addPopulation(5)             // Must use growth mechanics
````

### Example of Proper Raw Output with Validation:
````kotlin:MapUnit.kt
// Valid raw output with proper validation
fun moveToTile(targetTile: Tile) {
    // Mandatory validation
    if (!hasMovement()) return
    if (!canEnterTile(targetTile)) return
    if (!movement.canMoveTo(targetTile)) return
    
    // Actual movement execution through proper game mechanics
    movement.headTowards(targetTile)
}
````

Valid Raw Input and Valid Raw Output definitions emphasizes that:
1. Raw inputs/outputs must include their mandatory validation logic
2. Direct property access/modification is usually not valid
3. Actions must use established game mechanics
4. Validation is part of the "raw" interface, not an optional addition
