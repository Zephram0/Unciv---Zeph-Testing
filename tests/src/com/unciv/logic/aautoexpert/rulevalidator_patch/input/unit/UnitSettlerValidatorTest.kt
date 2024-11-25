package com.unciv.logic.aautoexpert.rulevalidator_patch.input.unit

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.testing.TestGame
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.unciv.models.ruleset.unique.UniqueType

class UnitSettlerValidatorTest {
    private lateinit var testGame: TestGame
    
    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(5)  // 5-tile radius should be enough for our tests
    }

    @Test
    fun `test basic settler validation`() {
        val civ = testGame.addCiv()
        val tile = testGame.getTile(Vector2.Zero)
        val settler = testGame.addUnit("Settler", civ, tile)

        assertTrue("Valid settler should pass basic requirements", 
            UnitSettlerValidator.canFoundCity(settler, tile))
    }

    @Test
    fun `test non-settler unit fails validation`() {
        val civ = testGame.addCiv()
        val tile = testGame.getTile(Vector2.Zero)
        val warrior = testGame.addUnit("Warrior", civ, tile)

        assertFalse("Non-settler unit should fail validation", 
            UnitSettlerValidator.canFoundCity(warrior, tile))
    }

    @Test
    fun `test water tile fails validation`() {
        val civ = testGame.addCiv()
        val tile = testGame.getTile(Vector2.Zero)
        testGame.setTileTerrainAndFeatures(Vector2.Zero, "Ocean")
        val settler = testGame.addUnit("Settler", civ, tile)

        assertFalse("Cannot found city on water", 
            UnitSettlerValidator.canFoundCity(settler, tile))
    }

    @Test
    fun `test city too close fails validation`() {
        val civ = testGame.addCiv()
        val cityTile = testGame.getTile(Vector2.Zero)
        testGame.addCity(civ, cityTile)
        
        val nearbyTile = testGame.getTile(Vector2(2f, 2f))
        val settler = testGame.addUnit("Settler", civ, nearbyTile)

        assertFalse("Cannot found city too close to another city", 
            UnitSettlerValidator.canFoundCity(settler, nearbyTile))
    }

    @Test
    fun `test city on resource passes validation`() {
        val civ = testGame.addCiv()
        val tile = testGame.getTile(Vector2.Zero)
        
        // Create and add strategic resource
        val ironResource = testGame.createResource("Strategic Resource")
        tile.resource = ironResource.name
        
        val settler = testGame.addUnit("Settler", civ, tile)

        assertTrue("Should be able to found city on resource tile", 
            UnitSettlerValidator.canFoundCity(settler, tile))
    }
}