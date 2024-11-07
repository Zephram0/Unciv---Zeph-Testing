package com.unciv.logic.automation.civilization.purchases.evaluators

import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.nation.PersonalityValue
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.unique.UniqueType

object UnitEvaluator {
    fun determineUnitToPurchase(city: City, personality: Personality): BaseUnit? {
        val availableUnits = city.cityConstructions.getConstructableUnits()
        return availableUnits.maxByOrNull { calculateUnitValue(it, city, personality) }
    }

    fun calculateUnitValue(unit: BaseUnit, city: City, personality: Personality): Int {
        var value = unit.cost // Base value from production cost

        // Strategic value based on unit type and capabilities
        value = calculateStrategicValue(unit, city, value)
        
        // Victory condition focus
        value = applyVictoryFocus(unit, city.civ, value)
        
        // Personality modifiers
        value = applyPersonalityModifiers(unit, personality, value)
        
        // Situational modifiers
        value = applySituationalModifiers(unit, city, value)

        return value
    }

    private fun calculateStrategicValue(unit: BaseUnit, city: City, baseValue: Int): Int {
        var value = baseValue

        when {
            unit.isMilitary -> {
                // Value ranged units more highly
                if (unit.rangedStrength > 0) value = (value * 1.2f).toInt()
                
                // Value siege units when at war
                if (unit.strength > 0 && unit.rangedStrength > 0 && city.civ.isAtWar()) 
                    value = (value * 1.3f).toInt()
                
                // Value naval units less on inland cities
                if (unit.isWaterUnit && !city.isCoastal)  // Changed from type.isWater()
                    value = (value * 0.7f).toInt()
            }
            // Value workers more early game
            unit.hasUnique(UniqueType.BuildImprovements) -> {
                value = (value * (1.5f - city.civ.cities.size * 0.1f)).toInt()
            }
            // Value settlers based on expansion needs
            unit.hasUnique(UniqueType.FoundCity) -> {
                value = calculateSettlerValue(city, value)
            }
        }

        return value
    }

    private fun applyVictoryFocus(unit: BaseUnit, civ: Civilization, baseValue: Int): Int {
        var value = baseValue
        
        when {
            civ.wantsToFocusOn(Victory.Focus.Military) -> {
                if (unit.isMilitary) value = (value * 1.5f).toInt()
            }
            civ.wantsToFocusOn(Victory.Focus.Science) -> {
                if (unit.hasUnique(UniqueType.CanSpreadReligion)) 
                    value = (value * 1.3f).toInt()
            }
            civ.wantsToFocusOn(Victory.Focus.Culture) -> {
                if (unit.hasUnique(UniqueType.GreatPerson)) 
                    value = (value * 1.4f).toInt()
            }
        }
        
        return value
    }

    private fun applyPersonalityModifiers(unit: BaseUnit, personality: Personality, baseValue: Int): Int {
        var value = baseValue

        if (unit.isMilitary) {
            // Military personality affects unit value
            value = (value * personality.modifierFocus(PersonalityValue.Military, 0.3f)).toInt()
            
            // Aggressive personality affects combat units
            if (unit.strength > 0 || unit.rangedStrength > 0) {
                value = (value * personality.modifierFocus(PersonalityValue.Aggressive, 0.2f)).toInt()
            }
        }

        // Expansion personality affects settlers
        if (unit.hasUnique(UniqueType.FoundCity)) {
            value = (value * personality.modifierFocus(PersonalityValue.Expansion, 0.4f)).toInt()
        }

        return value
    }

    private fun applySituationalModifiers(unit: BaseUnit, city: City, baseValue: Int): Int {
        var value = baseValue
        
        // Increase value of military units when threatened
        if (unit.isMilitary && isCityThreatened(city)) {
            value = (value * 1.5f).toInt()
        }

        // Increase value of naval units for coastal cities under threat
        if (unit.isWaterUnit && city.isCoastal() &&  // Changed from type.isWater()
            city.getTiles().any { it.militaryUnit?.civ?.isAtWarWith(city.civ) == true }) {
            value = (value * 1.3f).toInt()
        }

        return value
    }

    private fun calculateSettlerValue(city: City, baseValue: Int): Int {
        var value = baseValue
        val civ = city.civ
        
        // Reduce settler value if happiness is low
        if (civ.getHappiness() < 0) value = (value * 0.5f).toInt()
        
        // Reduce value if we already have many cities
        value = (value * (1.3f - civ.cities.size * 0.1f)).toInt()
        
        // Increase value if we have few cities and good happiness
        if (civ.cities.size < 4 && civ.getHappiness() > 5) 
            value = (value * 1.5f).toInt()
            
        return value
    }

    private fun isCityThreatened(city: City): Boolean {
        return city.getTiles().any { tile ->
            tile.militaryUnit?.civ?.isAtWarWith(city.civ) == true
        }
    }
}