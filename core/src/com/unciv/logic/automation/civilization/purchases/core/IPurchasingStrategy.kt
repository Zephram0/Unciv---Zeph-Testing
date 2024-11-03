package com.unciv.logic.automation.civilization.purchases.core

import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.nation.Personality

interface IPurchasingStrategy {
    fun evaluatePurchases(
        civ: Civilization, 
        personality: Personality
    ): List<PurchaseOption>
}
