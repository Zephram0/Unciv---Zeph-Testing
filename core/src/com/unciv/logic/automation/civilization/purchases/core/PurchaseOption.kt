package com.unciv.logic.automation.civilization.purchases.core

//TODO: Add variable for perceived value
class PurchaseOption(
    val type: PurchaseType,
    val cost: Int,
    val baseValue: Float,
    val description: String,
    val action: () -> Unit
) {
    enum class PurchaseType {
        Construction,
        UnitUpgrade,
        CityState,
        Tile
    }
}
