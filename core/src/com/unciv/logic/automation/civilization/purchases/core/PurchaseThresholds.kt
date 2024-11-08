package com.unciv.logic.automation.civilization.purchases.core

import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.Victory
import com.unciv.models.ruleset.nation.Personality

object PurchaseThresholds {

    /**
     * Determines a dynamic threshold for making purchase decisions based on the perceived value.
     */
    fun dynamicThreshold(perceivedValue: Int, maxThreshold: Double = 5.0): Double {
        return maxThreshold - (perceivedValue / 100.0) * (maxThreshold - 1.0)
    }

    /**
     * Determines whether the AI should make a purchase based on perceived value, cost, and available gold.
     */
    fun shouldPurchase(perceivedValue: Int, goldCost: Int, goldAvailable: Int): Boolean {
        val threshold = dynamicThreshold(perceivedValue)
        return goldAvailable >= goldCost * threshold
    }

    /**
     * Adjusts the perceived value based on the civilization's victory focus.
     */
    fun adjustForVictoryFocus(value: Int, focus: Victory.Focus, personality: Personality): Int {
        return when (focus) {
            Victory.Focus.Military -> (value * 1.2).toInt()
            Victory.Focus.Culture -> (value * 1.1).toInt()
            Victory.Focus.Science -> (value * 1.3).toInt()
            Victory.Focus.CityStates -> value
            else -> value
        }
    }

    // Additional threshold-related functions can be added here
}