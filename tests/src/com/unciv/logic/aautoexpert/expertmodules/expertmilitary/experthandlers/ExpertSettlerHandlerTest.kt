package com.unciv.logic.aautoexpert.expertmodules.expertmilitary.experthandlers

import com.badlogic.gdx.math.Vector2
import com.unciv.Constants
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.aautoexpert.expertmodules.expertcore.ExpertEvaluationModule
import com.unciv.logic.aautoexpert.rulevalidator_patch.ValidRawInputValidator
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.tile.TileResource

@RunWith(GdxTestRunner::class)
class ExpertSettlerHandlerTest {
    private lateinit var testGame: TestGame
    private lateinit var settlerHandler: ExpertSettlerHandler
    private lateinit var settler: MapUnit
    
    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(5) // Create a 5-hex radius map for testing
        val civ = testGame.addCiv()
        val startTile = testGame.getTile(Vector2.Zero)
        settler = testGame.addUnit("Settler", civ, startTile)
        settlerHandler = ExpertSettlerHandler(testGame.gameInfo.ruleset)
    }
    
    @Test
    fun `should found city on exceptional location regardless of turn`() {
        // given
        val exceptionalTile = testGame.getTile(Vector2.Zero)
        testGame.setTileTerrainAndFeatures(Vector2.Zero, "Plains", "Hill") // Good defensive position
        
        // Create and add luxury resource to ruleset
        val goldResource = TileResource().apply {
            name = "Gold"
            resourceType = ResourceType.Luxury
            terrainsCanBeFoundOn = listOf("Plains")
            improvement = "Mine"
            happiness = 4f
        }
        goldResource.setTransients(testGame.gameInfo.ruleset)
        testGame.ruleset.tileResources[goldResource.name] = goldResource
        
        // Add resource to tile
        exceptionalTile.resource = goldResource.name
        exceptionalTile.setTerrainTransients()
        
        // when
        settlerHandler.handleSettler(settler)
        
        // then
        assertTrue("City should be founded on exceptional location", exceptionalTile.isCityCenter())
        assertEquals("Founded city should belong to settler's civ", 
            settler.civ.civName, exceptionalTile.getCity()?.foundingCiv)
        assertFalse("Settler should be removed after founding city", settler.civ.units.getCivUnits().any { it.id == settler.id })
    }
    
    @Test
    fun `should not found city on invalid location`() {
        // given
        val invalidTile = testGame.getTile(Vector2.Zero)
        testGame.setTileTerrainAndFeatures(Vector2.Zero, "Ocean") // Water tile - invalid for city
        
        // when
        settlerHandler.handleSettler(settler)
        
        // then
        assertFalse("City should not be founded on water", invalidTile.isCityCenter())
        assertTrue("Settler should still exist", settler.civ.units.getCivUnits().any { it.id == settler.id })
    }
    
    @Test
    fun `should respect minimum city spacing`() {
        // given
        val existingCityTile = testGame.getTile(Vector2.Zero)
        testGame.addCity(settler.civ, existingCityTile)
        
        val nearbyTile = testGame.getTile(Vector2(2f, 2f))
        settler.movement.moveToTile(nearbyTile)
        
        // when
        settlerHandler.handleSettler(settler)
        
        // then
        assertFalse("City should not be founded too close to existing city", nearbyTile.isCityCenter())
        assertTrue("Settler should still exist", settler.civ.units.getCivUnits().any { it.id == settler.id })
    }
    
    @Test
    fun `should found city after turn 3 even on mediocre location`() {
        // given
        testGame.gameInfo.turns = 3
        val mediocreLocation = testGame.getTile(Vector2.Zero)
        testGame.setTileTerrainAndFeatures(Vector2.Zero, "Plains") // Basic plains tile
        
        // when
        settlerHandler.handleSettler(settler)
        
        // then
        assertTrue("City should be founded after turn 3 even on mediocre location", mediocreLocation.isCityCenter())
        assertEquals("Founded city should belong to settler's civ", 
            settler.civ.civName, mediocreLocation.getCity()?.foundingCiv)
        assertFalse("Settler should be removed after founding city", settler.civ.units.getCivUnits().any { it.id == settler.id })
    }
}
