package com.unciv.logic.automation.civilization.purchases.debug

import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption

object PurchaseDebugger {
    private var isEnabled = true
    private val stringBuilder = StringBuilder()
    
    fun startNewSession(civName: String, gold: Int, goldPerTurn: Float) {
        stringBuilder.clear()
        appendLine("=".repeat(50))
        appendLine("Purchase Analysis for $civName")
        appendLine("Available Gold: $gold")
        appendLine("Gold per Turn: $goldPerTurn")
        appendLine("=".repeat(50))
        appendLine()
    }
    
    fun addStrategyEvaluation(strategyName: String, options: List<PurchaseOption>) {
        if (!isEnabled) return
        
        appendLine("$strategyName Options:")
        if (options.isEmpty()) {
            appendLine("  No purchase options available")
            return
        }

        options.groupBy { it.type }.forEach { (type, typeOptions) ->
            val best = typeOptions.maxByOrNull { it.baseValue / it.cost }
            val worst = typeOptions.minByOrNull { it.baseValue / it.cost }
            
            appendLine("$type Options (${typeOptions.size} total)")
            best?.let { appendPurchaseOption("Best", it) }
            worst?.let { appendPurchaseOption("Worst", it) }
            appendLine()
        }
    }
    
    fun addFinalDecision(selectedOption: PurchaseOption?, finalScore: Float?, reason: String) {
        if (!isEnabled) return
        
        appendLine("\nFinal Decision:")
        if (selectedOption == null) {
            appendLine("No purchase selected: $reason")
            return
        }
        
        appendPurchaseOption("Selected Purchase", selectedOption)
        appendLine("Final Score: $finalScore")
        appendLine("Reason: $reason")
    }

    private fun appendPurchaseOption(label: String, option: PurchaseOption) {
        appendLine("  $label: ${option.description}")
        appendLine("    Cost: ${option.cost}")
        appendLine("    Base Value: ${option.baseValue}")
        appendLine("    Value/Cost Ratio: ${String.format("%.2f", option.baseValue / option.cost)}")
    }
    
    internal fun appendLine(text: String = "") {
        stringBuilder.append(text).append("\n")
    }
    
    fun getDebugLog(): String = stringBuilder.toString()
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
}