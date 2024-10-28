package com.unciv.logic.automation.civilization.purchases.unit

import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.models.unit.Unit

object UnitEnhancements {

    /**
     * Attempts to upgrade a given unit based on the AI's personality.
     */
    fun tryUpgradeUnit(unit: Unit) {
        UnitAutomation.tryUpgradeUnit(unit)
    }
}
