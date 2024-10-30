package com.unciv.logic.automation.civilization.purchases.unit

import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.map.mapunit.MapUnit

object UnitEnhancements {

    /**
     * Attempts to upgrade a given unit based on the AI's personality.
     */
    fun tryUpgradeUnit(unit: MapUnit) {
        UnitAutomation.tryUpgradeUnit(unit)
    }
}
