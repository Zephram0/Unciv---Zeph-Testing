package com.unciv.logic.automation.civilization.purchases.decision

import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.Personality

interface IPurchasingStrategy {
    fun execute(civ: Civilization, personality: Personality)
}
